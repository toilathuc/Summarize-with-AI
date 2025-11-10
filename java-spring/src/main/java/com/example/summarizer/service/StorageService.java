package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class StorageService {

    private static final Logger logger = Logger.getLogger(StorageService.class.getName());
    private final Path outputPath;
    private final ObjectMapper mapper = new ObjectMapper();

    public StorageService(@Value("${storage.output-path}") String outputPathStr) {
        Path path = Paths.get(outputPathStr);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        this.outputPath = path;

        try {
            Files.createDirectories(this.outputPath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create output directory: " + this.outputPath, e);
        }

        logger.info("✅ StorageService initialized. Output path: " + this.outputPath.toAbsolutePath());
    }

    public Path save(List<SummaryResult> summaries) throws IOException {
        return save(summaries, Map.of());
    }

    public Path save(List<SummaryResult> summaries, Map<String, Object> extra) throws IOException {
        ObjectNode root = mapper.createObjectNode();
        root.set("items", mapper.valueToTree(summaries));

        root.put("last_updated", OffsetDateTime.now(ZoneOffset.UTC).toString());
        root.put("total_items", summaries == null ? 0 : summaries.size());

        if (extra != null) {
            for (Map.Entry<String, Object> entry : extra.entrySet()) {
                root.putPOJO(entry.getKey(), entry.getValue());
            }
        }

        Path tmp = outputPath.resolveSibling(outputPath.getFileName().toString() + ".tmp");
        Files.writeString(tmp, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
        Files.move(tmp, outputPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

        logger.info("💾 Saved summaries to " + outputPath.toAbsolutePath());
        return outputPath;
    }

    public SummaryPayload loadExisting() throws IOException {
        if (!Files.exists(outputPath)) {
            logger.warning("⚠️ summaries.json not found at " + outputPath.toAbsolutePath());
            return null;
        }

        logger.info("📖 Loading summaries from " + outputPath.toAbsolutePath());
        return mapper.readValue(outputPath.toFile(), SummaryPayload.class);
    }
}
