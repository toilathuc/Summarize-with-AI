package com.example.summarizer.utils;

public final class JsonUtils {
    private JsonUtils() {}

    public static String extractJsonBlock(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }
}
