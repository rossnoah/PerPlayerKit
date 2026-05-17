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
package dev.noah.perplayerkit;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Migrates legacy config.yml (config-version 1) to the new layout (config-version 2).
 *
 * v1 stored translatable strings (prefix, motd.message, scheduled-broadcast.messages,
 * messages.*.message, disabled-command-message) directly in config.yml. v2 moves them
 * to lang files. If the user customized any of those values, this migrator writes them
 * into plugins/PerPlayerKit/lang/en.yml so their changes survive the upgrade.
 *
 * Runs before Lang is initialized.
 */
public class ConfigMigrator {

    public static final int CURRENT_VERSION = 2;

    private final Plugin plugin;

    public ConfigMigrator(Plugin plugin) {
        this.plugin = plugin;
    }

    public void migrate() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            return;
        }

        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(configFile);
        int version = userConfig.getInt("config-version", 1);
        if (version >= CURRENT_VERSION) {
            return;
        }

        plugin.getLogger().info("Migrating config from v" + version + " to v" + CURRENT_VERSION);

        Map<String, Object> savedCustomizations = extractCustomizedStrings(userConfig);
        if (!savedCustomizations.isEmpty()) {
            writeCustomizationsToLangFile(savedCustomizations);
            plugin.getLogger().info("Preserved " + savedCustomizations.size()
                    + " customized string(s) from config.yml into lang/en.yml");
        }

        userConfig.set("prefix", null);
        userConfig.set("disabled-command-message", null);
        if (userConfig.isConfigurationSection("motd")) {
            userConfig.set("motd.message", null);
        }
        if (userConfig.isConfigurationSection("scheduled-broadcast")) {
            userConfig.set("scheduled-broadcast.messages", null);
        }
        if (userConfig.isConfigurationSection("messages")) {
            for (String key : userConfig.getConfigurationSection("messages").getKeys(false)) {
                if (userConfig.isConfigurationSection("messages." + key)) {
                    userConfig.set("messages." + key + ".message", null);
                }
            }
        }

        userConfig.set("config-version", CURRENT_VERSION);
        if (!userConfig.contains("language")) {
            userConfig.set("language", "en");
        }

        try {
            userConfig.save(configFile);
            plugin.getLogger().info("config.yml migrated to v" + CURRENT_VERSION);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save migrated config.yml: " + e.getMessage());
        }
    }

    private Map<String, Object> extractCustomizedStrings(FileConfiguration userConfig) {
        FileConfiguration defaults = loadBundledDefaults();
        Map<String, Object> diffs = new LinkedHashMap<>();

        copyIfCustomized(userConfig, defaults, "prefix", diffs);

        if (userConfig.contains("motd.message")) {
            List<String> current = userConfig.getStringList("motd.message");
            List<String> defaultsList = bundledMotdDefault();
            if (!current.equals(defaultsList) && !current.isEmpty()) {
                diffs.put("motd.message", new ArrayList<>(current));
            }
        }

        if (userConfig.contains("scheduled-broadcast.messages")) {
            List<String> current = userConfig.getStringList("scheduled-broadcast.messages");
            List<String> defaultsList = bundledBroadcastDefault();
            if (!current.equals(defaultsList) && !current.isEmpty()) {
                diffs.put("scheduled-broadcast.messages", new ArrayList<>(current));
            }
        }

        if (userConfig.contains("disabled-command-message")) {
            String current = userConfig.getString("disabled-command-message");
            String defaultValue = "<red>Kits are disabled here!</red>";
            if (current != null && !current.equals(defaultValue)) {
                diffs.put("error.disabled-in-world", current);
            }
        }

        Map<String, String> broadcastKeyMap = Map.of(
                "messages.player-repaired.message", "broadcast-messages.player-repaired",
                "messages.player-healed.message", "broadcast-messages.player-healed",
                "messages.player-opened-kit-room.message", "broadcast-messages.player-opened-kit-room",
                "messages.player-loaded-private-kit.message", "broadcast-messages.player-loaded-private-kit",
                "messages.player-loaded-public-kit.message", "broadcast-messages.player-loaded-public-kit",
                "messages.player-loaded-enderchest.message", "broadcast-messages.player-loaded-enderchest",
                "messages.player-copied-kit.message", "broadcast-messages.player-copied-kit",
                "messages.player-copied-ec.message", "broadcast-messages.player-copied-ec",
                "messages.player-regeared.message", "broadcast-messages.player-regeared"
        );

        for (Map.Entry<String, String> entry : broadcastKeyMap.entrySet()) {
            String oldPath = entry.getKey();
            String newKey = entry.getValue();
            if (!userConfig.contains(oldPath)) {
                continue;
            }
            String current = userConfig.getString(oldPath);
            String defaultValue = defaults.getString(oldPath);
            if (current != null && !current.equals(defaultValue)) {
                diffs.put(newKey, current);
            }
        }

        return diffs;
    }

    private void copyIfCustomized(FileConfiguration user, FileConfiguration defaults, String path,
                                  Map<String, Object> out) {
        Object current = user.get(path);
        Object defaultValue = defaults.get(path);
        if (current == null) {
            return;
        }
        if (defaultValue != null && defaultValue.equals(current)) {
            return;
        }
        out.put(path, current);
    }

    private void writeCustomizationsToLangFile(Map<String, Object> customizations) {
        File langDir = new File(plugin.getDataFolder(), "lang");
        if (!langDir.exists() && !langDir.mkdirs()) {
            plugin.getLogger().warning("Failed to create lang directory");
            return;
        }
        File langFile = new File(langDir, "en.yml");

        if (!langFile.exists()) {
            try (java.io.InputStream in = plugin.getResource("lang/en.yml")) {
                if (in != null) {
                    Files.copy(in, langFile.toPath());
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to copy bundled lang/en.yml: " + e.getMessage());
                return;
            }
        }

        YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
        for (Map.Entry<String, Object> e : customizations.entrySet()) {
            langConfig.set(e.getKey(), e.getValue());
        }
        try {
            langConfig.save(langFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to save customizations to lang/en.yml: " + ex.getMessage());
        }
    }

    private FileConfiguration loadBundledDefaults() {
        try (java.io.InputStream in = plugin.getResource("lang/en.yml")) {
            if (in == null) {
                return new YamlConfiguration();
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return new YamlConfiguration();
        }
    }

    private List<String> bundledMotdDefault() {
        return loadBundledDefaults().getStringList("motd.message");
    }

    private List<String> bundledBroadcastDefault() {
        return loadBundledDefaults().getStringList("scheduled-broadcast.messages");
    }
}
