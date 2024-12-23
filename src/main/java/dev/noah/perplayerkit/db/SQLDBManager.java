package dev.noah.perplayerkit.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLDBManager implements DBManager{


    private final SQLDatabase db;

    public SQLDBManager(SQLDatabase db) {
        this.db = db;
    }


    private void createTable() {
        PreparedStatement ps;
        try {
            ps = db.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS kits "
                    + "(KITID VARCHAR(100),KITDATA TEXT(15000), PRIMARY KEY (KITID) )");
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(){
        createTable();
    }

    @Override
    public void close(){
        db.disconnect();
    }

    @Override
    public void keepAlive() {
        PreparedStatement ps;

        try {
            ps = db.getConnection().prepareStatement("SELECT 1");
            ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
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
