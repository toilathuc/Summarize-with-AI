package com.example.summarizer.pipelines;

import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.service.FeedService;
import com.example.summarizer.service.SummarizationOrchestrator;
import com.example.summarizer.service.StorageService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NewsPipeline {

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
        List<SummaryResult> summaries = orchestrator.summarize(articles);

        Map<String, Object> extra = new HashMap<>();
        extra.put("last_updated", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
        extra.put("total_items", summaries.size());

        return storageService.save(summaries, extra);
    }
}
