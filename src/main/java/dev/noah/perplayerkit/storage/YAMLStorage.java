/*
 * Copyright 2022-2026 Noah Ross
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

import dev.noah.perplayerkit.storage.exceptions.KitStorageException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static dev.noah.perplayerkit.storage.exceptions.KitStorageException.Operation.*;

public class YAMLStorage implements StorageManager {

    private final File storageFile;
    private Map<String, String> data;
    private boolean initialized;
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
    public synchronized void init() throws StorageOperationException {
        initialized = false;
        try {
            if (storageFile.exists()) {
                Yaml yaml = new Yaml();
                try (FileInputStream inputStream = new FileInputStream(storageFile)) {
                    Object loaded = yaml.load(inputStream);
                    Map<String, String> restored = new HashMap<>();
                    if (loaded != null) {
                        if (!(loaded instanceof Map<?, ?> entries)) throw new IllegalArgumentException("Expected kit ID/data pairs");
                        for (var entry : entries.entrySet()) {
                            if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof String value))
                                throw new IllegalArgumentException("Kit IDs and data must be strings");
                            restored.put(key, value);
                        }
                    }
                    data = restored;
                }
            } else {
                storageFile.getParentFile().mkdirs();
                storageFile.createNewFile();
            }
            initialized = true;
            plugin.getLogger().info("YAML storage initialized.");
        } catch (IOException | RuntimeException e) {
            throw new StorageOperationException("Could not initialize YAML kit storage", e);
        }
    }

    @Override
    public synchronized void close() {
        if (!initialized) return;
        try {
            saveToFile();
            plugin.getLogger().info("YAML storage closed and saved.");
        } catch (IOException e) {
            throw new IllegalStateException("Could not save YAML kit storage", e);
        }
    }

    @Override
    public void keepAlive() {
    }

    @Override
    public synchronized void saveKitDataByID(String kitID, String data) {
        this.data.put(kitID, data);
        try {
            saveToFile();
        } catch (IOException e) {
            throw new KitStorageException(SAVE, kitID, e);
        }
    }

    @Override
    public synchronized String getKitDataByID(String kitID) {
        return data.getOrDefault(kitID, "error");
    }

    @Override
    public synchronized boolean doesKitExistByID(String kitID) {
        return data.containsKey(kitID);
    }

    @Override
    public synchronized void deleteKitByID(String kitID) {
        data.remove(kitID);
        try {
            saveToFile();
        } catch (IOException e) {
            throw new KitStorageException(DELETE, kitID, e);
        }
    }

    private void saveToFile() throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        Path destination = storageFile.toPath().toAbsolutePath();
        Path temporary = Files.createTempFile(destination.getParent(), ".ppk-storage-", ".tmp");
        try {
            try (var writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) { yaml.dump(data, writer); }
            try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override
    public synchronized Set<String> getAllKitIDs() {
        return new HashSet<>(data.keySet());
    }
}