package com.example.summarizer.utils;

/**
 * Utility methods for dealing with noisy model outputs that may include
 * extra text around a JSON blob. The strategy here is pragmatic: return
 * the substring between the first '{' and the last '}' to obtain JSON.
 */
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
