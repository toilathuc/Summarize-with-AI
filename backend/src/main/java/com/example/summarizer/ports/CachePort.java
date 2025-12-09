package com.example.summarizer.ports;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.domain.SummaryResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CachePort {
    Optional<Map<String, Object>> getSummaries();
    void putSummaries(Map<String, Object> data);
    void evictSummaries();
    Optional<SummaryResult> getSummaryResult(String url);
    void putSummaryResult(SummaryResult result);
    void evictSummaryResult(String url);
    Optional<List<FeedArticle>> getFeed(Integer limit);
    void putFeed(Integer limit, List<FeedArticle> articles);
    void evictFeed(Integer limit);
    Map<String, String> loadSeenHashes();
    void saveSeenHashes(Map<String, String> seenHashes);
    void evictSeenHashes();
}
