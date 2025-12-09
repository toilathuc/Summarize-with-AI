package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.ports.CachePort;
import com.example.summarizer.ports.LoadSummariesQuery;
import com.example.summarizer.ports.SummaryStorePort;
import com.example.summarizer.utils.PayloadToMapUtils;

import java.io.IOException;
import java.util.*;

public record SummarizationService(SummaryStorePort summaryStore,
                                   CachePort cache) implements LoadSummariesQuery {

    @Override
    public Map<String, Object> getSummaries() throws IOException {

        Map<String, Object> summaries = cache.getSummaries().orElse(Map.of());
        if (!summaries.isEmpty()) {
            return new LinkedHashMap<>(summaries);
        }

        SummaryPayload payload = summaryStore.loadExisting();
        if (payload == null) {
            throw new IOException("No summaries found. Run summarization pipeline first.");
        }
        Map<String, Object> summariesCache = PayloadToMapUtils.convertPayloadToMap(payload);
        cache.evictSummaries();
        cache.putSummaries(summariesCache);
        return new LinkedHashMap<>(summariesCache);
    }
}
