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
import dev.noah.perplayerkit.ConfigManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL implements SQLDatabase {

    private Plugin plugin;
    private final String host;
    private final String port;
    private final String database;
    private final String username;
    private final String password;
    private boolean useSSL = false;

    public MySQL(Plugin plugin) {
        this.plugin = plugin;
        host = ConfigManager.get().getMySQLHost();
        port = ConfigManager.get().getMySQLPort();
        database = ConfigManager.get().getMySQLDatabase();
        username = ConfigManager.get().getMySQLUsername();
        password = ConfigManager.get().getMySQLPassword();
        useSSL = ConfigManager.get().getMySQLUseSSL();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("useSSL", useSSL);
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useUnicode", "true");
        config.setMaximumPoolSize(ConfigManager.get().getMySQLMaximumPoolSize());

        dataSource = new HikariDataSource(config);
    }

    private HikariDataSource dataSource;

    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed());
    }

    public void connect() {
        if (!isConnected()) {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(ConfigManager.get().getMySQLMaximumPoolSize());
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL);
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

}
