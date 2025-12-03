package com.example.summarizer.pipelines;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.ClockPort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.ports.RefreshNewsUseCase;
import com.example.summarizer.ports.SummarizeUseCase;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.utils.CacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
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
    public Path run(int topN) throws Exception {
        List<FeedArticle> articles = feedPort.fetchLatest(topN);
        Map<String, SummaryResult> cache = CacheUtils.loadSummaryCache(summaryStore, logger);
        List<SummaryResult> summaries = summarizer.summarize(articles, cache);

        Map<String, Object> extra = new HashMap<>();
        extra.put("last_updated", clock.nowUtc().toString());
        extra.put("total_items", summaries.size());

        return summaryStore.save(summaries, extra);
    }

}
