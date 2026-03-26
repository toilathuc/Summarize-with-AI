package com.example.summarizer.service;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.feeds.FeedClient;
import com.example.summarizer.ports.ArticleStorePort;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.ContentEnricherPort;
import com.example.summarizer.ports.FeedPort;
import com.example.summarizer.utils.DiffUtils;
import com.example.summarizer.utils.ContentHashUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedService implements FeedPort {

    private static final Logger logger = LoggerFactory.getLogger(FeedService.class);

    private static final String DEFAULT_SOURCE = "techmeme";
    private static final int MAX_ENRICH_PER_BATCH = 20;

    private final ArticleStorePort articleRepository;
    private final FeedClient client;
    private final ContentEnricherPort contentEnricher;
    private final CachePort cacheService;
    private final MeterRegistry registry;

    private long lastRssFetch = 0;
    private static final long RSS_COOLDOWN_MS = 60_000;

    public FeedService(ArticleStorePort articleRepository,
                       FeedClient client,
                       ContentEnricherPort contentEnricher,
                       CachePort cacheService,
                       MeterRegistry registry) {
        this.articleRepository = articleRepository;
        this.client = client;
        this.contentEnricher = contentEnricher;
        this.cacheService = cacheService;
        this.registry = registry;
    }

    @Override
    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {

        long now = System.currentTimeMillis();

        List<FeedArticle> cachedArticles = articleRepository.fetchLatestFromTable(limit);
        if ((now - lastRssFetch) < RSS_COOLDOWN_MS) {
            logger.warn("RSS cooldown active, returning cached items");
            Optional<List<FeedArticle>> cachedFeed = cacheService.getFeed(limit);
            if (cachedFeed.isPresent()) {
                recordFeedMetrics("hit", cachedFeed.get().size());
                return cachedFeed.get();
            }
            return cachedArticles;
        }
        Map<String, FeedArticle> cacheMap = cachedArticles.stream()
                .filter(Objects::nonNull)
                .filter(a -> a.getUrl() != null)
                .collect(Collectors.toMap(FeedArticle::getUrl, a -> a, (a, b) -> a));
        Map<String, String> seenHashes = loadSeenHashes(cacheMap);

        lastRssFetch = now;

        List<FeedArticle> fresh = client.fetchArticles(limit);

        if (fresh.isEmpty()) {
            if (!cachedArticles.isEmpty()) {
                logger.warn("RSS fetch failed, using cached {} items", cachedArticles.size());
                return cachedArticles;
            }
            logger.warn("RSS failed and cache empty, no data available");
            return List.of();
        }

        DiffUtils.DiffResult diff = DiffUtils.diff(fresh, seenHashes);
        logger.info("Diff: {} new, {} updated, {} skipped", diff.newCount(), diff.updatedCount(), diff.skippedCount());
        recordDiffMetrics(diff);

        applyCachedContent(diff.skippedItems(), cacheMap);

        List<FeedArticle> needEnrich = new ArrayList<>();
        needEnrich.addAll(diff.newItems());
        needEnrich.addAll(diff.updatedItems());

        logger.info("Diff: {} changed/new, {} reused",
                needEnrich.size(), fresh.size() - needEnrich.size());

        if (needEnrich.size() > MAX_ENRICH_PER_BATCH) {
            logger.warn("Too many items to enrich ({}). Limiting to first {}.",
                        needEnrich.size(), MAX_ENRICH_PER_BATCH);
            needEnrich = needEnrich.subList(0, MAX_ENRICH_PER_BATCH);
        }

        if (!needEnrich.isEmpty()) {
            logger.info("Enriching {} items...", needEnrich.size());
            contentEnricher.enrich(needEnrich, true);
        }

        markSummarizedFlags(fresh, diff, cacheMap);

        articleRepository.replaceAll(fresh, DEFAULT_SOURCE);
        logger.info("Saved {} articles into SQLite cache", fresh.size());

        cacheService.putFeed(limit, fresh);
        cacheService.saveSeenHashes(diff.newHashes());

        return fresh;
    }

    private Map<String, String> loadSeenHashes(Map<String, FeedArticle> cacheMap) {
        Map<String, String> seenHashes = new HashMap<>(cacheService.loadSeenHashes());
        if (!seenHashes.isEmpty()) return seenHashes;

        for (Map.Entry<String, FeedArticle> entry : cacheMap.entrySet()) {
            FeedArticle a = entry.getValue();
            String payload = String.join("|",
                    safe(a.getUrl()),
                    safe(a.getTitle()),
                    safe(a.getDescription()),
                    safe(a.getContent())
            );
            String hash = ContentHashUtils.contentHash(payload);
            if (hash != null) {
                seenHashes.put(entry.getKey(), hash);
            }
        }
        return seenHashes;
    }

    private void applyCachedContent(List<FeedArticle> skipped, Map<String, FeedArticle> cacheMap) {
        for (FeedArticle f : skipped) {
            FeedArticle old = cacheMap.get(f.getUrl());
            if (old == null) continue;
            f.setContent(old.getContent());
        }
    }

    private void markSummarizedFlags(List<FeedArticle> fresh,
                                     DiffUtils.DiffResult diff,
                                     Map<String, FeedArticle> cacheMap) {
        Set<String> skipUrls = diff.skippedItems().stream()
                .map(FeedArticle::getUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        for (FeedArticle f : fresh) {
            if (skipUrls.contains(f.getUrl())) {
                f.setIsSummarized(true);
                continue;
            }
            FeedArticle old = cacheMap.get(f.getUrl());
            if (old != null && ContentHashUtils.isContentHashMatch(old.getContent(), f.getContent())) {
                f.setIsSummarized(true);
            } else {
                f.setIsSummarized(false);
            }
        }
    }

    private void recordDiffMetrics(DiffUtils.DiffResult diff) {
        if (registry == null) return;
        registry.counter("summarizer_refresh_articles_total", "result", "new").increment(diff.newCount());
        registry.counter("summarizer_refresh_articles_total", "result", "updated").increment(diff.updatedCount());
        registry.counter("summarizer_refresh_articles_total", "result", "skipped").increment(diff.skippedCount());
    }

    private void recordFeedMetrics(String event, int count) {
        if (registry == null) return;
        registry.counter("summarizer_feed_items_total", "event", event).increment(count);
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
