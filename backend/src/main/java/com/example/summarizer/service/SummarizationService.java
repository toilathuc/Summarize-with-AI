package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.ports.ClockPort;
import com.example.summarizer.ports.LoadSummariesQuery;
import com.example.summarizer.ports.SummaryStorePort;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SummarizationService implements LoadSummariesQuery {

    private final SummaryStorePort summaryStore;
    private final ClockPort clock;

    public SummarizationService(SummaryStorePort summaryStore, ClockPort clock) {
        this.summaryStore = summaryStore;
        this.clock = clock;
    }

    @Override
    public Map<String, Object> getSummaries() throws IOException {
        SummaryPayload payload = summaryStore.loadExisting();
        if (payload == null) {
            throw new IOException("No summaries cached yet. Please run the pipeline first.");
        }

        Map<String, Object> extras = new HashMap<>(payload.getExtra());
        String lastUpdated = toStringValue(extras.get("last_updated"));
        boolean isStale = checkIfStale(lastUpdated, 60);
        String freshness = calculateAge(lastUpdated);

        List<?> items = payload.getSummaries();
        int count = items == null ? 0 : items.size();

        Map<String, Object> response = new HashMap<>(extras);
        response.put("items", payload.getSummaries());
        response.put("last_updated", lastUpdated);
        response.put("is_stale", isStale);
        response.put("freshness", freshness);
        response.put("count", count);
        return response;
    }

    private boolean checkIfStale(String lastUpdatedStr, int thresholdMinutes) {
        if (lastUpdatedStr == null || lastUpdatedStr.isBlank()) return true;
        try {
            OffsetDateTime last = OffsetDateTime.parse(lastUpdatedStr.replace("Z", "+00:00"));
            Duration age = Duration.between(last, clock.nowUtc());
            return age.toMinutes() > thresholdMinutes;
        } catch (DateTimeParseException | NullPointerException ex) {
            return true;
        }
    }

    private String calculateAge(String lastUpdatedStr) {
        if (lastUpdatedStr == null || lastUpdatedStr.isBlank()) return "Unknown";
        try {
            OffsetDateTime last = OffsetDateTime.parse(lastUpdatedStr.replace("Z", "+00:00"));
            Duration age = Duration.between(last, clock.nowUtc());
            long seconds = age.getSeconds();
            if (seconds < 60) return "Just now";
            if (seconds < 3600) return (seconds / 60) + " minutes ago";
            if (seconds < 86400) return (seconds / 3600) + " hours ago";
            return (seconds / 86400) + " days ago";
        } catch (DateTimeParseException | NullPointerException ex) {
            return "Unknown";
        }
    }

    private static String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
