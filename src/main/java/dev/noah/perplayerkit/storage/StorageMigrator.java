/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import org.bukkit.plugin.Plugin;

import java.util.Set;
import java.util.function.Consumer;

public class StorageMigrator {

    private final Plugin plugin;

    public StorageMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Migrate all data from source storage to destination storage.
     *
     * @param sourceType      The source storage type (sqlite, mysql, redis, yml)
     * @param destinationType The destination storage type (sqlite, mysql, redis, yml)
     * @param progressCallback Callback for progress updates (can be null)
     * @return MigrationResult containing success status and statistics
     */
    public MigrationResult migrate(String sourceType, String destinationType, Consumer<String> progressCallback) {
        if (sourceType.equalsIgnoreCase(destinationType)) {
            return new MigrationResult(false, 0, 0, "Source and destination storage types are the same.");
        }

        StorageManager source = null;
        StorageManager destination = null;

        try {
            // Create storage managers
            log(progressCallback, "Creating source storage connection (" + sourceType + ")...");
            source = new StorageSelector(plugin, sourceType).getDbManager();

            log(progressCallback, "Creating destination storage connection (" + destinationType + ")...");
            destination = new StorageSelector(plugin, destinationType).getDbManager();

            // Connect to both
            log(progressCallback, "Connecting to source storage...");
            source.connect();
            source.init();

            log(progressCallback, "Connecting to destination storage...");
            destination.connect();
            destination.init();

            // Get all kit IDs from source
            log(progressCallback, "Fetching all kit IDs from source...");
            Set<String> kitIDs = source.getAllKitIDs();
            int total = kitIDs.size();
            log(progressCallback, "Found " + total + " entries to migrate.");

            if (total == 0) {
                return new MigrationResult(true, 0, 0, "No data to migrate.");
            }

            // Migrate each kit
            int migrated = 0;
            int failed = 0;

            for (String kitID : kitIDs) {
                try {
                    String data = source.getKitDataByID(kitID);
                    if (data != null && !data.equals("Error") && !data.equals("error")) {
                        destination.saveKitDataByID(kitID, data);
                        migrated++;
                    } else {
                        failed++;
                        log(progressCallback, "Warning: Could not read data for kit ID: " + kitID);
                    }

                    // Progress update every 100 entries
                    if (migrated % 100 == 0) {
                        log(progressCallback, "Progress: " + migrated + "/" + total + " entries migrated...");
                    }
                } catch (Exception e) {
                    failed++;
                    log(progressCallback, "Error migrating kit ID " + kitID + ": " + e.getMessage());
                }
            }

            log(progressCallback, "Migration complete! Migrated: " + migrated + ", Failed: " + failed);
            return new MigrationResult(true, migrated, failed, null);

        } catch (StorageConnectionException | StorageOperationException e) {
            return new MigrationResult(false, 0, 0, "Connection error: " + e.getMessage());
        } finally {
            // Close connections
            if (source != null) {
                try {
                    source.close();
                } catch (StorageConnectionException e) {
                    plugin.getLogger().warning("Failed to close source storage: " + e.getMessage());
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (StorageConnectionException e) {
                    plugin.getLogger().warning("Failed to close destination storage: " + e.getMessage());
                }
            }
        }
    }

    private void log(Consumer<String> callback, String message) {
        plugin.getLogger().info("[Migration] " + message);
        if (callback != null) {
            callback.accept(message);
        }
    }

    public static class MigrationResult {
        private final boolean success;
        private final int migratedCount;
        private final int failedCount;
        private final String errorMessage;

        public MigrationResult(boolean success, int migratedCount, int failedCount, String errorMessage) {
            this.success = success;
            this.migratedCount = migratedCount;
            this.failedCount = failedCount;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getMigratedCount() {
            return migratedCount;
        }

        public int getFailedCount() {
            return failedCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
