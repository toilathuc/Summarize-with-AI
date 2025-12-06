package com.example.summarizer.service;

import com.example.summarizer.ports.LoadSummariesQuery;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CachedSummariesQuery implements LoadSummariesQuery {

    private final LoadSummariesQuery delegate;
    private final NewsCacheService cache;

    public CachedSummariesQuery(LoadSummariesQuery delegate, NewsCacheService cache) {
        this.delegate = delegate;
        this.cache = cache;
    }

    @Override
    public Map<String, Object> getSummaries() throws IOException {
        Optional<Map<String, Object>> cached = cache.getSummaries();
        if (cached.isPresent()) {
            return new LinkedHashMap<>(cached.get());
        }

        Map<String, Object> data = delegate.getSummaries();
        cache.putSummaries(data);
        return data;
    }

    public void evict() {
        cache.evictSummaries();
    }
}
