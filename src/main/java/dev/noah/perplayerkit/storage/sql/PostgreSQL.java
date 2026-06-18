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

import java.sql.Connection;
import java.sql.SQLException;

public class PostgreSQL implements SQLDatabase {

    private final Plugin plugin;
    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private final boolean useSSL;

    private HikariDataSource dataSource;

    public PostgreSQL(Plugin plugin) {
        this.plugin = plugin;
        host = plugin.getConfig().getString("postgresql.host");
        port = plugin.getConfig().getString("postgresql.port");
        database = plugin.getConfig().getString("postgresql.dbname");
        username = plugin.getConfig().getString("postgresql.username");
        password = plugin.getConfig().getString("postgresql.password");
        useSSL = plugin.getConfig().getBoolean("postgresql.useSSL", false);
    }

    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed());
    }

    public void connect() {
        if (!isConnected()) {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(plugin.getConfig().getInt("postgresql.maximumPoolSize", 10));
            config.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database
                    + "?sslmode=" + (useSSL ? "require" : "disable"));
            config.setDriverClassName("org.postgresql.Driver");
            config.setUsername(username);
            config.setPassword(password);

            dataSource = new HikariDataSource(config);
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

    @Override
    public String getCreateTableStatement() {
        // PostgreSQL TEXT takes no length modifier.
        return "CREATE TABLE IF NOT EXISTS kits (KITID VARCHAR(100), KITDATA TEXT, PRIMARY KEY (KITID))";
    }

    @Override
    public String getUpsertStatement() {
        // PostgreSQL has no REPLACE INTO; use INSERT ... ON CONFLICT.
        return "INSERT INTO kits (KITID, KITDATA) VALUES (?,?) "
                + "ON CONFLICT (KITID) DO UPDATE SET KITDATA = EXCLUDED.KITDATA";
    }

}
