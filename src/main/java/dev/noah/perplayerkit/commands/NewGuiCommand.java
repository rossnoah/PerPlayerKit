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
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command to access the new advanced GUI system
 */
public class NewGuiCommand implements CommandExecutor {
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by players!");
            return true;
        }
        
        Player player = (Player) sender;
        
        // Default to main menu if no GUI specified
        String guiName = "main-menu";
        
        if (args.length > 0) {
            guiName = args[0];
        }
        
        // Check if GUI exists
        if (!PerPlayerKit.guiManager.hasGui(guiName)) {
            player.sendMessage(ChatColor.RED + "GUI '" + guiName + "' not found!");
            player.sendMessage(ChatColor.GRAY + "Available GUIs: " + String.join(", ", PerPlayerKit.guiManager.getLoadedGuiNames()));
            return true;
        }
        
        // Open the GUI
        try {
            PerPlayerKit.guiManager.openGui(player, guiName);
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Failed to open GUI: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
}