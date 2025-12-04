package com.example.summarizer.utils;

import org.slf4j.Logger;
import org.sqlite.SQLiteDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class WalModeUtils {
    private WalModeUtils() {};

    public static void enableWalMode(SQLiteDataSource dataSource, Logger logger) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA busy_timeout = 5000;");
            stmt.execute("PRAGMA synchronous=NORMAL;");
            stmt.execute("PRAGMA temp_store=MEMORY;");
            stmt.execute("PRAGMA foreign_keys=ON;");
            logger.info("WAL mode enabled for SQLite database");

        } catch (SQLException e) {
            logger.warn("Failed to enable WAL mode: {}", e.getMessage());
        }
    }
}
