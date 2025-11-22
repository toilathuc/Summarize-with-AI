package com.example.summarizer.service;

import com.example.summarizer.clients.FirecrawlClient;
import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.ports.ContentEnricherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ContentCrawlerService implements ContentEnricherPort {

    private static final Logger logger = LoggerFactory.getLogger(ContentCrawlerService.class);

    private final FirecrawlClient firecrawlClient;
    private final int minContentLength;
    private final boolean alwaysRefresh;

    public ContentCrawlerService(
            FirecrawlClient firecrawlClient,
            @Value("${firecrawl.minContentLength:100}") int minContentLength,
            @Value("${firecrawl.alwaysRefresh:false}") boolean alwaysRefresh
    ) {
        this.firecrawlClient = firecrawlClient;
        this.minContentLength = Math.max(0, minContentLength);
        this.alwaysRefresh = alwaysRefresh;
    }

    /**
     * Populates missing article content using Firecrawl. When {@code forceRefresh} is true we will crawl
     * even if some content already exists (useful right after pulling from RSS where we only have snippets).
     */
    public void enrich(List<FeedArticle> articles, boolean forceRefresh) {
        if (articles == null || articles.isEmpty()) return;
        if (firecrawlClient == null || !firecrawlClient.isEnabled()) return;

        for (FeedArticle article : articles) {
            if (article == null) continue;
            if (!shouldRefresh(article, forceRefresh)) continue;
            Optional<String> markdown = firecrawlClient.fetchMarkdown(article.getUrl());
            if (markdown.isPresent()) {
                article.setContent(markdown.get());
                logger.debug("Enriched article '{}' with Firecrawl content", article.getTitle());
            }
        }
    }

    private boolean shouldRefresh(FeedArticle article, boolean forceRefresh) {
        if (forceRefresh || alwaysRefresh) return true;
        String content = article.getContent();
        if (content == null || content.isBlank()) return true;
        return content.length() < minContentLength;
    }
}
