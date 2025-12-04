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
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedService implements FeedPort {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    private static final String DEFAULT_SOURCE = "techmeme";
    private static final int MAX_ENRICH_PER_BATCH = 20;

    private final ArticleStorePort articleRepository;
    private final TechmemeFeedClient client;
    private final ContentEnricherPort contentEnricher;

    // prevent spam-runs (Techmeme RSS rarely updates <60s)
    private long lastRssFetch = 0;
    private static final long RSS_COOLDOWN_MS = 60_000;

    public FeedService(ArticleStorePort articleRepository,
                       TechmemeFeedClient client,
                       ContentEnricherPort contentEnricher) {
        this.articleRepository = articleRepository;
        this.client = client;
        this.contentEnricher = contentEnricher;
    }

    @Override
    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {

        long now = System.currentTimeMillis();

        // Prevent hammering RSS
        if ((now - lastRssFetch) < RSS_COOLDOWN_MS) {
            logger.warn("RSS cooldown active — returning cached items");
            return articleRepository.fetchLatestFromTable(limit);
        }
        lastRssFetch = now;

        // 1) Load cache
        List<FeedArticle> cached = articleRepository.fetchLatestFromTable(limit);
        Map<String, FeedArticle> cacheMap = cached.stream()
                .collect(Collectors.toMap(FeedArticle::getUrl, a -> a, (a, b) -> a));

        // 2) Fetch RSS
        List<FeedArticle> fresh = client.fetchArticles(limit);

        if (fresh.isEmpty()) {
            if (!cached.isEmpty()) {
                logger.warn("RSS fetch failed → using cached {} items", cached.size());
                return cached;
            }
            logger.warn("RSS failed and cache empty → no data available");
            return List.of();
        }

        // 3) Diff detection
        List<FeedArticle> needEnrich = detectDiff(fresh, cacheMap);

        logger.info("Diff → {} changed/new, {} reused",
                needEnrich.size(), fresh.size() - needEnrich.size());

        // Avoid enriching too many at once
        if (needEnrich.size() > MAX_ENRICH_PER_BATCH) {
            logger.warn("Too many items to enrich ({}). Limiting to first {}.", 
                        needEnrich.size(), MAX_ENRICH_PER_BATCH);
            needEnrich = needEnrich.subList(0, MAX_ENRICH_PER_BATCH);
        }

        // 4) Enrich only changed/new
        if (!needEnrich.isEmpty()) {
            logger.info("Enriching {} items...", needEnrich.size());
            contentEnricher.enrich(needEnrich, true);
        }

        // 5) Apply summarized flags
        applySummarizedFlags(fresh, cacheMap);

        // 6) Save to DB
        articleRepository.replaceAll(fresh, DEFAULT_SOURCE);
        logger.info("Saved {} articles into SQLite cache", fresh.size());

        return fresh;
    }

    /**
     * Diff detection:
     * - NEW → enrich
     * - CHANGED → enrich
     * - SAME → reuse
     */
    private List<FeedArticle> detectDiff(List<FeedArticle> fresh,
                                         Map<String, FeedArticle> cacheMap) {

        List<FeedArticle> needEnrich = new ArrayList<>();

        for (FeedArticle f : fresh) {
            FeedArticle old = cacheMap.get(f.getUrl());

            if (old == null) {
                logger.debug("[NEW] {}", f.getTitle());
                needEnrich.add(f);
                continue;
            }

            boolean same = ContentHashUtils.isContentHashMatch(old.getDescription(), f.getDescription());

            if (!same) {
                logger.debug("[CHANGED] {}", f.getTitle());
                needEnrich.add(f);
            } else {
                logger.debug("[REUSED] {}", f.getTitle());
                f.setContent(old.getContent());
            }
        }

        return needEnrich;
    }

    /**
     * Safely mark isSummarized only if content is truly unchanged.
     */
    private void applySummarizedFlags(List<FeedArticle> fresh,
                                      Map<String, FeedArticle> cacheMap) {

        for (FeedArticle f : fresh) {
            FeedArticle old = cacheMap.get(f.getUrl());
            if (old == null) continue;

            if (ContentHashUtils.isContentHashMatch(old.getContent(), f.getContent())) {
                f.setIsSummarized(true);
            }
        }
    }
}
