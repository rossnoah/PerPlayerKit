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
package dev.noah.perplayerkit.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class SQLite implements SQLDatabase {

    private final Plugin plugin;
    private final String databasePath;
    private HikariDataSource dataSource;

    public SQLite(Plugin plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder() + File.separator + "database.db";
    }

    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed());
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) {
            // Ensure plugin data folder exists
            plugin.getDataFolder().mkdirs();

            HikariConfig config = new HikariConfig();

            // Modern SQLite JDBC URL with optimized parameters (2025 best practices)
            config.setJdbcUrl("jdbc:sqlite:" + databasePath +
                    "?journal_mode=WAL" + // WAL mode for better concurrency
                    "&synchronous=NORMAL" + // Balance between safety and performance
                    "&cache_size=10000" + // 10MB cache (negative = KB, positive = pages)
                    "&temp_store=MEMORY" + // Use memory for temp tables
                    "&mmap_size=268435456" + // 256MB memory-mapped I/O
                    "&optimize" + // Run PRAGMA optimize on connection
                    "&foreign_keys=ON" + // Enable foreign key constraints
                    "&busy_timeout=30000"); // 30 second busy timeout

            config.setDriverClassName("org.sqlite.JDBC");
            config.setPoolName("SQLite-Pool");

            // SQLite-specific pool configuration (optimized for single-writer architecture)
            config.setMaximumPoolSize(1); // SQLite is single-writer, use 1 connection
            config.setMinimumIdle(1); // Keep one connection alive
            config.setConnectionTimeout(30000); // 30 seconds connection timeout
            config.setIdleTimeout(300000); // 5 minutes idle timeout (reduced from 10)
            config.setMaxLifetime(1800000); // 30 minutes max connection lifetime
            config.setLeakDetectionThreshold(60000); // 60 seconds leak detection
            config.setKeepaliveTime(30000); // 30 seconds keepalive (HikariCP 4.0+)

            // Modern HikariCP settings for better performance
            config.setInitializationFailTimeout(10000); // 10 seconds initialization timeout
            config.setValidationTimeout(5000); // 5 seconds validation timeout
            config.setConnectionTestQuery("SELECT 1"); // Lightweight connection test
            config.setAutoCommit(true); // SQLite default
            config.setReadOnly(false); // Allow writes
            config.setIsolateInternalQueries(false); // Don't isolate internal queries
            config.setRegisterMbeans(false); // Disable JMX for performance
            config.setAllowPoolSuspension(true); // Allow pool suspension for maintenance

            // SQLite-specific connection properties (optimized for modern usage)
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "500"); // Increased from 250
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "4096"); // Increased from 2048
            config.addDataSourceProperty("useServerPrepStmts", "false"); // SQLite doesn't support server-side prepared
                                                                         // statements
            config.addDataSourceProperty("rewriteBatchedStatements", "true"); // Optimize batch operations
            config.addDataSourceProperty("cacheResultSetMetadata", "true"); // Cache metadata for performance
            config.addDataSourceProperty("cacheServerConfiguration", "true"); // Cache server config
            config.addDataSourceProperty("elideSetAutoCommits", "true"); // Optimize auto-commit calls
            config.addDataSourceProperty("maintainTimeStats", "false"); // Disable time stats for performance

            dataSource = new HikariDataSource(config);
        }
    }

    public void disconnect() throws SQLException {
        if (isConnected()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            try {
                connect();
            } catch (ClassNotFoundException e) {
                throw new SQLException("Failed to load SQLite driver", e);
            }
        }
        return dataSource.getConnection();
    }
}
