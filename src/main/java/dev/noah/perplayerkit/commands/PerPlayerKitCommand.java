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

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PerPlayerKitCommand implements CommandExecutor, TabCompleter {

    private Plugin plugin;

    public PerPlayerKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "PerPlayerKit Commands:");
            sender.sendMessage(
                    ChatColor.YELLOW + "/" + label + " about" + ChatColor.GRAY + " - Show plugin information");
            if (sender.hasPermission("perplayerkit.admin")) {
                sender.sendMessage(
                        ChatColor.YELLOW + "/" + label + " reload" + ChatColor.GRAY + " - Reload plugin configuration");
                sender.sendMessage(ChatColor.YELLOW + "/" + label + " import <type>" + ChatColor.GRAY
                        + " - Import data from other plugins");
            }
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "about":
                sender.sendMessage(
                        ChatColor.GREEN + "PerPlayerKit is a plugin that allows players to have their own kits.");
                sender.sendMessage(ChatColor.GRAY + "Version: " + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Author: " + plugin.getDescription().getAuthors().get(0));
                return true;
            case "reload":
                if (!sender.hasPermission("perplayerkit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                sender.sendMessage(ChatColor.YELLOW + "Reloading PerPlayerKit...");
                sender.sendMessage(ChatColor.GRAY + "This may take a moment...");

                try {
                    long startTime = System.currentTimeMillis();
                    ((PerPlayerKit) plugin).reloadPlugin();
                    long endTime = System.currentTimeMillis();

                    sender.sendMessage(ChatColor.GREEN + "✓ PerPlayerKit reloaded successfully!");
                    sender.sendMessage(ChatColor.GRAY + "Reload completed in " + (endTime - startTime) + "ms");
                    sender.sendMessage(
                            ChatColor.GRAY + "All configuration values and conditional features have been updated.");
                } catch (Exception e) {
                    sender.sendMessage(ChatColor.RED + "✗ Error occurred while reloading: " + e.getMessage());
                    plugin.getLogger().warning("Error during reload: " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            case "import":
                if (!sender.hasPermission("perplayerkit.admin")) {
                    sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Missing import type! Available: kitsx");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "kitsx":
                        sender.sendMessage(ChatColor.GREEN + "Starting import...");
                        KitsXImporter importer = new KitsXImporter(plugin, sender);
                        if (!importer.checkForFiles()) {
                            sender.sendMessage(ChatColor.RED + "Missing files to import");
                            sender.sendMessage(
                                    ChatColor.RED + "Copy data folder from KitsX into the PerPlayerKit folder");
                        }
                        importer.importFiles();
                        sender.sendMessage(ChatColor.GREEN + "Attempted import of KitsX data!");
                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Invalid import type! Available: kitsx");
                        break;
                }
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand! Use /" + label + " for help.");
                return true;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {

        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("about");

            if (sender.hasPermission("perplayerkit.admin")) {
                completions.add("reload");
                completions.add("import");
            }

            return completions.stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("import") && sender.hasPermission("perplayerkit.admin")) {
            return List.of("kitsx").stream()
                    .filter(completion -> completion.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return null;
    }
}
