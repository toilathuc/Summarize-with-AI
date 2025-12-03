package com.example.summarizer.utils;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummaryStorePort;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class CacheUtils {
    private CacheUtils() {};

    public static String cacheKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.trim().toLowerCase();
    };

    public static Map<String, SummaryResult> loadSummaryCache(SummaryStorePort summaryStore, Logger logger) {
        try {
            SummaryPayload payload = summaryStore.loadExisting();
            if (payload == null || payload.getSummaries() == null || payload.getSummaries().isEmpty()) {
                return Map.of();
            }
            Map<String, SummaryResult> cache = new HashMap<>();
            for (SummaryResult summary : payload.getSummaries()) {
                String key = CacheUtils.cacheKey(summary.getUrl());
                if (key != null && !cache.containsKey(key)) {
                    cache.put(key, summary);
                }
            }
            logger.debug("Loaded {} cached summaries for reuse", cache.size());
            return cache;
        } catch (IOException ex) {
            logger.debug("Summary cache unavailable: {}", ex.getMessage());
            return Map.of();
        }
    }
}
