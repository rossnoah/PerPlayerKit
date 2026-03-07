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
package dev.noah.perplayerkit.commands.kits;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.commands.core.SlotArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.noah.perplayerkit.util.SoundManager;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DeleteKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = CommandGuards.requirePlayer(sender, ChatColor.RED + "Only Players can use this!");
        if (player == null) {
            return true;
        }

        UUID uuid = player.getUniqueId();
        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Usage: /deletekit <slot>");
            SoundManager.playFailure(player);
            return true;
        }

        Integer slot = SlotArgumentParser.parseSlot(args[0]);
        KitManager kitManager = KitManager.get();
        if (slot == null) {
            player.sendMessage(ChatColor.RED + "Usage: /deletekit <slot>");
            player.sendMessage(ChatColor.RED + "Select a real number");
            SoundManager.playFailure(player);
            return true;
        }

        if (!kitManager.hasKit(uuid, slot)) {
            player.sendMessage(ChatColor.RED + "Kit " + slot + " doesnt exist!");
            SoundManager.playFailure(player);
            return true;
        }

        if (kitManager.deleteKit(uuid, slot)) {
            player.sendMessage(ChatColor.GREEN + "Kit " + slot + " deleted!");
            SoundManager.playSuccess(player);
        } else {
            player.sendMessage(ChatColor.RED + "Kit deletion failed!");
            SoundManager.playFailure(player);
        }

        return true;
    }
}
