package com.example.summarizer.utils;

public final class CacheKeyUtils {
    private CacheKeyUtils() {};

    public static String cacheKey(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.trim().toLowerCase();
    };
}
