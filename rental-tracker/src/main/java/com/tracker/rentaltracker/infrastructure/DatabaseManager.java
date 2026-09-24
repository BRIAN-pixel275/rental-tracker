package com.tracker.rentaltracker.infrastructure;

import com.tracker.rentaltracker.exception.DatabaseConnectionException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens the SQLite connection, turns on foreign-key enforcement (off by
 * default in SQLite — miss this and constraint tests silently pass when they
 * shouldn't), and runs schema.sql on startup so the tables always exist.
 */
public class DatabaseManager {

    private final String dbUrl;
    private Connection connection;

    public DatabaseManager(String dbFilePath) {
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
    }

    public Connection getConnection() {
        if (connection == null) {
            throw new IllegalStateException("Database not initialised. Call connect() first.");
        }
        return connection;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection(dbUrl);
            try (Statement pragma = connection.createStatement()) {
                pragma.execute("PRAGMA foreign_keys = ON;");
            }
            runScript("/db/schema.sql");
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Could not open database: " + dbUrl, e);
        }
    }

    /** Loads the optional dev seed data. Safe to skip entirely in production use. */
    public void loadSampleData() {
        try {
            runScript("/db/sample-data.sql");
        } catch (SQLException e) {
            throw new DatabaseConnectionException("Failed to load sample data", e);
        }
    }

    private void runScript(String resourcePath) throws SQLException {
        String script = readResource(resourcePath);
        try (Statement stmt = connection.createStatement()) {
            for (String rawStatement : script.split(";")) {
                String statement = stripLineComments(rawStatement).trim();
                if (!statement.isEmpty()) {
                    stmt.execute(statement);
                }
            }
        }
    }

    /** Naive comment stripper: good enough for this project's SQL files, not a general SQL parser. */
    private String stripLineComments(String sql) {
        StringBuilder result = new StringBuilder();
        for (String line : sql.split("\n")) {
            int commentIndex = line.indexOf("--");
            result.append(commentIndex >= 0 ? line.substring(0, commentIndex) : line).append("\n");
        }
        return result.toString();
    }

    private String readResource(String resourcePath) {
        try (var input = getClass().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            return new String(input.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + resourcePath, e);
        }
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
}
