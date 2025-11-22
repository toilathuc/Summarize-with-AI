package com.example.summarizer.pipelines;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.ClockPort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.RefreshNewsUseCase;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewsPipeline implements RefreshNewsUseCase {

    private static final Logger logger = LoggerFactory.getLogger(NewsPipeline.class);

    private final FeedPort feedPort;
    private final SummarizeUseCase summarizer;
    private final SummaryStorePort summaryStore;
    private final ClockPort clock;

    public NewsPipeline(FeedPort feedPort, SummarizeUseCase summarizer, SummaryStorePort summaryStore, ClockPort clock) {
        this.feedPort = feedPort;
        this.summarizer = summarizer;
        this.summaryStore = summaryStore;
        this.clock = clock;
    }

    @Override
    public java.nio.file.Path run(int topN) throws Exception {
        List<com.example.summarizer.domain.FeedArticle> articles = feedPort.fetchLatest(topN);
        Map<String, SummaryResult> cache = loadSummaryCache();
        List<SummaryResult> summaries = summarizer.summarize(articles, cache);

        Map<String, Object> extra = new HashMap<>();
        extra.put("last_updated", clock.nowUtc().toString());
        extra.put("total_items", summaries.size());

        return summaryStore.save(summaries, extra);
    }

    private Map<String, SummaryResult> loadSummaryCache() {
        try {
            SummaryPayload payload = summaryStore.loadExisting();
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
