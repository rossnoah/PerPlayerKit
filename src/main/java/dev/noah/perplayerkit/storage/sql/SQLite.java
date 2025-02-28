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
        this.databasePath = new File(plugin.getDataFolder(),
                plugin.getConfig().getString("sqlite.filename", "database.db")).getAbsolutePath();
    }

    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed());
    }

    public void connect() {
        if (!isConnected()) {
            try {
                Class.forName("org.sqlite.JDBC");

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl("jdbc:sqlite:" + databasePath);
                config.setMaximumPoolSize(plugin.getConfig().getInt("sqlite.maximumPoolSize", 5));

                // SQLite-specific settings to help with locking issues
                config.addDataSourceProperty("journal_mode", "WAL");
                config.addDataSourceProperty("synchronous", "NORMAL");
                config.addDataSourceProperty("foreign_keys", "true");
                config.addDataSourceProperty("busy_timeout", "3000");

                // Connection testing
                config.setConnectionTestQuery("SELECT 1");
                config.setMinimumIdle(1);

                dataSource = new HikariDataSource(config);
            } catch (ClassNotFoundException e) {
                plugin.getLogger().severe("SQLite JDBC driver not found: " + e.getMessage());
            }
        }
    }

    public void disconnect() {
        if (isConnected()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        if (!isConnected()) {
            connect();
        }
        return dataSource.getConnection();
    }
}