package com.example.summarizer.service;

import com.example.summarizer.cache.KeyValueCacheClient;
import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.CachePort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Service
public class NewsCacheService implements CachePort {

    private static final Logger log = LoggerFactory.getLogger(NewsCacheService.class);

    private final KeyValueCacheClient cache;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String prefix;
    private final Duration summariesTtl;
    private final Duration summaryResultTtl;
    private final Duration feedTtl;
    private final Duration seenTtl;
    private final MeterRegistry registry;

    public NewsCacheService(
            KeyValueCacheClient cache,
            MeterRegistry registry,
            @Value("${redis.key-prefix:summarizer}") String prefix,
            @Value("${cache.summaries.ttl.seconds:3600}") long summariesTtlSeconds,
            @Value("${cache.summary-result.ttl.seconds:86400}") long summaryResultTtlSeconds,
            @Value("${cache.feed.ttl.seconds:120}") long feedTtlSeconds,
            @Value("${cache.seen.ttl.seconds:172800}") long seenTtlSeconds
    ) {
        this.cache = cache;
        this.registry = registry;
        this.prefix = prefix;
        this.summariesTtl = Duration.ofSeconds(Math.max(1, summariesTtlSeconds));
        this.summaryResultTtl = Duration.ofSeconds(Math.max(1, summaryResultTtlSeconds));
        this.feedTtl = Duration.ofSeconds(Math.max(1, feedTtlSeconds));
        this.seenTtl = Duration.ofSeconds(Math.max(1, seenTtlSeconds));
    }

    /* ====================== SUMMARIES ====================== */

    @Override
    public Optional<Map<String, Object>> getSummaries() {
        try {
            String raw = cache.get(summariesKey());
            if (raw == null) {
                recordCacheMetric("summary", "miss");
                return Optional.empty();
            }
            recordCacheMetric("summary", "hit");
            Map<String, Object> map = mapper.readValue(raw, new TypeReference<LinkedHashMap<String, Object>>() {});
            return Optional.of(map);
        } catch (Exception ex) {
            log.warn("Failed to read summaries cache: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void putSummaries(Map<String, Object> data) {
        if (data == null || data.isEmpty()) return;
        try {
            String payload = mapper.writeValueAsString(data);
            cache.set(summariesKey(), payload, summariesTtl);
        } catch (Exception ex) {
            log.warn("Failed to write summaries cache: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void evictSummaries() {
        cache.delete(summariesKey());
    }


    /* ====================== SUMMARY-RESULT =========================== */
    @Override
    public Optional<SummaryResult> getSummaryResult(String url) {
        try {
            String raw = cache.get(summaryResultKey(url));
            if (raw == null) {
                recordCacheMetric("summary_result", "miss");
                return Optional.empty();
            }
            recordCacheMetric("summary_result", "hit");
            SummaryResult result = mapper.readValue(raw, SummaryResult.class);
            return Optional.of(result);
        } catch (Exception ex) {
            log.warn("Failed to read summary-result cache: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void putSummaryResult(SummaryResult result) {
        try {
            String payload = mapper.writeValueAsString(result);
            cache.set(summaryResultKey(result.getUrl()), payload, summaryResultTtl);
        } catch (Exception ex) {
            log.warn("Failed to write summary-result cache: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void evictSummaryResult(String url) {
        cache.delete(summaryResultKey(url));
    }


    /* ====================== FEED =========================== */

    @Override
    public Optional<List<FeedArticle>> getFeed(Integer limit) {
        try {
            String raw = cache.get(feedKey(limit));
            if (raw == null) {
                recordCacheMetric("feed", "miss");
                return Optional.empty();
            }
            recordCacheMetric("feed", "hit");
            List<FeedArticle> list = mapper.readValue(raw, new TypeReference<List<FeedArticle>>() {});
            return Optional.of(list);
        } catch (Exception ex) {
            log.warn("Failed to read feed cache: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void putFeed(Integer limit, List<FeedArticle> articles) {
        if (articles == null || articles.isEmpty()) return;
        try {
            String payload = mapper.writeValueAsString(articles);
            cache.set(feedKey(limit), payload, feedTtl);
        } catch (Exception ex) {
            log.warn("Failed to write feed cache: {}", ex.getMessage());
        }
    }

    @Override
    @Transactional
    public void evictFeed(Integer limit) {
        cache.delete(feedKey(limit));
        // also clear the generic cache to avoid stale data for other callers
        cache.delete(feedKey(null));
    }

    /* ====================== SEEN HASHES ==================== */

    @Override
    public Map<String, String> loadSeenHashes() {
        return cache.hGetAll(seenKey());
    }

    @Override
    @Transactional
    public void saveSeenHashes(Map<String, String> hashes) {
        if (hashes == null || hashes.isEmpty()) {
            cache.delete(seenKey());
            return;
        }
        cache.hPutAll(seenKey(), hashes, seenTtl);
    }

    @Override
    @Transactional
    public void evictSeenHashes() {
        cache.delete(seenKey());
    }

    /* ====================== HELPERS ======================== */

    private String summariesKey() {
        return prefix + ":cache:summaries";
    }

    private String feedKey(Integer limit) {
        String suffix = (limit == null || limit <= 0) ? "all" : "top-" + limit;
        return prefix + ":cache:feed:" + suffix;
    }

    private String seenKey() {
        return prefix + ":seen:hashes";
    }

    private String summaryResultKey(String url) {
        return prefix + ":cache:summary-result:" + url;
    }

    private void recordCacheMetric(String cacheName, String result) {
        if (registry == null) return;
        registry.counter("summarizer_cache_requests_total",
                "cache", cacheName,
                "result", result).increment();
    }
}
