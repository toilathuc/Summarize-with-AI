package com.example.summarizer.utils;

import com.example.summarizer.domain.FeedArticle;

import java.util.*;

public final class DiffUtils {
    private DiffUtils() {}

    public record DiffResult(
            List<FeedArticle> fresh,
            List<FeedArticle> newItems,
            List<FeedArticle> updatedItems,
            List<FeedArticle> skippedItems,
            Map<String, String> newHashes
    ) {
        public int newCount() { return newItems.size(); }
        public int updatedCount() { return updatedItems.size(); }
        public int skippedCount() { return skippedItems.size(); }
    }

    public static DiffResult diff(List<FeedArticle> fresh, Map<String, String> seenHashes) {
        if (fresh == null) fresh = List.of();
        Map<String, String> seen = seenHashes == null ? Map.of() : seenHashes;

        List<FeedArticle> news = new ArrayList<>();
        List<FeedArticle> updates = new ArrayList<>();
        List<FeedArticle> skips = new ArrayList<>();
        Map<String, String> newHashes = new HashMap<>();

        for (FeedArticle item : fresh) {
            if (item == null) continue;
            String url = safe(item.getUrl());
            if (url.isBlank()) continue;

            String hash = computeHash(item);
            newHashes.put(url, hash);

            String prev = seen.get(url);

            if (prev == null) {
                news.add(item);
                continue;
            }

            if (!prev.equals(hash)) {
                updates.add(item);
            } else {
                skips.add(item);
            }
        }

        return new DiffResult(fresh, news, updates, skips, newHashes);
    }

    private static String computeHash(FeedArticle article) {
        String payload = String.join("|",
                safe(article.getUrl()),
                safe(article.getTitle()),
                safe(article.getDescription()),
                safe(article.getContent())
        );
        return ContentHashUtils.contentHash(payload);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
