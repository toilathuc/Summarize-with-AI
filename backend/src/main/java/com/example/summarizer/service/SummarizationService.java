package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.ports.ClockPort;
import com.example.summarizer.ports.LoadSummariesQuery;
import com.example.summarizer.ports.SummaryStorePort;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

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
            throw new IOException("No summaries found. Run summarization pipeline first.");
        }

        Map<String, Object> extras = new HashMap<>(payload.getExtra());

        String lastUpdated = stringValue(extras.get("last_updated"));
        OffsetDateTime parsedTime = parseTimeSafe(lastUpdated);

        boolean isStale = parsedTime == null || isOlderThan(parsedTime, Duration.ofMinutes(60));
        String freshness = parsedTime == null ? "Unknown" : formatAge(parsedTime);

        List<?> items = payload.getSummaries();
        int count = items == null ? 0 : items.size();

        // ensure correlation id exists
        extras.putIfAbsent("correlation_id", UUID.randomUUID().toString());

        // API response shape
        Map<String, Object> response = new LinkedHashMap<>();
        response.putAll(extras);
        response.put("items", items);
        response.put("count", count);
        response.put("last_updated", lastUpdated);
        response.put("is_stale", isStale);
        response.put("freshness", freshness);

        return response;
    }

    private static String stringValue(Object v) {
        return v == null ? null : v.toString();
    }

    private OffsetDateTime parseTimeSafe(String val) {
        if (val == null || val.isBlank()) return null;

        try {
            // auto handle Z, +00:00, offset variations
            return OffsetDateTime.parse(val);
        } catch (DateTimeParseException e) {
            try {
                // fallback: replace Z → UTC
                return OffsetDateTime.parse(val.replace("Z", "+00:00"));
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private boolean isOlderThan(OffsetDateTime timestamp, Duration threshold) {
        OffsetDateTime now = clock.nowUtc();
        Duration age = Duration.between(timestamp, now);
        return age.compareTo(threshold) > 0;
    }

    private String formatAge(OffsetDateTime timestamp) {
        Duration age = Duration.between(timestamp, clock.nowUtc());
        long seconds = age.getSeconds();

        if (seconds < 60) return "Just now";

        long minutes = seconds / 60;
        if (minutes < 60) return minutes + " minutes ago";

        long hours = minutes / 60;
        if (hours < 24) {
            long remainMin = minutes % 60;
            return remainMin == 0 ? (hours + " hours ago")
                    : (hours + "h " + remainMin + "m ago");
        }

        long days = hours / 24;
        return days + " days ago";
    }
}
