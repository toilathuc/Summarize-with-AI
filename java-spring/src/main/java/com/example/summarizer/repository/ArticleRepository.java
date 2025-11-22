package com.example.summarizer.repository;

import com.example.summarizer.domain.FeedArticle;
import com.example.summarizer.ports.ArticleStorePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
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
    }

    private void initSchema() {
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS articles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        url TEXT,
                        content TEXT,
                        source TEXT,
                        created_at TEXT NOT NULL
                    )
                    """);
            stmt.executeUpdate("""
                    CREATE INDEX IF NOT EXISTS idx_articles_created_at
                        ON articles (created_at DESC, id DESC)
                    """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize articles schema", e);
        }
    }

    @Override
    public List<FeedArticle> fetchLatest(Integer limit) throws IOException {
        List<FeedArticle> articles = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT title, url, content FROM articles ORDER BY created_at DESC, id DESC");
        if (limit != null && limit > 0) {
            sql.append(" LIMIT ?");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (limit != null && limit > 0) {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FeedArticle article = new FeedArticle(
                            rs.getString("title"),
                            rs.getString("url"),
                            rs.getString("content")
                    );
                    articles.add(article);
                }
            }
        } catch (SQLException e) {
            throw new IOException("Failed to fetch articles from SQLite", e);
        }

        return articles;
    }

    @Override
    public void replaceAll(List<FeedArticle> articles, String source) throws IOException {
        String now = OffsetDateTime.now(ZoneOffset.UTC).toString();
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                truncateArticles(conn);
                insertArticles(conn, articles, source, now);
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw new IOException("Failed to persist articles", ex);
            }
        } catch (SQLException e) {
            throw new IOException("Failed to persist articles", e);
        }
    }

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

    private void truncateArticles(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("DELETE FROM articles");
        }
    }

    private void insertArticles(Connection conn, List<FeedArticle> articles, String source, String createdAt) throws SQLException {
        if (articles == null || articles.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO articles (title, url, content, source, created_at) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (FeedArticle article : articles) {
                ps.setString(1, article.getTitle());
                ps.setString(2, article.getUrl());
                ps.setString(3, article.getContent());
                ps.setString(4, source);
                ps.setString(5, createdAt);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
