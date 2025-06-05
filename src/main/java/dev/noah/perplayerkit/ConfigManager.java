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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class ConfigManager {
    private final File configFile;
    private final Plugin plugin;
    private static ConfigManager instance;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        instance = this;
    }

    public static ConfigManager get() {
        if (instance == null) {
            throw new IllegalStateException("ConfigManager has not been initialized yet!");
        }
        return instance;
    }

    public void loadConfig() {
        if (configFile.exists()) {
            mergeMissingKeys();
        } else {
            plugin.saveDefaultConfig();
        }
        plugin.saveConfig();
    }

    private void mergeMissingKeys() {
        InputStream defaultConfigStream = plugin.getResource("config.yml");
        if (defaultConfigStream == null) {
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        FileConfiguration defaultConfig = YamlConfiguration
                .loadConfiguration(new InputStreamReader(defaultConfigStream));

        boolean updated = false;

        // loop through keys and add missing ones
        for (String key : defaultConfig.getKeys(true)) {
            // special handling for public kits
            if (key.equals("publickits")) {
                // add publickits if its missing
                if (!config.contains(key)) {
                    config.set(key, defaultConfig.getConfigurationSection(key).getValues(true));
                    plugin.getLogger().info("Added missing section: publickits");
                    updated = true;
                }
                continue;
            } else if (key.startsWith("publickits")) {
                continue;
            }

            // Add missing keys for everything else
            if (!config.contains(key)) {
                config.set(key, defaultConfig.get(key));
                plugin.getLogger().info("Added missing config key: " + key);
                updated = true;
            }
        }

        // save the updated config
        if (updated) {
            try {
                config.save(configFile);
                plugin.getLogger().info("Configuration updated with missing keys.");
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to save updated configuration: " + e.getMessage());
            }
        }
    }

    // Storage configuration
    public String getStorageType() {
        return plugin.getConfig().getString("storage.type");
    }

    // MySQL configuration
    public String getMySQLHost() {
        return plugin.getConfig().getString("mysql.host");
    }

    public String getMySQLPort() {
        return plugin.getConfig().getString("mysql.port");
    }

    public String getMySQLDatabase() {
        return plugin.getConfig().getString("mysql.dbname");
    }

    public String getMySQLUsername() {
        return plugin.getConfig().getString("mysql.username");
    }

    public String getMySQLPassword() {
        return plugin.getConfig().getString("mysql.password");
    }

    public boolean getMySQLUseSSL() {
        return plugin.getConfig().getBoolean("mysql.useSSL", false);
    }

    public int getMySQLMaximumPoolSize() {
        return plugin.getConfig().getInt("mysql.maximumPoolSize", 10);
    }

    // Redis configuration
    public String getRedisHost() {
        return plugin.getConfig().getString("redis.host");
    }

    public int getRedisPort() {
        int port = plugin.getConfig().getInt("redis.port");
        return port == 0 ? Integer.parseInt(plugin.getConfig().getString("redis.port", "6379")) : port;
    }

    public String getRedisPassword() {
        return plugin.getConfig().getString("redis.password");
    }

    // Anti-exploit configuration
    public boolean isOnlyAllowKitroomItems() {
        return plugin.getConfig().getBoolean("anti-exploit.only-allow-kitroom-items", false);
    }

    public boolean isImportFilterEnabled() {
        return plugin.getConfig().getBoolean("anti-exploit.import-filter", false);
    }

    public boolean isBlockSpacesInCommandsEnabled() {
        return plugin.getConfig().getBoolean("anti-exploit.block-spaces-in-commands", false);
    }

    public boolean isPreventShulkersDroppingItemsEnabled() {
        return plugin.getConfig().getBoolean("anti-exploit.prevent-shulkers-dropping-items", false);
    }

    // Feature configuration
    public boolean isRekitOnRespawnEnabled() {
        return plugin.getConfig().getBoolean("feature.rekit-on-respawn", true);
    }

    public boolean isRekitOnKillEnabled() {
        return plugin.getConfig().getBoolean("feature.rekit-on-kill", false);
    }

    public boolean isOldDeathDropsEnabled() {
        return plugin.getConfig().getBoolean("feature.old-death-drops", false);
    }

    public boolean isSendUpdateMessageOnJoinEnabled() {
        return plugin.getConfig().getBoolean("feature.send-update-message-on-join", true);
    }

    public boolean isBroadcastOnPlayerActionEnabled() {
        return plugin.getConfig().getBoolean("feature.broadcast-on-player-action", true);
    }

    public boolean isHealOnEnderchestLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.heal-on-enderchest-load", false);
    }

    public boolean isFeedOnEnderchestLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.feed-on-enderchest-load", false);
    }

    public boolean isSetSaturationOnEnderchestLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.set-saturation-on-enderchest-load", false);
    }

    public boolean isRemovePotionEffectsOnEnderchestLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.remove-potion-effects-on-enderchest-load", false);
    }

    public boolean isSetHealthOnKitLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.set-health-on-kit-load", false);
    }

    public boolean isSetHungerOnKitLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.set-hunger-on-kit-load", false);
    }

    public boolean isSetSaturationOnKitLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.set-saturation-on-kit-load", false);
    }

    public boolean isRemovePotionEffectsOnKitLoadEnabled() {
        return plugin.getConfig().getBoolean("feature.remove-potion-effects-on-kit-load", false);
    }

    // Regear configuration
    public int getRegearCommandCooldown() {
        return plugin.getConfig().getInt("regear.command-cooldown", 5);
    }

    public int getRegearDamageTimer() {
        return plugin.getConfig().getInt("regear.damage-timer", 5);
    }

    public boolean isRegearAllowWhileUsingElytra() {
        return plugin.getConfig().getBoolean("regear.allow-while-using-elytra", true);
    }

    public boolean isRegearPreventPuttingItemsInInventory() {
        return plugin.getConfig().getBoolean("regear.prevent-putting-items-in-regear-inventory", false);
    }

    public String getRegearMode() {
        return plugin.getConfig().getString("regear.mode", "command");
    }

    public boolean isRegearInvertWhitelist() {
        return plugin.getConfig().getBoolean("regear.invert-whitelist", false);
    }

    public List<String> getRegearWhitelist() {
        return plugin.getConfig().getStringList("regear.whitelist");
    }

    // MOTD configuration
    public boolean isMOTDEnabled() {
        return plugin.getConfig().getBoolean("motd.enabled");
    }

    public List<String> getMOTDMessage() {
        return plugin.getConfig().getStringList("motd.message");
    }

    public long getMOTDDelay() {
        return plugin.getConfig().getLong("motd.delay");
    }

    // Broadcast configuration
    public String getPrefix() {
        return plugin.getConfig().getString("prefix", "<gray>[<aqua>Kits</aqua>]</gray> ");
    }

    public String getMessage(String key, String defaultValue) {
        return plugin.getConfig().getString(key, defaultValue);
    }

    public boolean isScheduledBroadcastEnabled() {
        return plugin.getConfig().getBoolean("scheduled-broadcast.enabled");
    }

    public List<String> getScheduledBroadcastMessages() {
        return plugin.getConfig().getStringList("scheduled-broadcast.messages");
    }

    public int getScheduledBroadcastPeriod() {
        return plugin.getConfig().getInt("scheduled-broadcast.period");
    }

    // Kit room configuration
    public String getKitRoomItemMaterial(int slot) {
        return plugin.getConfig().getString("kitroom.items." + slot + ".material");
    }

    public String getKitRoomItemName(int slot) {
        return plugin.getConfig().getString("kitroom.items." + slot + ".name");
    }

    // Public kits configuration
    public ConfigurationSection getPublicKitsSection() {
        return plugin.getConfig().getConfigurationSection("publickits");
    }

    public String getPublicKitName(String key) {
        return plugin.getConfig().getString("publickits." + key + ".name");
    }

    public String getPublicKitIcon(String key) {
        return plugin.getConfig().getString("publickits." + key + ".icon");
    }

    // Sound configuration
    public String getSound(String path, String defaultName) {
        return plugin.getConfig().getString(path, defaultName);
    }

    // Disabled command configuration
    public List<String> getDisabledCommandWorlds() {
        return plugin.getConfig().getStringList("disabled-command-worlds");
    }

    public String getDisabledCommandMessage() {
        return plugin.getConfig().getString("disabled-command-message");
    }
}
