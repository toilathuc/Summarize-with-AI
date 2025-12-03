package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryRequest;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.utils.CacheUtils;
import com.example.summarizer.utils.ChunkUtils;
import com.example.summarizer.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SummarizationOrchestrator implements SummarizeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SummarizationOrchestrator.class);

    // ==========================
    // CONFIG
    // ==========================
    private static final int MAX_PARALLEL_BATCHES = 3;
    private static final Duration BATCH_TIMEOUT = Duration.ofSeconds(45);
    private static final int BATCH_RETRY = 1;
    private static final int CIRCUIT_THRESHOLD = 3;
    private static final int MAX_CONTENT_CHARS = 4000;

    private final SummarizerPort client;
    private final String promptTemplate;
    private final int batchSize;
    private final Semaphore semaphore = new Semaphore(MAX_PARALLEL_BATCHES);
    private final ExecutorService pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private final ObjectMapper mapper = new ObjectMapper();

    private final AtomicInteger failures = new AtomicInteger(0);
    private volatile boolean circuitOpen = false;

    public SummarizationOrchestrator(SummarizerPort client, String promptTemplate, int batchSize) {
        this.client = client;
        this.promptTemplate = promptTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    // ===============================================================
    // PUBLIC API
    // ===============================================================

    @Override
    public List<SummaryResult> summarize(List<FeedArticle> articles) throws Exception {
        return summarize(articles, null);
    }

    @Override
    public List<SummaryResult> summarize(List<FeedArticle> articles, Map<String, SummaryResult> cache) throws Exception {

        if (articles == null || articles.isEmpty()) return List.of();

        Map<String, SummaryResult> safeCache = cache == null ? Map.of() : cache;

        List<SummaryResult> ordered = new ArrayList<>(Collections.nCopies(articles.size(), null));
        List<PendingRequest> pending = new ArrayList<>();
        int reused = 0;

        // 1) REUSE CACHE
        for (int i = 0; i < articles.size(); i++) {
            FeedArticle art = articles.get(i);
            SummaryResult c = findCachedResult(art, safeCache);

            if (c != null && Boolean.TRUE.equals(art.getIsSummarized())) {
                ordered.set(i, c);
                reused++;
            } else {
                pending.add(new PendingRequest(art.toSummaryRequest(), i));
            }
        }

        logger.info("Summaries → {} reused, {} pending", reused, pending.size());
        if (pending.isEmpty()) return ordered;

        // 2) SPLIT BATCH
        List<List<PendingRequest>> batches = ChunkUtils.chunked(pending, batchSize);

        List<Future<List<BatchResult>>> fs = new ArrayList<>();
        int batchId = 0;

        for (List<PendingRequest> batch : batches) {
            int myId = batchId++;

            semaphore.acquire(); // Limit parallel
            fs.add(pool.submit(() -> {
                try {
                    return processBatchWithRetry(batch, myId);
                } finally {
                    semaphore.release();
                }
            }));
        }

        // 3) COLLECT RESULTS
        for (Future<List<BatchResult>> f : fs) {
            for (BatchResult br : f.get()) {
                ordered.set(br.position, br.summary);
            }
        }

        return ordered;
    }

    // ===============================================================
    // CORE PROCESSING
    // ===============================================================

    private List<BatchResult> processBatchWithRetry(List<PendingRequest> batch, int batchId) {

        if (circuitOpen) {
            logger.warn("⚡ Circuit OPEN → fallback batch {}", batchId);
            return fallbackBatch(batch);
        }

        for (int attempt = 0; attempt <= BATCH_RETRY; attempt++) {

            try {
                List<BatchResult> rs = processBatch(batch, batchId);

                failures.set(0);
                circuitOpen = false;
                return rs;

            } catch (Exception ex) {

                logger.warn("Batch {} attempt {} failed → {}", batchId, attempt + 1, ex.getMessage());

                int failCount = failures.incrementAndGet();
                if (failCount >= CIRCUIT_THRESHOLD) {
                    circuitOpen = true;
                    logger.error("🚨 CIRCUIT OPEN after {} failures. All next batches fallback.", failCount);
                }

                if (attempt == BATCH_RETRY) break;
            }
        }

        return fallbackBatch(batch);
    }

    private List<BatchResult> processBatch(List<PendingRequest> batch, int batchId) throws Exception {

        List<SummaryRequest> reqs = batch.stream().map(p -> p.request).toList();
        String prompt = buildPrompt(reqs);

        logger.debug("[Batch {}] running {} items", batchId, batch.size());

        CompletableFuture<String> cf = CompletableFuture.supplyAsync(() -> {
            try {
                return client.generate(prompt);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, pool);

        String raw = cf.orTimeout(BATCH_TIMEOUT.getSeconds(), TimeUnit.SECONDS).join();

        String jsonStr = JsonUtils.extractJsonBlock(raw);
        JsonNode payload = mapper.readTree(jsonStr);

        List<SummaryResult> parsed = parseSummaries(payload);

        if (parsed.size() != batch.size()) {
            logger.warn("[Batch {}] Parsed {} but expected {} → fallback",
                    batchId, parsed.size(), batch.size());
            parsed = fallbackSummaries(reqs);
        }

        List<BatchResult> out = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            out.add(new BatchResult(batch.get(i).position, parsed.get(i)));
        }

        return out;
    }

    // ===============================================================
    // SUPPORT FUNCTIONS
    // ===============================================================

    private String buildPrompt(List<SummaryRequest> batch) throws Exception {

        List<Map<String, Object>> safeItems = new ArrayList<>();

        for (SummaryRequest r : batch) {
            Map<String, Object> m = new HashMap<>(r.toPromptMap());
            Object content = m.get("content");

            if (content instanceof String s) {
                m.put("content", truncate(s, MAX_CONTENT_CHARS));
            }
            safeItems.add(m);
        }

        String json = mapper.writeValueAsString(safeItems);
        return promptTemplate.replace("{items_json}", json);
    }

    private String truncate(String s, int max) {
        return (s == null || s.length() <= max) ? s : (s.substring(0, max) + "…");
    }

    private List<SummaryResult> parseSummaries(JsonNode root) {
        JsonNode arr = root.path("summaries");
        if (!arr.isArray()) return List.of();

        List<SummaryResult> out = new ArrayList<>();
        for (JsonNode n : arr) {
            try {
                out.add(SummaryResult.fromJson(n));
            } catch (Exception ex) {
                logger.warn("Parse failed: {}", ex.getMessage());
            }
        }
        return out;
    }

    private List<BatchResult> fallbackBatch(List<PendingRequest> batch) {
        List<SummaryRequest> reqs = batch.stream().map(p -> p.request).toList();
        List<SummaryResult> fb = fallbackSummaries(reqs);

        List<BatchResult> out = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            out.add(new BatchResult(batch.get(i).position, fb.get(i)));
        }
        return out;
    }

    private List<SummaryResult> fallbackSummaries(List<SummaryRequest> list) {
        List<SummaryResult> out = new ArrayList<>();
        for (SummaryRequest r : list) {
            out.add(new SummaryResult(
                    r.getTitle(),
                    r.getUrl(),
                    List.of("⚠️ Fallback: Summary unavailable"),
                    "Content could not be processed",
                    "news"
            ));
        }
        return out;
    }

    private SummaryResult findCachedResult(FeedArticle article, Map<String, SummaryResult> cache) {
        if (article == null) return null;
        String key = CacheUtils.cacheKey(article.getUrl());
        return key == null ? null : cache.get(key);
    }

    // ===============================================================
    // INTERNAL CLASSES
    // ===============================================================
    private record PendingRequest(SummaryRequest request, int position) {}
    private record BatchResult(int position, SummaryResult summary) {}
}
