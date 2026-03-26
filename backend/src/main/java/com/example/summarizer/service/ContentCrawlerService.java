package com.example.summarizer.service;

import com.example.summarizer.clients.CrawlClient;
import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.ports.ContentEnricherPort;
import com.example.summarizer.utils.ChunkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class ContentCrawlerService implements ContentEnricherPort {

    private static final Logger logger = LoggerFactory.getLogger(ContentCrawlerService.class);
    private static final int BATCH_SIZE = 2;

    private final CrawlClient crawlClient;
    private final int minContentLength;
    private final boolean alwaysRefresh;
    private final ExecutorService pool = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().factory());

    public ContentCrawlerService(
            CrawlClient crawlClient,
            @Value("${firecrawl.minContentLength:100}") int minContentLength,
            @Value("${firecrawl.alwaysRefresh:false}") boolean alwaysRefresh
    ) {
        this.crawlClient = crawlClient;
        this.minContentLength = Math.max(0, minContentLength);
        this.alwaysRefresh = alwaysRefresh;
    }

    @Override
    public void enrich(List<FeedArticle> articles, boolean forceRefresh) {
        if (articles == null || articles.isEmpty()) {
            return;
        }
        if (!crawlClient.isEnabled()) {
            return;
        }

        List<FeedArticle> targets = articles.stream()
                .filter(Objects::nonNull)
                .filter(a -> shouldRefresh(a, forceRefresh))
                .filter(a -> a.getUrl() != null && !a.getUrl().isBlank())
                .collect(Collectors.toList());

        if (targets.isEmpty()) {
            return;
        }

        logger.debug("Enrich start: {} articles, batch size {}", targets.size(), BATCH_SIZE);

        List<List<FeedArticle>> batches = ChunkUtils.chunked(targets, BATCH_SIZE);

        for (int i = 0; i < batches.size(); i++) {
            List<FeedArticle> batch = batches.get(i);
            List<Callable<Void>> tasks = new ArrayList<>();

            for (FeedArticle article : batch) {
                tasks.add(() -> {
                    try {
                        Optional<String> markdown = crawlClient.fetchMarkdown(article.getUrl());
                        if (markdown.isPresent() && !markdown.get().isBlank()) {
                            article.setContent(markdown.get());
                            logger.debug("Firecrawl enriched {}", article.getTitle());
                        } else {
                            article.setContent(fallbackContent(article));
                            logger.warn("Firecrawl unavailable for {}, using fallback content", article.getTitle());
                        }
                    } catch (Exception ex) {
                        article.setContent(fallbackContent(article));
                        logger.warn("Firecrawl failed for {}, using fallback content: {}", article.getTitle(), ex.getMessage());
                    }
                    return null;
                });
            }

            try {
                pool.invokeAll(tasks);
                logger.info("Firecrawl enriched batch {}/{} ({})", i + 1, batches.size(), batch.size());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.error("Firecrawl enrich interrupted at batch {}/{}: {}", i + 1, batches.size(), e.getMessage());
                break;
            }
        }

        logger.debug("Enrich completed");
    }

    private boolean shouldRefresh(FeedArticle article, boolean forceRefresh) {
        if (forceRefresh || alwaysRefresh) {
            return true;
        }

        String content = article.getContent();
        return content == null || content.isBlank() || content.length() < minContentLength;
    }

    private String fallbackContent(FeedArticle article) {
        String content = article.getContent();
        if (content != null && !content.isBlank()) {
            return content;
        }

        String description = article.getDescription();
        if (description != null && !description.isBlank()) {
            return description;
        }

        String title = article.getTitle();
        return title == null ? "" : title;
    }
}
