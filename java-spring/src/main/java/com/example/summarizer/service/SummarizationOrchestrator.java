package com.example.summarizer.service;

import com.example.summarizer.clients.GeminiClient;
import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryRequest;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.utils.ChunkUtils;
import com.example.summarizer.utils.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Port of the Python SummarizationService orchestration logic.
 */
public class SummarizationOrchestrator {

    private final GeminiClient client;
    private final String promptTemplate;
    private final int batchSize;
    private final ObjectMapper mapper = new ObjectMapper();

    public SummarizationOrchestrator(GeminiClient client, String promptTemplate, int batchSize) {
        this.client = client;
        this.promptTemplate = promptTemplate;
        this.batchSize = Math.max(1, batchSize);
    }

    public List<SummaryResult> summarize(List<FeedArticle> articles) throws Exception {
        List<SummaryRequest> requests = new ArrayList<>();
        for (FeedArticle a : articles) requests.add(a.toSummaryRequest());

        List<SummaryResult> results = new ArrayList<>();
        for (List<SummaryRequest> batch : ChunkUtils.chunked(requests, batchSize)) {
            String prompt = buildPrompt(batch);
            try {
                String raw = client.generate(prompt);
                String jsonBlock = JsonUtils.extractJsonBlock(raw);
                JsonNode payload = mapper.readTree(jsonBlock);
                results.addAll(parseSummaries(payload));
            } catch (com.fasterxml.jackson.core.JsonProcessingException jex) {
                results.addAll(fallbackSummaries(batch));
            } catch (Exception ex) {
                results.addAll(fallbackSummaries(batch));
            }
        }
        return results;
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
}
