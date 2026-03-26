package com.example.summarizer.service;

import com.example.summarizer.clients.CrawlClient;
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

    private final CrawlClient firecrawlClient;
    private final int minContentLength;
    private final boolean alwaysRefresh;

    private final ExecutorService pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    private static final int BATCH_SIZE = 2;

    public ContentCrawlerService(
            CrawlClient firecrawlClient,
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

        List<FeedArticle> targets = articles.stream()
                .filter(Objects::nonNull)
                .filter(a -> shouldRefresh(a, forceRefresh))
                .filter(a -> a.getUrl() != null && !a.getUrl().isBlank())
                .collect(Collectors.toList());

        if (targets.isEmpty()) return;

        logger.debug("Enrich start → {} articles (batch size = {})",
                targets.size(), BATCH_SIZE);

        List<List<FeedArticle>> batches = ChunkUtils.chunked(targets, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {

            List<FeedArticle> batch = batches.get(i);

            List<Callable<Void>> tasks = new ArrayList<>();

            for (FeedArticle article : batch) {
                tasks.add(() -> {
                    try {
                        Optional<String> md = firecrawlClient.fetchMarkdown(article.getUrl());

                        if (md.isPresent() && !md.get().isBlank()) {
                            article.setContent(md.get());
                            logger.debug("Firecrawl enriched {}", article.getTitle());
                        } else {
                            logger.debug("Firecrawl empty or ignored {}", article.getTitle());
                        }

                    } catch (Exception ex) {
                        logger.warn("Firecrawl enrich failed for '{}': {}", article.getTitle(), ex.getMessage());
                    }
                    return null;
                });
            }

            try {
            pool.invokeAll(tasks);
            logger.info("Firecrawl enriched batch {}/{} ({} articles)",
                    i + 1, batches.size(), batch.size());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Firecrawl enrich interrupted at batch {}/{}: {}", i + 1, batches.size(), e.getMessage());
                break;
            }
        }

        logger.debug("Enrich completed.");
    }

    private boolean shouldRefresh(FeedArticle article, boolean forceRefresh) {
        if (forceRefresh || alwaysRefresh) return true;

        String content = article.getContent();
        return content == null || content.isBlank() || content.length() < minContentLength;
    }
}
