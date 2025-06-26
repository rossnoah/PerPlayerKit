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
import dev.noah.perplayerkit.ConfigManager;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class DisabledCommand {

    /**
     * Checks if the player is in a world where commands are disabled.
     * 
     * @param player The player to check
     * @return true if commands are disabled in the player's world, false otherwise
     */
    public static boolean isBlockedInWorld(Player player) {
        World world = player.getWorld();
        return ConfigManager.get().getDisabledCommandWorlds().contains(world.getName());
    }

    /**
     * Sends the disabled command message to the player if they are in a blocked
     * world.
     * 
     * @param player The player to send the message to
     * @return true if the player was in a blocked world and the message was sent,
     *         false otherwise
     */
    public static boolean sendDisabledMessage(Player player) {
        if (isBlockedInWorld(player)) {
            player.sendMessage(
                    ChatColor.translateAlternateColorCodes('&', ConfigManager.get().getDisabledCommandMessage()));
            return true;
        }
        return false;
    }
}
