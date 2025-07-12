/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
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
                storageManager = new YAMLStorage(plugin,
                        plugin.getDataFolder() + File.separator + "please-use-a-real-database.yml");
                break;
            case "redis":
                storageManager = new RedisStorage(plugin);
                break;
            case "mysql":
                SQLDatabase db = new MySQL(plugin);
                storageManager = new SQLStorage(db);
                break;
            case "sqlite":
            default:
                // default to sqlite
                db = new SQLite(plugin);
                storageManager = new SQLStorage(db);
                break;
        }

    }

    public StorageManager getDbManager() {
        return storageManager;
    }

}
