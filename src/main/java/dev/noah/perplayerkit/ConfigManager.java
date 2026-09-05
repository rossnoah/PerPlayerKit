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
package dev.noah.perplayerkit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class ConfigManager {
    private final Plugin plugin;
    public ConfigManager(Plugin plugin) { this.plugin = plugin; }

    public boolean loadConfig() {
        Path file = plugin.getDataFolder().toPath().resolve("config.yml");
        try {
            if (!Files.exists(file)) plugin.saveDefaultConfig();
            YamlConfiguration config = ConfigFiles.read(file);
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) throw new java.io.IOException("Bundled config.yml is missing");
                YamlConfiguration defaults = new YamlConfiguration();
                defaults.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                boolean changed = false;
                for (String key : defaults.getKeys(true)) {
                    if (key.startsWith("publickits.") || defaults.isConfigurationSection(key)) continue;
                    if (!config.contains(key)) { config.set(key, defaults.get(key)); changed = true; }
                }
                if (!config.contains("publickits")) {
                    config.set("publickits", defaults.get("publickits")); changed = true;
                }
                if (changed) ConfigFiles.write(config, file);
            }
            plugin.reloadConfig();
            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Unable to load config.yml: " + e.getMessage());
            return false;
        }
    }
}
