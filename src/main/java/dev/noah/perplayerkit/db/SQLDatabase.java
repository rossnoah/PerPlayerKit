package dev.noah.perplayerkit.db;

import java.sql.Connection;
import java.sql.SQLException;

public interface SQLDatabase {

    boolean isConnected();

    void connect() throws ClassNotFoundException, SQLException;

    void disconnect();

    Connection getConnection();

}
