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

import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.DisabledCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.inventory.ItemStack;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

public class EnderchestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {

            if (DisabledCommand.isBlockedInWorld(player)) {
                return true;
            }
            viewOnlyEC(player);
            return true;
        }

        sender.sendMessage("Only players can use this command");
        if (sender instanceof Player s) SoundManager.playFailure(s);
        return true;
    }

    public void viewOnlyEC(Player p) {

        ItemStack fill = ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE,1,"");

        Menu menu = ChestMenu.builder(5).title(ChatColor.BLUE + "View Only Enderchest").build();


        for (int i = 0; i < 9; i++) {
            menu.getSlot(i).setItem(fill);
        }
        for (int i = 36; i < 45; i++) {
            menu.getSlot(i).setItem(fill);
        }
//        set the items in the inventory to the items in the enderchest
        ItemStack[] items = p.getEnderChest().getContents();
        for (int i = 0; i < 27; i++) {
            menu.getSlot(i + 9).setItem(items[i]);
        }
        menu.open(p);
        SoundManager.playOpenGui(p);
    }
}


