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

import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noah.perplayerkit.util.SoundManager;

import java.util.List;

public class SavePublicKitCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //if not player
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(p)) {
            return true;
        }

        //if not enough arguments

        if (args.length < 1) {
            p.sendMessage(ChatColor.RED + "You need to specify a kit id");
            p.sendMessage(ChatColor.RED + "Usage: /" + label + " <kitid>");
            return true;
        }

        String kidId = args[0];

        if (KitManager.get().getPublicKitList().stream().noneMatch(kit -> kit.id.equals(kidId))) {
            p.sendMessage(ChatColor.RED + "Public kit " + kidId + " does not exist");
            p.sendMessage(ChatColor.RED + "You may need to add a public kit in the config");
            return true;
        }

        Inventory inv = p.getInventory();

        ItemStack[] data = new ItemStack[41];
//        copy inventory into data
        for (int i = 0; i < 41; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                data[i] = item.clone();
            }
        }


        data = ItemFilter.get().filterItemStack(data);

        KitManager kitManager = KitManager.get();
        //save kit
        boolean success = kitManager.savePublicKit(kidId, data);
        if (success) {
            kitManager.savePublicKitToDB(kidId);
            p.sendMessage("Saved kit " + kidId);
            SoundManager.playSuccess(p);
        } else {
            p.sendMessage("Error saving kit " + kidId);
            SoundManager.playFailure(p);
        }

        return true;


    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return KitManager.get().getPublicKitList().stream().map(kit -> kit.id).toList();
    }
}
