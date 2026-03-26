package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummaryStorePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class StorageService implements SummaryStorePort {

    private static final Logger logger = Logger.getLogger(StorageService.class.getName());

    private final Path databasePath;
    private final SQLiteDataSource dataSource;
    private final ObjectMapper mapper = new ObjectMapper();

    public StorageService(@Value("${storage.database-path}") String databasePathStr) {
        Path path = Paths.get(databasePathStr);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        this.databasePath = path;

        try {
            Files.createDirectories(this.databasePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory for SQLite cache: " + this.databasePath, e);
        }

        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl("jdbc:sqlite:" + this.databasePath.toAbsolutePath());

        initDatabase();
        logger.info(" StorageService initialized. SQLite path: " + this.databasePath.toAbsolutePath());
    }

    private void initDatabase() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS summary_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT,
                        url TEXT,
                        bullets TEXT,
                        why_it_matters TEXT,
                        type TEXT,
                        created_at TEXT NOT NULL
                    )
                    """);
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS summary_metadata (
                        key TEXT PRIMARY KEY,
                        value TEXT NOT NULL
                    )
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite schema", e);
        }
    }

    public Path save(List<SummaryResult> summaries) throws IOException {
        return save(summaries, Map.of());
    }

    @Override
    public Path save(List<SummaryResult> summaries, Map<String, Object> extra) throws IOException {
        List<SummaryResult> safeSummaries = summaries == null ? List.of() : summaries;
        Map<String, Object> metadata = new HashMap<>();
        if (extra != null) metadata.putAll(extra);
        metadata.put("total_items", safeSummaries.size());
        metadata.putIfAbsent("last_updated", OffsetDateTime.now(ZoneOffset.UTC).toString());

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                truncateSummaries(conn);
                insertSummaries(conn, safeSummaries, metadata.get("last_updated").toString());
                persistMetadata(conn, metadata);
                conn.commit();
            } catch (IOException | SQLException ex) {
                conn.rollback();
                if (ex instanceof IOException ioEx) {
                    throw ioEx;
                }
                throw new IOException("Failed to persist summaries into SQLite", ex);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to persist summaries into SQLite", e);
        }

        logger.info(" Saved summaries to SQLite cache at " + databasePath.toAbsolutePath());
        return databasePath;
    }

    private void truncateSummaries(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM summary_items");
        }
    }

    private void insertSummaries(Connection conn, List<SummaryResult> summaries, String createdAt) throws SQLException, IOException {
        if (summaries.isEmpty()) return;

        String sql = "INSERT INTO summary_items (title, url, bullets, why_it_matters, type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SummaryResult summary : summaries) {
                ps.setString(1, summary.getTitle());
                ps.setString(2, summary.getUrl());
                ps.setString(3, serializeList(summary.getBullets()));
                ps.setString(4, summary.getWhyItMatters());
                ps.setString(5, summary.getType());
                ps.setString(6, createdAt);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void persistMetadata(Connection conn, Map<String, Object> metadata) throws SQLException, IOException {
        if (metadata == null || metadata.isEmpty()) return;
        String sql = "INSERT INTO summary_metadata(key, value) VALUES (?, ?) " +
                "ON CONFLICT(key) DO UPDATE SET value = excluded.value";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, mapper.writeValueAsString(entry.getValue()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    @Override
    public SummaryPayload loadExisting() throws IOException {
        try (Connection conn = dataSource.getConnection()) {
            List<SummaryResult> summaries = fetchSummaries(conn);
            Map<String, Object> metadata = fetchMetadata(conn);

            if (summaries.isEmpty() && metadata.isEmpty()) {
                logger.warning(" No cached summaries found in SQLite at " + databasePath.toAbsolutePath());
                return null;
            }

            SummaryPayload payload = new SummaryPayload(summaries);
            metadata.forEach(payload::putExtra);
            return payload;
        } catch (SQLException e) {
            throw new IOException("Failed to load summaries from SQLite", e);
        }
    }

    private List<SummaryResult> fetchSummaries(Connection conn) throws SQLException, IOException {
        List<SummaryResult> summaries = new ArrayList<>();
        String sql = "SELECT title, url, bullets, why_it_matters, type FROM summary_items ORDER BY id ASC";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SummaryResult result = new SummaryResult();
                result.setTitle(rs.getString("title"));
                result.setUrl(rs.getString("url"));
                result.setBullets(deserializeList(rs.getString("bullets")));
                result.setWhyItMatters(rs.getString("why_it_matters"));
                result.setType(rs.getString("type"));
                summaries.add(result);
            }
        }
        return summaries;
    }

    private Map<String, Object> fetchMetadata(Connection conn) throws SQLException, IOException {
        Map<String, Object> metadata = new HashMap<>();
        String sql = "SELECT key, value FROM summary_metadata";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                metadata.put(rs.getString("key"), deserializeValue(rs.getString("value")));
            }
        }
        return metadata;
    }

    private String serializeList(List<String> values) throws JsonProcessingException {
        List<String> safe = values == null ? List.of() : values;
        return mapper.writeValueAsString(safe);
    }

    private List<String> deserializeList(String json) throws IOException {
        if (json == null || json.isBlank()) return new ArrayList<>();
        var node = mapper.readTree(json);
        List<String> out = new ArrayList<>();
        if (!node.isArray()) return out;
        node.forEach(n -> out.add(n.asText()));
        return out;
    }

    private Object deserializeValue(String json) throws IOException {
        if (json == null) return null;
        var node = mapper.readTree(json);
        if (node.isTextual()) return node.asText();
        if (node.isNumber()) return node.numberValue();
        if (node.isBoolean()) return node.booleanValue();
        if (node.isNull()) return null;
        if (node.isArray()) return mapper.convertValue(node, List.class);
        return mapper.convertValue(node, Map.class);
    }
}
