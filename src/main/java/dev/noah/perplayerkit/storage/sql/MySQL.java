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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    /**
     * MySQL's TEXT caps at 64 KB, which a kit full of shulker boxes can exceed.
     * MEDIUMTEXT allows 16 MB.
     */
    @Override
    public String getCreateTableStatement() {
        return "CREATE TABLE IF NOT EXISTS kits (KITID VARCHAR(100), KITDATA MEDIUMTEXT, PRIMARY KEY (KITID))";
    }

    /**
     * Tables created by earlier versions used TEXT(15000), which MySQL stores as TEXT.
     * Widening is lossless and older versions keep working against MEDIUMTEXT. If the
     * database user lacks ALTER permission we warn and carry on with the old limit
     * rather than refusing to start.
     */
    @Override
    public void upgradeSchema(Connection conn) {
        try {
            String type;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT DATA_TYPE FROM information_schema.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'kits' AND COLUMN_NAME = 'KITDATA'");
                 ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return;
                }
                type = rs.getString(1);
            }
            if ("text".equalsIgnoreCase(type) || "tinytext".equalsIgnoreCase(type)) {
                plugin.getLogger().info("Widening kits.KITDATA from " + type.toUpperCase() + " to MEDIUMTEXT so large kits can be saved");
                try (PreparedStatement ps = conn.prepareStatement("ALTER TABLE kits MODIFY KITDATA MEDIUMTEXT")) {
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Could not widen kits.KITDATA to MEDIUMTEXT (" + e.getMessage() + "). "
                    + "Kits over 64 KB will fail to save until you run: ALTER TABLE kits MODIFY KITDATA MEDIUMTEXT;");
        }
    }
}
