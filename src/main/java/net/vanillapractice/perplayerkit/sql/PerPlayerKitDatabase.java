package net.vanillapractice.perplayerkit.sql;

import java.sql.Connection;
import java.sql.SQLException;

public interface PerPlayerKitDatabase {

    boolean isConnected();

    void connect() throws ClassNotFoundException, SQLException;

    void disconnect();

    Connection getConnection();

}
