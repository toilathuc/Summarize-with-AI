package com.example.summarizer.service;

import com.example.summarizer.cache.InMemoryCacheClient;
import com.example.summarizer.domain.FeedArticle;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class NewsCacheServiceTest {

    @Test
    void feedCacheRoundTrip() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NewsCacheService cache = new NewsCacheService(
                new InMemoryCacheClient(),
                registry,
                "summarizer:test",
                3600,
                120,
                86400
        );

        Optional<List<FeedArticle>> miss = cache.getFeed(20);
        assertTrue(miss.isEmpty(), "first read should miss");

        cache.putFeed(20, List.of(new FeedArticle("t", "https://a", "d")));
        Optional<List<FeedArticle>> hit = cache.getFeed(20);
        assertTrue(hit.isPresent(), "cache should hit after write");
        assertEquals(1, hit.get().size());

        double hits = registry.get("summarizer_cache_requests_total")
                .tag("cache", "feed")
                .tag("result", "hit")
                .counter().count();
        assertTrue(hits >= 1.0, "hit counter should increment");
    }

    @Test
    void summariesCacheRoundTrip() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        NewsCacheService cache = new NewsCacheService(
                new InMemoryCacheClient(),
                registry,
                "summarizer:test",
                3600,
                120,
                86400
        );

        Map<String, Object> payload = Map.of("items", List.of(), "count", 0);
        cache.putSummaries(payload);

        Optional<Map<String, Object>> hit = cache.getSummaries();
        assertTrue(hit.isPresent());
        assertEquals(0, hit.get().get("count"));
    }

    @Test
    void seenHashesRoundTrip() {
        NewsCacheService cache = new NewsCacheService(
                new InMemoryCacheClient(),
                new SimpleMeterRegistry(),
                "summarizer:test",
                3600,
                120,
                86400
        );

        cache.saveSeenHashes(Map.of("a", "hash-a"));
        Map<String, String> loaded = cache.loadSeenHashes();
        assertEquals("hash-a", loaded.get("a"));

        cache.evictSeenHashes();
        assertTrue(cache.loadSeenHashes().isEmpty());
    }
}
