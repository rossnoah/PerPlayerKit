package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.sql.MySQL;
import dev.noah.perplayerkit.storage.sql.SQLDatabase;
import dev.noah.perplayerkit.storage.sql.SQLite;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class StorageSelector {


    private StorageManager storageManager;
    private Plugin plugin;

    public StorageSelector(Plugin plugin, String storageType) {

        this.plugin = plugin;

        switch (storageType) {

            case "yml":
            case "yaml":
                storageManager = new YAMLStorage(plugin.getDataFolder() + File.separator + "please-use-a-real-database.yml");
                break;
            case "redis":
                storageManager = new RedisStorage();
                break;
            case "mysql":
                SQLDatabase db = new MySQL();
                storageManager = new SQLStorage(db);
                break;
            case "sqlite":
            default:
                //default to sqlite
                db = new SQLite(plugin.getDataFolder() + File.separator + "database.db");
                storageManager = new SQLStorage(db);
                break;
        }

    }

    public StorageManager getDbManager() {
        return storageManager;
    }


}
