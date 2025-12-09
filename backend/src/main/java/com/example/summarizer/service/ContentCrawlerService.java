package com.example.summarizer.service;

import com.example.summarizer.clients.FirecrawlClient;
import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.ports.ContentEnricherPort;
import com.example.summarizer.utils.ChunkUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class ContentCrawlerService implements ContentEnricherPort {

    private static final Logger logger = LoggerFactory.getLogger(ContentCrawlerService.class);

    private final FirecrawlClient firecrawlClient;
    private final int minContentLength;
    private final boolean alwaysRefresh;

    // ⚡ Virtual threads → scalable, nhẹ hơn fixed-thread
    private final ExecutorService pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    // Mỗi batch chỉ 2 bài để tránh 429
    private static final int BATCH_SIZE = 2;

    public ContentCrawlerService(
            FirecrawlClient firecrawlClient,
            @Value("${firecrawl.minContentLength:100}") int minContentLength,
            @Value("${firecrawl.alwaysRefresh:false}") boolean alwaysRefresh
    ) {
        this.firecrawlClient = firecrawlClient;
        this.minContentLength = Math.max(0, minContentLength);
        this.alwaysRefresh = alwaysRefresh;
    }

    @Override
    public void enrich(List<FeedArticle> articles, boolean forceRefresh) {

        if (articles == null || articles.isEmpty()) return;
        if (!firecrawlClient.isEnabled()) return;

        // 1) Lọc ra only những bài cần enrich
        List<FeedArticle> targets = articles.stream()
                .filter(Objects::nonNull)
                .filter(a -> shouldRefresh(a, forceRefresh))
                .filter(a -> a.getUrl() != null && !a.getUrl().isBlank())
                .collect(Collectors.toList());

        if (targets.isEmpty()) return;

        logger.debug("Enrich start → {} articles (batch size = {})",
                targets.size(), BATCH_SIZE);

        // 2) Chia batch
        List<List<FeedArticle>> batches = ChunkUtils.chunked(targets, BATCH_SIZE);

        for (List<FeedArticle> batch : batches) {

            List<Callable<Void>> tasks = new ArrayList<>();

            for (FeedArticle article : batch) {
                tasks.add(() -> {
                    try {
                        Optional<String> md = firecrawlClient.fetchMarkdown(article.getUrl());

                        if (md.isPresent() && !md.get().isBlank()) {
                            article.setContent(md.get());
                            logger.debug("[Firecrawl] Enriched → {}", article.getTitle());
                        } else {
                            logger.debug("[Firecrawl] Empty/ignored → {}", article.getTitle());
                        }

                    } catch (Exception ex) {
                        logger.warn("Firecrawl enrich failed for '{}': {}", article.getTitle(), ex.getMessage());
                    }
                    return null;
                });
            }

            try {
                // 3) Thực thi từng batch → giảm 429 cực mạnh
                pool.invokeAll(tasks);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Enrich batch interrupted", e);
                break;
            }
        }

        logger.debug("Enrich completed.");
    }

    /**
     * Quyết định xem bài có cần crawl lại không
     */
    private boolean shouldRefresh(FeedArticle article, boolean forceRefresh) {
        if (forceRefresh || alwaysRefresh) return true;

        String content = article.getContent();
        return content == null || content.isBlank() || content.length() < minContentLength;
    }
}
