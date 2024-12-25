package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLStorage implements StorageManager {


    private final SQLDatabase db;

    public SQLStorage(SQLDatabase db) {
        this.db = db;
    }


    private void createTable() throws SQLException{
        PreparedStatement ps;
            ps = db.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS kits "
                    + "(KITID VARCHAR(100),KITDATA TEXT(15000), PRIMARY KEY (KITID) )");
            ps.executeUpdate();

    }

    @Override
    public void init() throws StorageOperationException {
        try {
            createTable();
        }catch (SQLException e) {
           throw new StorageOperationException("Failed to initialize the database", e);
        }
    }

    @Override
    public void connect() throws StorageConnectionException {
        try{
            db.connect();
        }catch (ClassNotFoundException | SQLException e) {
            throw new StorageConnectionException("Failed to connect to the database", e);
        }
    }

    @Override
    public boolean isConnected() {
        return db.isConnected();
    }

    @Override
    public void close() throws StorageConnectionException {
        try {
            db.disconnect();
        } catch (SQLException e) {
            throw new StorageConnectionException("Failed to close the database connection", e);
        }
    }

    @Override
    public void keepAlive() throws StorageConnectionException {
        PreparedStatement ps;

        try {
            ps = db.getConnection().prepareStatement("SELECT 1");
            ps.executeQuery();
        } catch (SQLException e) {
            throw new StorageConnectionException("Failed to keep the connection alive", e);
        }

    }

    @Override
    public void saveKitDataByID(String kitID, String data) {

        try {
            PreparedStatement ps = db.getConnection().prepareStatement("REPLACE INTO kits" +
                    " (KITID,KITDATA) VALUES (?,?)");
            ps.setString(1, kitID);
            ps.setString(2, data);
            ps.executeUpdate();


        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        if (doesKitExistByID(kitID)) {
            try {

                PreparedStatement ps = db.getConnection().prepareStatement("SELECT KITDATA FROM kits WHERE KITID=?");
                ps.setString(1, kitID);
                ResultSet rs = ps.executeQuery();
                String kitdata;

                if (rs.next()) {
                    kitdata = rs.getString(1);
                    return kitdata;
                }

            } catch (SQLException e) {
                e.printStackTrace();
                return "Error";
            }
        }
        return "Error";
    }

    @Override
    public boolean doesKitExistByID(String kitID) {

        try {
            PreparedStatement ps = db.getConnection().prepareStatement("SELECT * FROM kits WHERE KITID=?");
            ps.setString(1, kitID);
            ResultSet results = ps.executeQuery();
            return results.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void deleteKitByID(String kitID) {

        try {
            PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM kits WHERE KITID=?");
            ps.setString(1, kitID);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
