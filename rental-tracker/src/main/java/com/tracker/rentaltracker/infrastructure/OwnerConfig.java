package com.tracker.rentaltracker.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The users table has no "this is the owner" flag — it holds the owner and
 * every renter in one table, by design. So the app tracks which user row is
 * "you" in a tiny sidecar file next to the database, written once on first
 * launch and read on every launch after.
 */
public class OwnerConfig {

    private final Path configPath;

    public OwnerConfig(Path configPath) {
        this.configPath = configPath;
    }

    public Optional<Integer> readOwnerId() {
        try {
            if (!Files.exists(configPath)) {
                return Optional.empty();
            }
            String content = Files.readString(configPath).trim();
            return content.isEmpty() ? Optional.empty() : Optional.of(Integer.parseInt(content));
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    public void writeOwnerId(int ownerId) {
        try {
            Files.writeString(configPath, String.valueOf(ownerId));
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist owner id to " + configPath, e);
        }
    }
}
