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
import dev.noah.perplayerkit.gui2.data.DataContext;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Command for switching GUI themes
 */
public class ThemeSwitchCommand implements CommandExecutor, TabCompleter {
    
    private static final List<String> AVAILABLE_THEMES = Arrays.asList(
        "default", "dark", "neon", "medieval"
    );
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // If no arguments, open theme selector GUI
        if (args.length == 0) {
            try {
                DataContext context = new DataContext();
                context.set("player.name", player.getName());
                context.set("player.current_theme", getCurrentTheme(player));
                
                PerPlayerKit.guiManager.openGui(player, "theme-selector", context);
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Failed to open theme selector: " + e.getMessage());
            }
            return true;
        }
        
        // Switch to specified theme
        String themeName = args[0].toLowerCase();
        
        if (!AVAILABLE_THEMES.contains(themeName)) {
            player.sendMessage(ChatColor.RED + "Unknown theme: " + themeName);
            player.sendMessage(ChatColor.GRAY + "Available themes: " + String.join(", ", AVAILABLE_THEMES));
            return true;
        }
        
        // Set the theme for the player
        setPlayerTheme(player, themeName);
        
        // Open the appropriate main menu for the theme
        String guiName = getMainMenuForTheme(themeName);
        
        try {
            DataContext context = new DataContext();
            context.set("player.name", player.getName());
            context.set("player.current_theme", themeName);
            
            PerPlayerKit.guiManager.openGui(player, guiName, context);
            
            player.sendMessage(ChatColor.GREEN + "Theme switched to: " + 
                ChatColor.BOLD + capitalize(themeName));
                
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to switch theme: " + e.getMessage());
        }
        
        return true;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return AVAILABLE_THEMES.stream()
                .filter(theme -> theme.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return null;
    }
    
    private String getCurrentTheme(Player player) {
        // For now, use a simple metadata approach
        // In a full implementation, this could be stored in database or config
        if (player.hasMetadata("ppk_theme")) {
            return player.getMetadata("ppk_theme").get(0).asString();
        }
        return "default";
    }
    
    private void setPlayerTheme(Player player, String theme) {
        // Store theme preference using metadata
        // In a full implementation, this could be stored in database
        player.setMetadata("ppk_theme", new org.bukkit.metadata.FixedMetadataValue(PerPlayerKit.getPlugin(), theme));
    }
    
    private String getMainMenuForTheme(String theme) {
        switch (theme.toLowerCase()) {
            case "dark":
                return "main-menu-dark";
            case "neon":
                return "main-menu-neon";
            case "medieval":
            case "default":
            default:
                return "main-menu";
        }
    }
    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}