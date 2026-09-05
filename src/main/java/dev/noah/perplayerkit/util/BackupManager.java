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
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.storage.BackupCapable;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class BackupManager {

    private static BackupManager instance;
    private final Plugin plugin;
    private final StorageManager storageManager;
    private final File backupDir;
    private final boolean enabled;
    private BukkitTask hourlyTask;
    private BukkitTask dailyTask;
    private BukkitTask weeklyTask;
    private BukkitTask monthlyTask;
    private BukkitTask cleanupTask;

    // Time constants
    private static final long HOUR_IN_TICKS = 20 * 60 * 60; // 1 hour in ticks
    private static final long DAY_IN_TICKS = HOUR_IN_TICKS * 24; // 1 day in ticks
    private static final long WEEK_IN_TICKS = DAY_IN_TICKS * 7; // 1 week in ticks
    private static final long MONTH_IN_TICKS = DAY_IN_TICKS * 30; // 1 month in ticks

    // Retention periods in milliseconds
    private static final long HOURLY_RETENTION = TimeUnit.HOURS.toMillis(24); // 24 hours
    private static final long DAILY_RETENTION = TimeUnit.DAYS.toMillis(7); // 7 days
    private static final long WEEKLY_RETENTION = TimeUnit.DAYS.toMillis(30); // 30 days
    private static final long MONTHLY_RETENTION = TimeUnit.DAYS.toMillis(365); // 365 days

    // Filename patterns
    private static final String HOURLY_PREFIX = "hourly_";
    private static final String DAILY_PREFIX = "daily_";
    private static final String WEEKLY_PREFIX = "weekly_";
    private static final String MONTHLY_PREFIX = "monthly_";

    public BackupManager(Plugin plugin, StorageManager storageManager) {
        this.plugin = plugin;
        this.storageManager = storageManager;
        this.enabled = plugin.getConfig().getBoolean("storage.backup.enabled", true);
        this.backupDir = new File(plugin.getDataFolder(), "backups");

        if (enabled) {
            initializeBackupDirectory();
            scheduleBackups();
        }

        instance = this;
    }

    public static BackupManager get() {
        return instance;
    }

    private void initializeBackupDirectory() {
        if (!backupDir.exists()) {
            if (backupDir.mkdirs()) {
                plugin.getLogger().info("Created backup directory: " + backupDir.getAbsolutePath());
            } else {
                plugin.getLogger().warning("Failed to create backup directory: " + backupDir.getAbsolutePath());
            }
        }
    }

    private void scheduleBackups() {
        plugin.getLogger().info("Scheduling automatic backups...");

        // Schedule hourly backups (every hour)
        hourlyTask = new BukkitRunnable() {
            @Override
            public void run() {
                performBackup(HOURLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(plugin, HOUR_IN_TICKS, HOUR_IN_TICKS);

        // Schedule daily backups (every day)
        dailyTask = new BukkitRunnable() {
            @Override
            public void run() {
                performBackup(DAILY_PREFIX);
            }
        }.runTaskTimerAsynchronously(plugin, DAY_IN_TICKS, DAY_IN_TICKS);

        // Schedule weekly backups (every week)
        weeklyTask = new BukkitRunnable() {
            @Override
            public void run() {
                performBackup(WEEKLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(plugin, WEEK_IN_TICKS, WEEK_IN_TICKS);

        // Schedule monthly backups (every month)
        monthlyTask = new BukkitRunnable() {
            @Override
            public void run() {
                performBackup(MONTHLY_PREFIX);
            }
        }.runTaskTimerAsynchronously(plugin, MONTH_IN_TICKS, MONTH_IN_TICKS);

        // Schedule cleanup task (every 6 hours)
        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupOldBackups();
            }
        }.runTaskTimerAsynchronously(plugin, HOUR_IN_TICKS * 6, HOUR_IN_TICKS * 6);

        plugin.getLogger().info("Automatic backups scheduled successfully");
    }

    /**
     * Perform a backup of all file-based storage
     * 
     * @param prefix The prefix for the backup filename
     */
    public void performBackup(String prefix) {
        if (!enabled) {
            return;
        }

        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

            backupDatabase(prefix, timestamp);

            // Backup YAML storage
            File yamlFile = new File(plugin.getDataFolder(), "please-use-a-real-database.yml");
            if (yamlFile.exists()) {
                File backupFile = new File(backupDir, prefix + "yaml-storage_" + timestamp + ".yml");
                Files.copy(yamlFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Backed up YAML storage to: " + backupFile.getName());
            }

            // Backup any other file-based storage files
            backupAdditionalFiles(prefix, timestamp);

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to perform backup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Back up the SQLite database. The database engine writes the snapshot itself
     * (see {@link BackupCapable}), which is consistent even while the server is
     * writing; copying the live file is only used if the engine cannot do it.
     */
    private void backupDatabase(String prefix, String timestamp) throws IOException {
        File databaseFile = new File(plugin.getDataFolder(), "database.db");
        if (!databaseFile.exists()) {
            return;
        }

        File backupFile = new File(backupDir, prefix + "database_" + timestamp + ".db");

        if (storageManager instanceof BackupCapable capable && capable.supportsNativeBackup()) {
            try {
                capable.backupTo(backupFile.toPath());
                plugin.getLogger().info("Backed up SQLite database to: " + backupFile.getName());
                return;
            } catch (StorageOperationException e) {
                plugin.getLogger().warning("SQLite snapshot backup failed (" + e.getMessage()
                        + "), falling back to copying the database files");
            }
        }

        copyDatabaseFiles(prefix, timestamp, databaseFile, backupFile);
    }

    /**
     * Fallback used when the database cannot snapshot itself: copy the database
     * file together with its WAL and SHM sidecars, which have to be restored
     * alongside it for the copy to be usable.
     */
    private void copyDatabaseFiles(String prefix, String timestamp, File databaseFile, File backupFile)
            throws IOException {
        Files.copy(databaseFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        plugin.getLogger().info("Backed up SQLite database to: " + backupFile.getName());

        // Write-Ahead Log
        File walFile = new File(plugin.getDataFolder(), "database.db-wal");
        if (walFile.exists()) {
            Files.copy(walFile.toPath(), new File(backupDir, prefix + "database-wal_" + timestamp + ".db").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }

        // Shared Memory index
        File shmFile = new File(plugin.getDataFolder(), "database.db-shm");
        if (shmFile.exists()) {
            Files.copy(shmFile.toPath(), new File(backupDir, prefix + "database-shm_" + timestamp + ".db").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Backup additional files that might be important for the plugin
     * 
     * @param prefix    The backup prefix
     * @param timestamp The timestamp for the backup
     */
    private void backupAdditionalFiles(String prefix, String timestamp) throws IOException {
        // Backup config.yml
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (configFile.exists()) {
            File backupFile = new File(backupDir, prefix + "config_" + timestamp + ".yml");
            Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Clean up old backups according to retention policy
     */
    private void cleanupOldBackups() {
        if (!enabled || !backupDir.exists()) {
            return;
        }

        File[] backupFiles = backupDir.listFiles();
        if (backupFiles == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();

        // Sort files by last modified time (oldest first)
        Arrays.sort(backupFiles, Comparator.comparing(File::lastModified));

        for (File file : backupFiles) {
            if (file.isFile()) {
                long fileAge = currentTime - file.lastModified();
                String fileName = file.getName();

                boolean shouldDelete = false;

                // Check retention policy based on prefix
                if (fileName.startsWith(HOURLY_PREFIX) && fileAge > HOURLY_RETENTION) {
                    shouldDelete = true;
                } else if (fileName.startsWith(DAILY_PREFIX) && fileAge > DAILY_RETENTION) {
                    shouldDelete = true;
                } else if (fileName.startsWith(WEEKLY_PREFIX) && fileAge > WEEKLY_RETENTION) {
                    shouldDelete = true;
                } else if (fileName.startsWith(MONTHLY_PREFIX) && fileAge > MONTHLY_RETENTION) {
                    shouldDelete = true;
                }

                if (shouldDelete) {
                    if (file.delete()) {
                        plugin.getLogger().info("Deleted old backup: " + fileName);
                    } else {
                        plugin.getLogger().warning("Failed to delete old backup: " + fileName);
                    }
                }
            }
        }
    }

    /**
     * Perform an immediate backup (can be called manually)
     */
    public void performManualBackup() {
        if (!enabled) {
            plugin.getLogger().warning("Backups are disabled in configuration");
            return;
        }

        plugin.getLogger().info("Performing manual backup...");
        new BukkitRunnable() {
            @Override
            public void run() {
                performBackup("manual_");
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Get the backup directory
     * 
     * @return The backup directory
     */
    public File getBackupDirectory() {
        return backupDir;
    }

    /**
     * Check if backups are enabled
     * 
     * @return True if backups are enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Get backup statistics
     * 
     * @return Array containing [hourly, daily, weekly, monthly] backup counts
     */
    public int[] getBackupCounts() {
        if (!backupDir.exists()) {
            return new int[] { 0, 0, 0, 0 };
        }

        File[] files = backupDir.listFiles();
        if (files == null) {
            return new int[] { 0, 0, 0, 0 };
        }

        int hourly = 0, daily = 0, weekly = 0, monthly = 0;

        for (File file : files) {
            if (file.isFile()) {
                String name = file.getName();
                if (name.startsWith(HOURLY_PREFIX))
                    hourly++;
                else if (name.startsWith(DAILY_PREFIX))
                    daily++;
                else if (name.startsWith(WEEKLY_PREFIX))
                    weekly++;
                else if (name.startsWith(MONTHLY_PREFIX))
                    monthly++;
            }
        }

        return new int[] { hourly, daily, weekly, monthly };
    }

    /**
     * Shutdown the backup manager and cancel all scheduled tasks
     */
    public void shutdown() {
        if (hourlyTask != null)
            hourlyTask.cancel();
        if (dailyTask != null)
            dailyTask.cancel();
        if (weeklyTask != null)
            weeklyTask.cancel();
        if (monthlyTask != null)
            monthlyTask.cancel();
        if (cleanupTask != null)
            cleanupTask.cancel();

        if (enabled) {
            plugin.getLogger().info("Backup manager shutdown complete");
        }
    }
}
