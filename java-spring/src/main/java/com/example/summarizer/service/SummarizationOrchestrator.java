package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryRequest;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummarizerPort;
import com.example.summarizer.utils.CacheKeyUtils;
import com.example.summarizer.utils.ChunkUtils;
import com.example.summarizer.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Port of the Python SummarizationService orchestration logic.
 */
public class SummarizationOrchestrator implements SummarizeUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SummarizationOrchestrator.class);

    private final SummarizerPort client;
    private final String promptTemplate;
    private final int batchSize;
    private final ObjectMapper mapper = new ObjectMapper();

    public SummarizationOrchestrator(SummarizerPort client, String promptTemplate, int batchSize) {
        this.client = client;
        this.promptTemplate = promptTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    @Override
    public List<SummaryResult> summarize(List<FeedArticle> articles) throws Exception {
        return summarize(articles, null);
    }

    @Override
    public List<SummaryResult> summarize(List<FeedArticle> articles, Map<String, SummaryResult> cache) throws Exception {
        Map<String, SummaryResult> safeCache = cache == null ? Map.of() : cache;
        List<SummaryResult> orderedResults = new ArrayList<>(Collections.nCopies(articles.size(), null));
        List<PendingRequest> pending = new ArrayList<>();

        int reused = 0;
        for (int i = 0; i < articles.size(); i++) {
            FeedArticle article = articles.get(i);
            SummaryResult cached = findCachedResult(article, safeCache);
            if (cached != null && article.getIsSummarized()) {
                orderedResults.set(i, cached);
                reused++;
            } else {
                pending.add(new PendingRequest(article.toSummaryRequest(), i));
            }
        }

        int generated = 0;
        for (List<PendingRequest> batch : ChunkUtils.chunked(pending, batchSize)) {
            List<SummaryRequest> requests = new ArrayList<>();
            for (PendingRequest req : batch) requests.add(req.request);
            List<SummaryResult> batchResults;
            try {
                String prompt = buildPrompt(requests);
                String raw = client.generate(prompt);
                String jsonBlock = JsonUtils.extractJsonBlock(raw);
                JsonNode payload = mapper.readTree(jsonBlock);
                batchResults = parseSummaries(payload);
            } catch (com.fasterxml.jackson.core.JsonProcessingException jex) {
                batchResults = fallbackSummaries(requests);
            } catch (Exception ex) {
                batchResults = fallbackSummaries(requests);
            }
            generated += batchResults.size();
            for (int i = 0; i < batch.size(); i++) {
                SummaryResult result = i < batchResults.size()
                        ? batchResults.get(i)
                        : fallbackSummaries(List.of(batch.get(i).request)).get(0);
                orderedResults.set(batch.get(i).position, result);
            }
        }

        for (int i = 0; i < orderedResults.size(); i++) {
            if (orderedResults.get(i) == null) {
                SummaryRequest fallbackReq = articles.get(i).toSummaryRequest();
                orderedResults.set(i, fallbackSummaries(List.of(fallbackReq)).get(0));
            }
        }

        logger.debug("Summaries reused: {}, generated: {}", reused, orderedResults.size() - reused);
        return orderedResults;
    }

    private String buildPrompt(List<SummaryRequest> batch) throws Exception {
        List<Object> items = new ArrayList<>();
        for (SummaryRequest r : batch) items.add(r.toPromptMap());
        String json = mapper.writeValueAsString(items);
        return promptTemplate.replace("{items_json}", json);
    }

    private List<SummaryResult> parseSummaries(JsonNode payload) {
        List<SummaryResult> out = new ArrayList<>();
        JsonNode arr = payload.path("summaries");
        if (arr.isArray()) {
            for (JsonNode n : arr) out.add(SummaryResult.fromJson(n));
        }
        return out;
    }

    private List<SummaryResult> fallbackSummaries(List<SummaryRequest> batch) {
        List<SummaryResult> out = new ArrayList<>();
        for (SummaryRequest r : batch) {
            out.add(new SummaryResult(
                    r.getTitle(),
                    r.getUrl(),
                    List.of("Summary not available due to parsing error"),
                    "Content needs manual review",
                    "news"
            ));
        }
        return out;
    }

    private SummaryResult findCachedResult(FeedArticle article, Map<String, SummaryResult> cache) {
        if (cache.isEmpty() || article == null) return null;
        String key = CacheKeyUtils.cacheKey(article.getUrl());
        if (key == null) return null;
        return cache.get(key);
    }

    private static class PendingRequest {
        private final SummaryRequest request;
        private final int position;

        private PendingRequest(SummaryRequest request, int position) {
            this.request = request;
            this.position = position;
        }
    }
}
