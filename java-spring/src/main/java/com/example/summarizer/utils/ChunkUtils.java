package com.example.summarizer.utils;

import java.util.ArrayList;
import java.util.List;

public final class ChunkUtils {
    private ChunkUtils() {}

    public static <T> List<List<T>> chunked(List<T> items, int size) {
        List<List<T>> out = new ArrayList<>();
        if (items == null || items.isEmpty() || size <= 0) return out;
        for (int i = 0; i < items.size(); i += size) {
            int end = Math.min(items.size(), i + size);
            out.add(new ArrayList<>(items.subList(i, end)));
        }
        return out;
    }
}
