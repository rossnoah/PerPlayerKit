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

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YAMLStorage implements StorageManager {

    private final File storageFile;
    private Map<String, String> data;
    private Plugin plugin;

    public YAMLStorage(Plugin plugin,String filePath) {
        this.plugin = plugin;
        this.storageFile = new File(filePath);
        this.data = new HashMap<>();
    }

    @Override
    public void connect() {

    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void init() {
        try {
            if (storageFile.exists()) {
                Yaml yaml = new Yaml();
                try (FileInputStream inputStream = new FileInputStream(storageFile)) {
                    Map<String, String> loadedData = yaml.load(inputStream);
                    if (loadedData != null) {
                        data = loadedData;
                    }
                }
            } else {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }
            plugin.getLogger().info("YAML storage initialized.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            saveToFile();
            plugin.getLogger().info("YAML storage closed and saved.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keepAlive() {
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        this.data.put(kitID, data);
        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        return data.getOrDefault(kitID, "error");
    }

    @Override
    public boolean doesKitExistByID(String kitID) {
        return data.containsKey(kitID);
    }

    @Override
    public void deleteKitByID(String kitID) {
        data.remove(kitID);
        try {
            saveToFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(storageFile)) {
            yaml.dump(data, writer);
        }
    }
}