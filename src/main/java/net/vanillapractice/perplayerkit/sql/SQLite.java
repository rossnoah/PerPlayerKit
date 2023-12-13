package net.vanillapractice.perplayerkit.sql;

import net.vanillapractice.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLite implements PerPlayerKitDatabase {

    Plugin pl = PerPlayerKit.getPlugin();

    private final String databasePath = pl.getDataFolder() + File.separator + "database.db"; // Change to your SQLite database file path

    private Connection connection;

    public boolean isConnected() {
        return (connection != null);
    }

    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        }
    }

    public void disconnect() {
        if (isConnected()) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
