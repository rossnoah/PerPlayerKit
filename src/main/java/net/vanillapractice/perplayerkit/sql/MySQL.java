package net.vanillapractice.perplayerkit.sql;

import net.vanillapractice.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQL implements PerPlayerKitDatabase{
/*

*/
Plugin pl = PerPlayerKit.getPlugin();

    private String host = pl.getConfig().getString("database.host");
    private String port = pl.getConfig().getString("database.port");
    private String database = pl.getConfig().getString("database.dbname");
    private String username = pl.getConfig().getString("database.username");
    private String password = pl.getConfig().getString("database.password");

 //*/



    private Connection connection;

    public boolean isConnected(){
        return (connection!=null);
    }


    public void connect()throws ClassNotFoundException, SQLException {
        if(!isConnected()){
            connection = DriverManager.getConnection("jdbc:mysql://" +
                            host + ":" + port + "/" + database + "?useSSL=false",
                    username, password);
        }
    }

    public void disconnect(){
        if(isConnected()){
            try{
                connection.close();
            } catch (SQLException e){
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection(){
        return connection;
    }

}
