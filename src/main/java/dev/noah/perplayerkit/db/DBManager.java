package dev.noah.perplayerkit.db;

public interface DBManager {


    void init(); //called to initialize the db, create tables/collections, etc

    void keepAlive(); //called in case this db type requires a keep alive

    void saveKitDataByID(String kitID, String data);

    String getKitDataByID(String kitID);

    boolean doesKitExistByID(String kitID);

    void deleteKitByID(String kitID);
}

