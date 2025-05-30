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
package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DisabledCommand {




    private static boolean isBlockedInWorld(World world) {
        return PerPlayerKit.getPlugin().getConfig().getStringList("disabled-command-worlds").contains(world.getName());
    }


    public static boolean isBlockedInWorld(Player player) {
        if (isBlockedInWorld(player.getWorld())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', PerPlayerKit.getPlugin().getConfig().getString("disabled-command-message")));
            SoundManager.playFailure(player);
            return true;
        }
        return false;
    }
}
