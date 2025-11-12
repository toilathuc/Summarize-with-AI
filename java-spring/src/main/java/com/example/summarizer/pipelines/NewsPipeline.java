package com.example.summarizer.pipelines;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.service.FeedService;
import com.example.summarizer.service.SummarizationOrchestrator;
import com.example.summarizer.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NewsPipeline {

    private static final Logger logger = LoggerFactory.getLogger(NewsPipeline.class);

    private final FeedService feedService;
    private final SummarizationOrchestrator orchestrator;
    private final StorageService storageService;

    public NewsPipeline(FeedService feedService, SummarizationOrchestrator orchestrator, StorageService storageService) {
        this.feedService = feedService;
        this.orchestrator = orchestrator;
        this.storageService = storageService;
    }

    public java.nio.file.Path run(int topN) throws Exception {
        List<com.example.summarizer.domain.FeedArticle> articles = feedService.fetchLatest(topN);
        Map<String, SummaryResult> cache = loadSummaryCache();
        List<SummaryResult> summaries = orchestrator.summarize(articles, cache);

        Map<String, Object> extra = new HashMap<>();
        extra.put("last_updated", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
        extra.put("total_items", summaries.size());

        return storageService.save(summaries, extra);
    }

    private Map<String, SummaryResult> loadSummaryCache() {
        try {
            SummaryPayload payload = storageService.loadExisting();
            if (payload == null || payload.getSummaries() == null || payload.getSummaries().isEmpty()) {
                return Map.of();
            }
            Map<String, SummaryResult> cache = new HashMap<>();
            for (SummaryResult summary : payload.getSummaries()) {
                String key = cacheKey(summary.getUrl(), summary.getTitle());
                if (key != null && !cache.containsKey(key)) {
                    cache.put(key, summary);
                }
            }
            logger.debug("Loaded {} cached summaries for reuse", cache.size());
            return cache;
        } catch (IOException ex) {
            logger.debug("Summary cache unavailable: {}", ex.getMessage());
            return Map.of();
        }
    }

    private String cacheKey(String url, String title) {
        if (url != null && !url.isBlank()) return url.trim();
        if (title != null && !title.isBlank()) return title.trim();
        return null;
    }
}
