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

import dev.noah.perplayerkit.KitRoomDataManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class KitRoomCommand implements CommandExecutor, TabCompleter {
    private static final String LOAD = "load";
    private static final String SAVE = "save";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 1) {
            sendUsageError(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase(LOAD)) {
            KitRoomDataManager.get().loadFromDB();
            sender.sendMessage(ChatColor.GREEN + "Kit Room loaded from SQL");
            if (sender instanceof Player p) SoundManager.playSuccess(p);
            return true;
        }

        if (args[0].equalsIgnoreCase(SAVE)) {
            KitRoomDataManager.get().saveToDBAsync();
            sender.sendMessage(ChatColor.GREEN + "Kit Room saved to SQL");
            if (sender instanceof Player p) SoundManager.playSuccess(p);
            return true;
        }

        sendUsageError(sender);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            list.add(SAVE);
            list.add(LOAD);
            return list;
        }
        return null;
    }

    private void sendUsageError(CommandSender sender) {
        sender.sendMessage(ChatColor.RED + "Incorrect Usage!");
        sender.sendMessage("/kitroom <load/save>");
        if (sender instanceof Player p) {
            SoundManager.playFailure(p);
        }
    }
}
