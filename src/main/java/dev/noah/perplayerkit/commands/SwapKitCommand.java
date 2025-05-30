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

import com.google.common.primitives.Ints;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.ChatColor;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only Players can use this!");
            return true;
        }

        if (args.length != 2) {
            player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
            SoundManager.playFailure(player);
            return true;
        }

        Integer slot1 = Ints.tryParse(args[0]);
        Integer slot2 = Ints.tryParse(args[1]);

        if (slot1 == null || slot2 == null) {
            player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
            player.sendMessage(ChatColor.RED + "Select real numbers");
            SoundManager.playFailure(player);
            return true;
        }

        KitManager kitManager = KitManager.get();
        UUID uuid = player.getUniqueId();

        if (!kitManager.hasKit(uuid, slot1)) {
            player.sendMessage(ChatColor.RED + "Kit " + slot1 + " doesn't exist!");
            SoundManager.playFailure(player);
            return true;
        }

        if (!kitManager.hasKit(uuid, slot2)) {
            player.sendMessage(ChatColor.RED + "Kit " + slot2 + " doesn't exist!");
            SoundManager.playFailure(player);
            return true;
        }

        ItemStack[] tempkit = kitManager.getPlayerKit(uuid, slot1).clone();
        kitManager.savekit(uuid, slot1, kitManager.getPlayerKit(uuid, slot2), true);
        kitManager.savekit(uuid, slot2, tempkit.clone(), true);
        kitManager.saveEnderchestToDB(uuid, slot1);
        kitManager.saveEnderchestToDB(uuid, slot2);

        player.sendMessage(ChatColor.GREEN + "Kits " + slot1 + " and " + slot2 + " have been swapped!");
        SoundManager.playSuccess(player);
        return true;
    }
}
