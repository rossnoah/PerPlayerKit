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
        host = plugin.getConfig().getString("storage.mysql.host");
        port = plugin.getConfig().getString("storage.mysql.port");
        database = plugin.getConfig().getString("storage.mysql.dbname");
        username = plugin.getConfig().getString("storage.mysql.username");
        password = plugin.getConfig().getString("storage.mysql.password");
        useSSL = plugin.getConfig().getBoolean("storage.mysql.use-ssl", false);
    }

    private HikariDataSource dataSource;

    public boolean isConnected() {
        return (dataSource != null && !dataSource.isClosed());
    }


    public void connect() {
        if (!isConnected()) {
            HikariConfig config = new HikariConfig();
            config.setMaximumPoolSize(plugin.getConfig().getInt("storage.mysql.maximum-pool-size",10));
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
