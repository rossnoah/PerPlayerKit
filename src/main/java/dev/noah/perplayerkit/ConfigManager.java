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
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigManager {
    private final File configFile;
    private final FileConfiguration config;
    private final Plugin plugin;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void loadConfig() {
        if (configFile.exists()) {
            mergeMissingKeys();
        }else{
            plugin.saveDefaultConfig();
        }
        plugin.saveConfig();
    }

    private void mergeMissingKeys() {
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            return;
        }

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultConfigStream));

        boolean updated = false;

        //loop through keys and add missing ones
        for (String key : defaultConfig.getKeys(true)) {
            //special handling for public kits
            if (key.equals("publickits")) {
                // add publickits if its missing
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.getConfigurationSection(key).getValues(true));
                    plugin.getLogger().info("Added missing section: publickits");
                    updated = true;
                }
                continue;
            }else if(key.startsWith("publickits")){
                continue;
            }

            // Add missing keys for everything else
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
                plugin.getLogger().info("Added missing config key: " + key);
                updated = true;
            }
        }

        //save the updated config
        if (updated) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Configuration updated with missing keys.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save updated configuration: " + e.getMessage());
            }
        }
    }


}
