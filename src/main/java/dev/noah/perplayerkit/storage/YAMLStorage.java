package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.PerPlayerKit;
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

    public YAMLStorage(String filePath) {
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
            PerPlayerKit.getPlugin().getLogger().info("YAML storage initialized.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        try {
            saveToFile();
            PerPlayerKit.getPlugin().getLogger().info("YAML storage closed and saved.");
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