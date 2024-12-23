package dev.noah.perplayerkit.db;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL implements SQLDatabase {

    Plugin pl = PerPlayerKit.getPlugin();

    private final String host = pl.getConfig().getString("database.host");
    private final String port = pl.getConfig().getString("database.port");
    private final String database = pl.getConfig().getString("database.dbname");
    private final String username = pl.getConfig().getString("database.username");
    private final String password = pl.getConfig().getString("database.password");


    private Connection connection;

    public boolean isConnected() {
        return (connection != null);
    }


    public void connect() throws ClassNotFoundException, SQLException {
        if (!isConnected()) {
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=false",
                    username, password);
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
