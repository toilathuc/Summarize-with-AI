package com.example.summarizer.service;

import com.example.summarizer.domain.SummaryPayload;
import com.example.summarizer.domain.SummaryResult;
import com.example.summarizer.ports.SummaryStorePort;

import com.example.summarizer.utils.WalModeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class StorageService implements SummaryStorePort {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

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
        WalModeUtils.enableWalMode(dataSource, logger);

        logger.info("📦 StorageService initialized. SQLite at {}", this.databasePath.toAbsolutePath());
    }

    /** INITIALIZE SCHEMA ==================================================== */

    private void initDatabase() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS summary_items (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT,
                    url TEXT UNIQUE,
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

            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_summary_url ON summary_items(url)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_summary_created ON summary_items(created_at DESC, id DESC)");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize SQLite schema", e);
        }
    }

    /** SAVE ================================================================ */

    @Override
    public Path save(List<SummaryResult> summaries, Map<String, Object> extra) throws IOException {
        if (summaries == null) summaries = List.of();
        
        // Filter out any null entries (defense in depth)
        summaries = summaries.stream()
                .filter(Objects::nonNull)
                .toList();

        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        if (extra != null) metadata.putAll(extra);
        metadata.put("total_items", summaries.size());
        metadata.putIfAbsent("last_updated", OffsetDateTime.now(ZoneOffset.UTC).toString());

        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);

            try {
                upsertSummaries(conn, summaries, (String) metadata.get("last_updated"));
                persistMetadata(conn, metadata);
                conn.commit();

            } catch (Exception ex) {
                conn.rollback();
                throw new IOException("Failed to persist summaries into SQLite", ex);
            }

        } catch (SQLException e) {
            throw new IOException("Failed to persist summaries into SQLite", e);
        }

        logger.info("💾 Saved {} summarized items into SQLite", summaries.size());
        return databasePath;
    }

    private void upsertSummaries(Connection conn, List<SummaryResult> summaries, String createdAt)
            throws SQLException, IOException {

        if (summaries.isEmpty()) return;

        String sql = """
            INSERT INTO summary_items(title, url, bullets, why_it_matters, type, created_at)
            VALUES(?, ?, ?, ?, ?, ?)
            ON CONFLICT(url) DO UPDATE SET
                title = excluded.title,
                bullets = excluded.bullets,
                why_it_matters = excluded.why_it_matters,
                type = excluded.type,
                created_at = excluded.created_at
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (SummaryResult s : summaries) {

                if (s.getUrl() == null || s.getUrl().isBlank()) {
                    continue;
                }

                ps.setString(1, sanitize(s.getTitle()));
                ps.setString(2, s.getUrl());
                ps.setString(3, serializeList(s.getBullets()));
                ps.setString(4, sanitize(s.getWhyItMatters()));
                ps.setString(5, sanitize(s.getType()));
                ps.setString(6, createdAt);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /** METADATA ============================================================ */

    private void persistMetadata(Connection conn, Map<String, Object> metadata)
            throws SQLException, IOException {

        String sql = """
            INSERT INTO summary_metadata(key, value)
            VALUES(?, ?)
            ON CONFLICT(key) DO UPDATE SET value = excluded.value
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                ps.setString(1, entry.getKey());
                ps.setString(2, mapper.writeValueAsString(entry.getValue()));
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** LOAD ================================================================ */

    @Override
    public SummaryPayload loadExisting() throws IOException {
        try (Connection conn = dataSource.getConnection()) {

            List<SummaryResult> summaries = fetchSummaries(conn);
            Map<String, Object> metadata = fetchMetadata(conn);

            SummaryPayload payload = new SummaryPayload(summaries);
            metadata.forEach(payload::putExtra);

            return payload;

        } catch (SQLException e) {
            throw new IOException("Failed to load summaries from SQLite", e);
        }
    }

    private List<SummaryResult> fetchSummaries(Connection conn)
            throws SQLException, IOException {

        List<SummaryResult> out = new ArrayList<>();

        String sql = """
            SELECT title, url, bullets, why_it_matters, type
            FROM summary_items
            ORDER BY created_at DESC, id DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                SummaryResult s = new SummaryResult();
                s.setTitle(rs.getString("title"));
                s.setUrl(rs.getString("url"));
                s.setBullets(deserializeList(rs.getString("bullets")));
                s.setWhyItMatters(rs.getString("why_it_matters"));
                s.setType(rs.getString("type"));
                out.add(s);
            }
        }
        return out;
    }

    private Map<String, Object> fetchMetadata(Connection conn) throws SQLException, IOException {
        Map<String, Object> meta = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement("SELECT key, value FROM summary_metadata");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                meta.put(rs.getString("key"), deserializeValue(rs.getString("value")));
            }
        }

        return meta;
    }

    /** JSON UTILS ========================================================== */

    private String serializeList(List<String> list) throws JsonProcessingException {
        if (list == null) return "[]";
        return mapper.writeValueAsString(list);
    }

    private List<String> deserializeList(String json) throws IOException {
        if (json == null || json.isBlank()) return List.of();
        return mapper.readValue(json.getBytes(StandardCharsets.UTF_8), List.class);
    }

    private Object deserializeValue(String json) throws IOException {
        if (json == null) return null;
        return mapper.readValue(json.getBytes(StandardCharsets.UTF_8), Object.class);
    }

    /** SAFE STRING ========================================================= */

    private String sanitize(String s) {
        if (s == null) return null;
        return new String(s.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
