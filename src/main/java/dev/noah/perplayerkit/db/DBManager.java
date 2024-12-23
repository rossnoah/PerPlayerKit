package dev.noah.perplayerkit.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBManager {


    private final PerPlayerKitDatabase db;

    public DBManager(PerPlayerKitDatabase db) {
        this.db = db;
    }

    public void createTable() {
        PreparedStatement ps;
        try {
            ps = db.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS kits "
                    + "(KITID VARCHAR(100),KITDATA TEXT(15000), PRIMARY KEY (KITID) )");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void keepAlive() {
        PreparedStatement ps;

        try {
            ps = db.getConnection().prepareStatement("SELECT 1");
            ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void saveMySQLKit(String kitID, String data) {

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

    public String getMySQLKit(String kitID) {
        if (exists(kitID)) {
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


    public boolean exists(String kitID) {

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

    public boolean deleteKitSQL(String kitID) {

        try {
            PreparedStatement ps = db.getConnection().prepareStatement("DELETE FROM kits WHERE KITID=?");
            ps.setString(1, kitID);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
