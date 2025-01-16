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
package dev.noah.perplayerkit.util.importutil;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KitsXImporter {

    private final String kitroomFilePath = "data/kitroom.yml";
    private final String kitsFilePath = "data/kits.yml";
    private final String enderchestsFilePath = "data/enderchest.yml";

    private final Plugin plugin;
    private final CommandSender sender;

    public KitsXImporter(Plugin plugin, CommandSender sender) {
        this.plugin = plugin;
        this.sender = sender;
    }

    public boolean checkForFiles() {
        // Check to make sure the files we need exist
        File kitroom = new File(plugin.getDataFolder(), kitroomFilePath);
        File kits = new File(plugin.getDataFolder(), kitsFilePath);
        File enderchests = new File(plugin.getDataFolder(), enderchestsFilePath);

        return kitroom.exists() && kits.exists() && enderchests.exists();
    }

    public void importFiles() {
        if (!checkForFiles()) {
            sender.sendMessage(ChatColor.RED + "Required files are missing. Cannot proceed with the import.");
            return;
        }

        importKitroom(sender);
        importKits(sender);
        importEnderchests(sender);
    }



    private void importKitroom(CommandSender sender) {
        // Define the file path for kitroom.yml
        File kitroomFile = new File(plugin.getDataFolder(), kitroomFilePath);

        // Check if the file exists
        if (!kitroomFile.exists()) {
            sender.sendMessage(ChatColor.RED + "kitroom.yml file not found! Skipping import.");
            return;
        }

        // Load the YAML file
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(kitroomFile);

        // Parse the categories, limiting to 5 categories
        if (yamlConfig.contains("categories")) {
            List<String> categoryKeys = new ArrayList<>(yamlConfig.getConfigurationSection("categories").getKeys(false));

            // Only process up to 5 categories
            for (int categoryIndex = 0; categoryIndex < Math.min(5, categoryKeys.size()); categoryIndex++) {
                String category = categoryKeys.get(categoryIndex);

                sender.sendMessage(ChatColor.BLUE + "Processing category: " + category);

                // Create an array to hold up to 45 items in the current category
                ItemStack[] categoryItems = new ItemStack[45];
                int itemIndex = 0;

                for (String key : yamlConfig.getConfigurationSection("categories." + category).getKeys(false)) {
                    if (itemIndex >= 45) {
                        sender.sendMessage(ChatColor.YELLOW + "Ignoring additional items in category: " + category);
                        break;
                    }

                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack itemStack = yamlConfig.getItemStack("categories." + category + "." + key);

                        if (itemStack != null && slot < 45) {
                            categoryItems[slot] = itemStack;
                            itemIndex++;
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.GOLD + "[Warning] Invalid slot number: " + key + " in category: " + category);
                    }
                }

                // Save the data using setKitRoom
                KitRoomDataManager.get().setKitRoom(categoryIndex, categoryItems);
                KitRoomDataManager.get().saveToDBAsync();

                // Log the number of items imported in this category
                sender.sendMessage(ChatColor.GREEN + "Imported " + itemIndex + " items in category: " + category);
            }
        }

        // Log completion
        sender.sendMessage(ChatColor.AQUA + "Finished importing kitroom.yml.");
    }

    private void importKits(CommandSender sender) {
        // Define the file path for kits.yml
        File kitsFile = new File(plugin.getDataFolder(), kitsFilePath);

        // Check if the file exists
        if (!kitsFile.exists()) {
            sender.sendMessage(ChatColor.RED + "kits.yml file not found! Skipping import.");
            return;
        }

        // Load the YAML file
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(kitsFile);

        // Check if there are any player UUIDs defined
        if (yamlConfig.getKeys(false).isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No player kits found in kits.yml.");
            return;
        }

        for (String uuid : yamlConfig.getKeys(false)) {
            sender.sendMessage(ChatColor.BLUE + "Processing kits for player UUID: " + uuid);

            if (!yamlConfig.isConfigurationSection(uuid)) {
                sender.sendMessage(ChatColor.YELLOW + "No kits found for UUID: " + uuid);
                continue;
            }

            for (String kitKey : yamlConfig.getConfigurationSection(uuid).getKeys(false)) {
                int kitNumber;
                try {
                    kitNumber = Integer.parseInt(kitKey.replace("Kit ", "").trim());
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.GOLD + "[Warning] Invalid kit number: " + kitKey + " for player UUID: " + uuid);
                    continue;
                }

                sender.sendMessage(ChatColor.GREEN + "Processing Kit " + kitNumber + " for player UUID: " + uuid);

                if (!yamlConfig.isConfigurationSection(uuid + "." + kitKey)) {
                    sender.sendMessage(ChatColor.YELLOW + "No items found for Kit " + kitNumber + " for player UUID: " + uuid);
                    continue;
                }

                // Adjusted to handle up to 41 slots
                ItemStack[] kitItems = new ItemStack[41];
                for (String slotKey : yamlConfig.getConfigurationSection(uuid + "." + kitKey).getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.GOLD + "[Warning] Invalid slot number: " + slotKey + " in Kit " + kitNumber);
                        continue;
                    }

                    if (slot < 0 || slot >= 41) {
                        sender.sendMessage(ChatColor.YELLOW + "Slot " + slot + " in Kit " + kitNumber + " is out of range (0-40). Skipping.");
                        continue;
                    }

                    ItemStack itemStack = yamlConfig.getItemStack(uuid + "." + kitKey + "." + slotKey);
                    if (itemStack != null) {
                        kitItems[slot] = itemStack; // Store the item in the correct slot
                    }
                }

                // Save or handle the kit data
                KitManager.get().savekit(UUID.fromString(uuid), kitNumber, kitItems);
            }

            KitManager.get().savePlayerKitsToDB(UUID.fromString(uuid));

        }

        // Log completion
        sender.sendMessage(ChatColor.AQUA + "Finished importing kits.yml.");
    }

    private void importEnderchests(CommandSender sender) {
        // Define the file path for kits.yml
        File kitsFile = new File(plugin.getDataFolder(), kitsFilePath);

        // Check if the file exists
        if (!kitsFile.exists()) {
            sender.sendMessage(ChatColor.RED + "enderchest.yml file not found! Skipping import.");
            return;
        }

        // Load the YAML file
        YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(kitsFile);

        // Check if there are any player UUIDs defined
        if (yamlConfig.getKeys(false).isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No player kits found in kits.yml.");
            return;
        }

        for (String uuid : yamlConfig.getKeys(false)) {
            sender.sendMessage(ChatColor.BLUE + "Processing EC for player UUID: " + uuid);

            if (!yamlConfig.isConfigurationSection(uuid)) {
                sender.sendMessage(ChatColor.YELLOW + "No EC found for UUID: " + uuid);
                continue;
            }

            for (String kitKey : yamlConfig.getConfigurationSection(uuid).getKeys(false)) {
                int kitNumber;
                try {
                    kitNumber = Integer.parseInt(kitKey.replace("Kit ", "").trim());
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.GOLD + "[Warning] Invalid EC number: " + kitKey + " for player UUID: " + uuid);
                    continue;
                }

                sender.sendMessage(ChatColor.GREEN + "Processing EC " + kitNumber + " for player UUID: " + uuid);

                if (!yamlConfig.isConfigurationSection(uuid + "." + kitKey)) {
                    sender.sendMessage(ChatColor.YELLOW + "No items found for EC " + kitNumber + " for player UUID: " + uuid);
                    continue;
                }

                // Adjusted to handle up to 27 slots
                ItemStack[] kitItems = new ItemStack[27];
                for (String slotKey : yamlConfig.getConfigurationSection(uuid + "." + kitKey).getKeys(false)) {
                    int slot;
                    try {
                        slot = Integer.parseInt(slotKey);
                    } catch (NumberFormatException e) {
                        sender.sendMessage(ChatColor.GOLD + "[Warning] Invalid slot number: " + slotKey + " in EC " + kitNumber);
                        continue;
                    }

                    if (slot < 0 || slot >= 27) {
                        sender.sendMessage(ChatColor.YELLOW + "Slot " + slot + " in EC " + kitNumber + " is out of range (0-26). Skipping.");
                        continue;
                    }

                    ItemStack itemStack = yamlConfig.getItemStack(uuid + "." + kitKey + "." + slotKey);
                    if (itemStack != null) {
                        kitItems[slot] = itemStack; // Store the item in the correct slot
                    }
                }

                // Save or handle the kit data
                KitManager.get().savekit(UUID.fromString(uuid), kitNumber, kitItems);
            }

            KitManager.get().savePlayerKitsToDB(UUID.fromString(uuid));

        }

        // Log completion
        sender.sendMessage(ChatColor.AQUA + "Finished importing enderchest.yml.");
    }



}
