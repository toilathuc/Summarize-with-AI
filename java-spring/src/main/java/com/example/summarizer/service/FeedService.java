package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.feeds.TechmemeFeedClient;
import com.example.summarizer.repository.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class FeedService {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);
    private static final String DEFAULT_SOURCE = "techmeme";

    private final ArticleRepository articleRepository;
    private final TechmemeFeedClient client;
    private final ContentCrawlerService contentCrawlerService;

    public FeedService(ArticleRepository articleRepository,
                       TechmemeFeedClient client,
                       ContentCrawlerService contentCrawlerService) {
        this.articleRepository = articleRepository;
        this.client = client;
        this.contentCrawlerService = contentCrawlerService;
    }

    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {
        List<FeedArticle> cached = articleRepository.fetchLatest(limit);
        contentCrawlerService.enrich(cached, false);
        if (!cached.isEmpty()) {
            logger.debug("Serving {} articles from SQLite cache (limit={})", cached.size(), limit);
            return cached;
        }

        List<FeedArticle> fallback = client.fetchArticles(null);
        if (!fallback.isEmpty()) {
            contentCrawlerService.enrich(fallback, true);
            articleRepository.replaceAll(fallback, DEFAULT_SOURCE);
            logger.info("Cached {} Techmeme articles fetched from RSS", fallback.size());
            if (limit != null && fallback.size() > limit) {
                logger.debug("Returning first {} articles after seeding cache", limit);
                return fallback.subList(0, limit);
            }
        } else {
            logger.warn("No articles returned from RSS or sample feed");
        }
        return fallback;
    }
}
