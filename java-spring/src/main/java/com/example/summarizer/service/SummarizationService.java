package com.example.summarizer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

@Service
public class SummarizationService {

    private final ObjectMapper mapper = new ObjectMapper();

    public Map<String, Object> getSummaries() throws IOException {
        Path file = Path.of("data", "outputs", "summaries.json");
        if (!Files.exists(file)) {
            throw new IOException("summaries.json not found at: " + file.toAbsolutePath());
        }

        JsonNode root = mapper.readTree(file.toFile());
        String lastUpdated = root.path("last_updated").asText(null);

        boolean isStale = checkIfStale(lastUpdated, 60);
        String freshness = calculateAge(lastUpdated);
        int count = root.path("items").isArray() ? root.path("items").size() : 0;

        Map<String, Object> out = new HashMap<>();
        out.put("items", root.path("items"));
        out.put("last_updated", lastUpdated);
        out.put("is_stale", isStale);
        out.put("freshness", freshness);
        out.put("count", count);
        return out;
    }

    private static boolean checkIfStale(String lastUpdatedStr, int thresholdMinutes) {
        if (lastUpdatedStr == null || lastUpdatedStr.isBlank()) return true;
        try {
            OffsetDateTime last = OffsetDateTime.parse(lastUpdatedStr.replace("Z", "+00:00"));
            Duration age = Duration.between(last, OffsetDateTime.now(last.getOffset()));
            return age.toMinutes() > thresholdMinutes;
        } catch (DateTimeParseException | NullPointerException ex) {
            return true;
        }
    }

    private static String calculateAge(String lastUpdatedStr) {
        if (lastUpdatedStr == null || lastUpdatedStr.isBlank()) return "Unknown";
        try {
            OffsetDateTime last = OffsetDateTime.parse(lastUpdatedStr.replace("Z", "+00:00"));
            Duration age = Duration.between(last, OffsetDateTime.now(last.getOffset()));
            long seconds = age.getSeconds();
            if (seconds < 60) return "Just now";
            if (seconds < 3600) return (seconds / 60) + " minutes ago";
            if (seconds < 86400) return (seconds / 3600) + " hours ago";
            return (seconds / 86400) + " days ago";
        } catch (DateTimeParseException | NullPointerException ex) {
            return "Unknown";
        }
    }
}
