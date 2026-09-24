package com.tracker.rentaltracker.testsupport;

import com.tracker.rentaltracker.infrastructure.DatabaseManager;

import java.nio.file.Path;

/** Builds a fresh, schema-loaded SQLite database in a temp file for each test. */
public final class TestDatabaseFactory {
    private TestDatabaseFactory() {}

    public static DatabaseManager freshDatabase(Path tempDir) {
        Path dbFile = tempDir.resolve("test-" + System.nanoTime() + ".db");
        DatabaseManager db = new DatabaseManager(dbFile.toString());
        db.connect();
        return db;
    }
}
