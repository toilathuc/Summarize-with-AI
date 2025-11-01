package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StorageService {

    private final Path outputPath = Path.of("data", "outputs", "summaries.json");
    private final ObjectMapper mapper = new ObjectMapper();

    public Path getOutputPath() { return outputPath; }

    public Path save(List<SummaryResult> summaries) throws IOException {
        return save(summaries, Map.of());
    }

    public Path save(List<SummaryResult> summaries, Map<String, Object> extra) throws IOException {
        outputPath.getParent().toFile().mkdirs();

        ObjectNode root = mapper.createObjectNode();
        root.set("items", mapper.valueToTree(summaries));

        // Add last_updated and total_items by default
        root.put("last_updated", OffsetDateTime.now(ZoneOffset.UTC).toString());
        root.put("total_items", summaries == null ? 0 : summaries.size());

        if (extra != null) {
            for (Map.Entry<String, Object> e : extra.entrySet()) {
                root.putPOJO(e.getKey(), e.getValue());
            }
        }

        // Atomic write: write to temp file then move
        Path tmp = outputPath.resolveSibling(outputPath.getFileName().toString() + ".tmp");
        Files.writeString(tmp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        Files.move(tmp, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return outputPath;
    }

    public SummaryPayload loadExisting() throws IOException {
        if (!Files.exists(outputPath)) return null;
        return mapper.readValue(outputPath.toFile(), SummaryPayload.class);
    }
}
