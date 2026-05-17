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
package dev.noah.perplayerkit.commands.admin;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.util.Lang;
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
        Player player = CommandGuards.requirePlayerInEnabledWorld(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 1) {
            Lang.get().send(player, "error.missing-kit-id");
            Lang.get().send(player, "command.savepublickit-usage", "command", label);
            return true;
        }

        String kitId = args[0];

        if (KitManager.get().getPublicKitList().stream().noneMatch(kit -> kit.id.equals(kitId))) {
            Lang.get().send(player, "error.public-kit-not-found", "kitid", kitId);
            Lang.get().send(player, "error.add-public-kit-config");
            return true;
        }

        Inventory inv = player.getInventory();

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
        boolean success = kitManager.savePublicKit(kitId, data);
        if (success) {
            kitManager.savePublicKitToDB(kitId);
            Lang.get().send(player, "success.public-kit-saved-admin", "kitid", kitId);
            SoundManager.playSuccess(player);
        } else {
            Lang.get().send(player, "error.public-kit-save-failed", "kitid", kitId);
            SoundManager.playFailure(player);
        }

        return true;


    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> ids = KitManager.get().getPublicKitList().stream().map(kit -> kit.id).toList();
            return ids.isEmpty() ? null : ids;
        }

        return null;
    }
}
