package dev.noah.perplayerkit.storage.sql;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL implements SQLDatabase {

    Plugin pl = PerPlayerKit.getPlugin();

    private final String host = pl.getConfig().getString("mysql.host");
    private final String port = pl.getConfig().getString("mysql.port");
    private final String database = pl.getConfig().getString("mysql.dbname");
    private final String username = pl.getConfig().getString("mysql.username");
    private final String password = pl.getConfig().getString("mysql.password");


    private Connection connection;

    public boolean isConnected() {
        return (connection != null);
    }


    public void connect() throws SQLException {
        if (!isConnected()) {
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=false",
                    username, password);
        }
    }

    public void disconnect() throws SQLException {
        if (isConnected()) {
                connection.close();
        }
    }

    public Connection getConnection() {
        return connection;
    }

}
