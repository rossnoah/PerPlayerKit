package net.vanillapractice.perplayerkit.sql;

import net.vanillapractice.perplayerkit.PerPlayerKit;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLGetter {

    private final PerPlayerKit plugin;
    public SQLGetter(PerPlayerKit plugin){
        this.plugin = plugin;
    }

    public void createTable(){
        PreparedStatement ps;

        try{
            ps = plugin.database.getConnection().prepareStatement("CREATE TABLE IF NOT EXISTS kits "
                            + "(KITID VARCHAR(100),KITDATA TEXT(15000), PRIMARY KEY (KITID) )" );
            ps.executeUpdate();
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    public void keepAlive(){
        PreparedStatement ps;

        try{
            ps = plugin.database.getConnection().prepareStatement("SELECT 1" );
            ps.executeQuery();
        }catch (SQLException e){
            e.printStackTrace();
        }

    }

    public void saveMySQLKit(String kitID, String data){

        try {
            /*if(!exists(kitID)){
                PreparedStatement ps = plugin.SQL.getConnection().prepareStatement("INSERT INTO kits" +
                        " (KITID,KITDATA) VALUES (?,?)");
                ps.setString(1,kitID);
                ps.setString(2,data);
                ps.executeUpdate();


           }else{
                PreparedStatement delete = plugin.SQL.getConnection().prepareStatement("DELETE FROM kits WHERE KITID=?");
                delete.setString(1,kitID);
                delete.executeUpdate();

             */
                PreparedStatement ps = plugin.database.getConnection().prepareStatement("REPLACE INTO kits" +
                        " (KITID,KITDATA) VALUES (?,?)");
                ps.setString(1,kitID);
                ps.setString(2,data);
                ps.executeUpdate();
           // }


        }catch (SQLException e){
            e.printStackTrace();
        }
    }

    public String getMySQLKit(String kitID) {
        if (exists(kitID)) {
            try {

                PreparedStatement ps = plugin.database.getConnection().prepareStatement("SELECT KITDATA FROM kits WHERE KITID=?");
                ps.setString(1,kitID);
                ResultSet rs = ps.executeQuery();
                String kitdata;



//                while (rs.next()) {
//                    for (int i = 1; i <= 1; i++) {
//                        System.out.print(rs.getString(i) + "\t");
//                    }
//                    System.out.println();
//                }

                if(rs.next()){
//                    PerPlayerKit.getPlugin().getLogger().info(rs.toString());



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


    public boolean exists(String kitID){

        try{
            PreparedStatement ps = plugin.database.getConnection().prepareStatement("SELECT * FROM kits WHERE KITID=?");
            ps.setString(1,kitID);
            ResultSet results = ps.executeQuery();
            if(results.next()){
                return true;
            }
            return false;
        }catch (SQLException e){
            e.printStackTrace();
        }
        return  false;
    }

    public boolean deleteKitSQL(String kitID){

        try{
            PreparedStatement ps = plugin.database.getConnection().prepareStatement("DELETE FROM kits WHERE KITID=?");
            ps.setString(1,kitID);
            ps.executeUpdate();
            return true;
        }catch (SQLException e){
            e.printStackTrace();
            return false;
        }
    }

}
