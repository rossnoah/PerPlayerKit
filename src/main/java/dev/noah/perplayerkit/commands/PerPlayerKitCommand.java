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
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.storage.StorageMigrator;
import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PerPlayerKitCommand implements CommandExecutor, TabCompleter {

    private static final List<String> STORAGE_TYPES = Arrays.asList("sqlite", "mysql", "redis", "yml");

    private Plugin plugin;
    public PerPlayerKitCommand(Plugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Missing arguments!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "about":
                sender.sendMessage(ChatColor.GREEN + "PerPlayerKit is a plugin that allows players to have their own kits.");
                return true;
            case "import":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Missing import type!");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "kitsx":
                        sender.sendMessage(ChatColor.GREEN + "Starting import...");
                        KitsXImporter importer = new KitsXImporter(plugin,sender);
                        if(!importer.checkForFiles()){
                            sender.sendMessage(ChatColor.RED+"Missing files to import");
                            sender.sendMessage(ChatColor.RED+"Copy data folder from KitsX into the PerPlayerKit folder");
                        }
                        importer.importFiles();
                        sender.sendMessage(ChatColor.GREEN + "Attempted import of KitsX data!");

                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Invalid import type!");
                        break;
                }
                return true;
            case "migrate":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /perplayerkit migrate <source> <destination>");
                    sender.sendMessage(ChatColor.GRAY + "Available storage types: sqlite, mysql, redis, yml");
                    return true;
                }

                String sourceType = args[1].toLowerCase();
                String destType = args[2].toLowerCase();

                if (!STORAGE_TYPES.contains(sourceType)) {
                    sender.sendMessage(ChatColor.RED + "Invalid source storage type: " + sourceType);
                    sender.sendMessage(ChatColor.GRAY + "Available types: sqlite, mysql, redis, yml");
                    return true;
                }

                if (!STORAGE_TYPES.contains(destType)) {
                    sender.sendMessage(ChatColor.RED + "Invalid destination storage type: " + destType);
                    sender.sendMessage(ChatColor.GRAY + "Available types: sqlite, mysql, redis, yml");
                    return true;
                }

                if (sourceType.equals(destType)) {
                    sender.sendMessage(ChatColor.RED + "Source and destination cannot be the same!");
                    return true;
                }

                sender.sendMessage(ChatColor.YELLOW + "Starting migration from " + sourceType + " to " + destType + "...");
                sender.sendMessage(ChatColor.GRAY + "This may take a while for large datasets. Check console for progress.");

                // Run migration asynchronously to avoid blocking the main thread
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    StorageMigrator migrator = new StorageMigrator(plugin);
                    StorageMigrator.MigrationResult result = migrator.migrate(sourceType, destType,
                            message -> Bukkit.getScheduler().runTask(plugin, () -> sender.sendMessage(ChatColor.GRAY + message)));

                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (result.isSuccess()) {
                            sender.sendMessage(ChatColor.GREEN + "Migration completed successfully!");
                            sender.sendMessage(ChatColor.GREEN + "Migrated: " + result.getMigratedCount() + " entries");
                            if (result.getFailedCount() > 0) {
                                sender.sendMessage(ChatColor.YELLOW + "Failed: " + result.getFailedCount() + " entries");
                            }
                            sender.sendMessage(ChatColor.YELLOW + "Remember to update your config.yml storage.type to '" + destType + "' and restart the server.");
                        } else {
                            sender.sendMessage(ChatColor.RED + "Migration failed: " + result.getErrorMessage());
                        }
                    });
                });
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand!");
                return true;

        }
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(args.length == 1) {
            return List.of("about", "import", "migrate");
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return List.of("kitsx");
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return STORAGE_TYPES.stream()
                    .filter(type -> type.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if(args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            String sourceType = args[1].toLowerCase();
            return STORAGE_TYPES.stream()
                    .filter(type -> !type.equals(sourceType))
                    .filter(type -> type.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return null;
    }
}
