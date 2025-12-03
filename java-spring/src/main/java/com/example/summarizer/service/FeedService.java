package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.feeds.TechmemeFeedClient;
import com.example.summarizer.ports.ArticleStorePort;
import com.example.summarizer.ports.ContentEnricherPort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.utils.ContentHashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FeedService implements FeedPort {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);
    private static final String DEFAULT_SOURCE = "techmeme";

    private final ArticleStorePort articleRepository;
    private final TechmemeFeedClient client;
    private final ContentEnricherPort contentEnricher;

    public FeedService(ArticleStorePort articleRepository,
                       TechmemeFeedClient client,
                       ContentEnricherPort contentEnricher) {
        this.articleRepository = articleRepository;
        this.client = client;
        this.contentEnricher = contentEnricher;
    }

    @Override
    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {
        List<FeedArticle> cached = articleRepository.fetchLatest(limit);
        contentEnricher.enrich(cached, false);

        // Always try to fetch fresh on demand; fall back to cache when RSS/sample is empty.
        List<FeedArticle> fresh = client.fetchArticles(limit);
        if (!fresh.isEmpty()) {
            contentEnricher.enrich(fresh, true);
            markContentHashFromCache(fresh, cached);
            articleRepository.replaceAll(fresh, DEFAULT_SOURCE);
            logger.info("Cached {} Techmeme articles fetched from RSS", fresh.size());
            if (limit != null && fresh.size() > limit) {
                logger.debug("Returning first {} articles after seeding cache", limit);
                return fresh.subList(0, limit);
            }
            return fresh;
        }

        if (!cached.isEmpty()) {
            logger.warn("RSS fetch returned no items; serving {} cached articles", cached.size());
            return cached;
        }

        logger.warn("No articles returned from RSS or cache");
        return fresh;
    }

    private void markContentHashFromCache(List<FeedArticle> fresh, List<FeedArticle> cached) {
        if (cached == null || cached.isEmpty()) return;

        Map<String, String> cachedContentByUrl = cached.stream()
                .collect(Collectors.toMap(FeedArticle::getUrl, FeedArticle::getContent, (a, b) -> a));

        fresh.forEach(f -> {
            String oldContent = cachedContentByUrl.get(f.getUrl());
            if (oldContent != null &&
                    ContentHashUtils.isContentHashMatch(oldContent, f.getContent())) {
                f.setIsSummarized(Boolean.TRUE);
            }
        });
    }

}
