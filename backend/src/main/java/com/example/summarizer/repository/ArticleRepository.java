package com.example.summarizer.repository;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.ports.ArticleStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.sqlite.SQLiteDataSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ArticleRepository implements ArticleStorePort {

    private final Path databasePath;
    private final SQLiteDataSource dataSource;

    public ArticleRepository(@Value("${storage.database-path}") String databasePathStr) {

        Path path = Paths.get(databasePathStr);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir")).resolve(path);
        }
        this.databasePath = path;

        try {
            Files.createDirectories(this.databasePath.getParent());
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory for SQLite articles: " + this.databasePath, e);
        }

        this.dataSource = new SQLiteDataSource();
        this.dataSource.setUrl("jdbc:sqlite:" + this.databasePath.toAbsolutePath());

        initSchema();
        initPragma();
    }

    /** PRAGMA tối ưu cho đọc/ghi song song */
    private void initPragma() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA busy_timeout=5000;");
            stmt.execute("PRAGMA temp_store=MEMORY;");
            stmt.execute("PRAGMA foreign_keys=ON;");

        } catch (SQLException e) {
            throw new RuntimeException("Failed to set SQLite PRAGMA", e);
        }
    }

    /** Schema + Index */
    private void initSchema() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS articles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    url TEXT,
                    content TEXT,
                    source TEXT,
                    is_summarized INTEGER DEFAULT 0,
                    created_at TEXT NOT NULL
                )
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_articles_sort
                ON articles (created_at DESC, id DESC)
            """);

            stmt.executeUpdate("""
                CREATE INDEX IF NOT EXISTS idx_articles_url
                ON articles (url)
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize articles schema", e);
        }
    }

    /** FETCH =========================================================== */

    @Override
    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {

        String sql = """
            SELECT title, url, content, is_summarized
            FROM articles
            ORDER BY created_at DESC, id DESC
        """;

        if (limit != null && limit > 0) {
            sql += " LIMIT ?";
        }

        List<FeedArticle> out = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (limit != null && limit > 0) ps.setInt(1, limit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    String url = normalizeUrl(rs.getString("url"));

                    FeedArticle article = new FeedArticle(
                            rs.getString("title"),
                            url,
                            rs.getString("content"),
                            rs.getInt("is_summarized") == 1
                    );

                    out.add(article);
                }
            }

        } catch (SQLException e) {
            throw new IOException("Failed to fetch articles from SQLite", e);
        }

        return out;
    }

    /** SAVE =========================================================== */

    @Override
    public void replaceAll(List<FeedArticle> articles, String source) throws IOException {

        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();

        try (Connection conn = dataSource.getConnection()) {

            conn.setAutoCommit(false);

            try {
                clearTable(conn);
                insertBatch(conn, articles, source, now);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw new IOException("Failed to persist articles", ex);
            }

        } catch (SQLException e) {
            throw new IOException("Failed to persist articles", e);
        }
    }

    private void clearTable(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM articles");
        }
    }

    private void insertBatch(Connection conn, List<FeedArticle> articles, String source, String createdAt)
            throws SQLException {

        if (articles == null || articles.isEmpty()) return;

        String sql = """
            INSERT INTO articles(title, url, content, source, is_summarized, created_at)
            VALUES(?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            for (FeedArticle a : articles) {

                ps.setString(1, sanitize(a.getTitle()));
                ps.setString(2, normalizeUrl(a.getUrl()));
                ps.setString(3, sanitize(a.getContent()));
                ps.setString(4, sanitize(source));
                ps.setInt(5, Boolean.TRUE.equals(a.getIsSummarized()) ? 1 : 0);
                ps.setString(6, createdAt);

                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    /** METADATA ======================================================= */

    @Override
    public boolean isEmpty() throws IOException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(1) AS total FROM articles")) {

            return rs.next() && rs.getInt("total") == 0;

        } catch (SQLException e) {
            throw new IOException("Failed to check article count", e);
        }
    }

    /** UTILS ========================================================== */

    /** RSS đôi khi có url = "#" → bỏ */
    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) return null;
        if (url.equals("#")) return null;
        return url.trim();
    }

    /** sanitize UTF-8 để tránh lỗi insert khi có emoji hoặc ký tự control */
    private String sanitize(String input) {
        if (input == null) return null;
        return new String(input.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
