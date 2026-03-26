package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryRequest;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.utils.ChunkUtils;
import com.example.summarizer.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SummarizationOrchestrator implements SummarizeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SummarizationOrchestrator.class);
    private static final int MAX_PARALLEL_BATCHES = 2;
    private static final Duration BATCH_TIMEOUT = Duration.ofSeconds(120);
    private static final int BATCH_RETRY = 2;
    private static final int CIRCUIT_THRESHOLD = 3;
    private static final int MAX_CONTENT_CHARS = 500;

    private final SummarizerPort client;
    private final CachePort cache;
    private final String promptTemplate;
    private final int batchSize;
    private final Semaphore semaphore = new Semaphore(MAX_PARALLEL_BATCHES);
    private final ExecutorService pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicInteger failures = new AtomicInteger(0);
    private volatile boolean circuitOpen = false;

    public SummarizationOrchestrator(SummarizerPort client, CachePort cache, String promptTemplate, int batchSize) {
        this.client = client;
        this.cache = cache;
        this.promptTemplate = promptTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public List<SummaryResult> summarize(List<FeedArticle> articles) throws Exception {
        if (articles == null || articles.isEmpty()) return List.of();

        List<SummaryResult> ordered = new ArrayList<>(Collections.nCopies(articles.size(), null));
        List<PendingRequest> pending = new ArrayList<>();
        int reused = 0;

        for (int i = 0; i < articles.size(); i++) {
            FeedArticle art = articles.get(i);
            if (art.getIsSummarized()) {
                SummaryResult result = cache.getSummaryResult(art.getUrl()).orElse(null);
                if (result != null) {
                    ordered.set(i, result);
                    reused++;
                } else {
                    pending.add(new PendingRequest(art.toSummaryRequest(), i));
                }
            } else {
                pending.add(new PendingRequest(art.toSummaryRequest(), i));
            }
        }

        logger.info("Summaries: {} reused, {} pending", reused, pending.size());
        if (pending.isEmpty()) return ordered;

        List<List<PendingRequest>> batches = ChunkUtils.chunked(pending, batchSize);
        List<Future<List<BatchResult>>> futures = new ArrayList<>();
        int batchId = 0;

        for (List<PendingRequest> batch : batches) {
            int myId = batchId++;

            semaphore.acquire();
            futures.add(pool.submit(() -> {
                try {
                    Thread.sleep(15000);
                    return processBatchWithRetry(batch, myId);
                } finally {
                    semaphore.release();
                }
            }));
        }

        for (Future<List<BatchResult>> future : futures) {
            for (BatchResult result : future.get()) {
                ordered.set(result.position, result.summary);
            }
        }

        return ordered;
    }

    private List<BatchResult> processBatchWithRetry(List<PendingRequest> batch, int batchId) {
        if (circuitOpen) {
            logger.warn("Circuit OPEN, fallback batch {}", batchId);
            return fallbackBatch(batch);
        }

        for (int attempt = 0; attempt <= BATCH_RETRY; attempt++) {
            try {
                List<BatchResult> results = processBatch(batch, batchId);
                failures.set(0);
                circuitOpen = false;
                return results;
            } catch (Exception ex) {
                logger.warn("Batch {} attempt {} failed: {}", batchId, attempt + 1, ex.getMessage());

                int failCount = failures.incrementAndGet();
                if (failCount >= CIRCUIT_THRESHOLD) {
                    circuitOpen = true;
                    logger.error("CIRCUIT OPEN after {} failures. All next batches fallback.", failCount);
                }

                if (attempt == BATCH_RETRY) break;
            }
        }

        return fallbackBatch(batch);
    }

    private List<BatchResult> processBatch(List<PendingRequest> batch, int batchId) throws Exception {
        List<SummaryRequest> reqs = batch.stream().map(p -> p.request).toList();
        String prompt = buildPrompt(reqs);

        logger.info("[Batch {}] running {} items", batchId, batch.size());

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
            logger.warn("[Batch {}] Parsed {} but expected {} -> fallback", batchId, parsed.size(), batch.size());
            parsed = fallbackSummaries(reqs);
        }

        List<BatchResult> out = new ArrayList<>();
        for (int i = 0; i < batch.size(); i++) {
            out.add(new BatchResult(batch.get(i).position, parsed.get(i)));
        }
        return out;
    }

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
        return (s == null || s.length() <= max) ? s : (s.substring(0, max) + "...");
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
                    List.of("Fallback: Summary unavailable"),
                    "Content could not be processed",
                    "news"
            ));
        }
        return out;
    }

    private record PendingRequest(SummaryRequest request, int position) {}
    private record BatchResult(int position, SummaryResult summary) {}
}
