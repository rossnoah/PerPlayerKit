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
package dev.noah.perplayerkit.commands.core;

import dev.noah.perplayerkit.util.DisabledCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class CommandGuards {
    private static final String DEFAULT_ONLY_PLAYERS_MESSAGE = "Only players can use this command";

    private CommandGuards() {
    }

    public static @Nullable Player requirePlayer(CommandSender sender) {
        return requirePlayer(sender, DEFAULT_ONLY_PLAYERS_MESSAGE);
    }

    public static @Nullable Player requirePlayer(CommandSender sender, String onlyPlayersMessage) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(onlyPlayersMessage);
        return null;
    }

    public static @Nullable Player requirePlayerInEnabledWorld(CommandSender sender) {
        return requirePlayerInEnabledWorld(sender, DEFAULT_ONLY_PLAYERS_MESSAGE);
    }

    public static @Nullable Player requirePlayerInEnabledWorld(CommandSender sender, String onlyPlayersMessage) {
        Player player = requirePlayer(sender, onlyPlayersMessage);
        if (player == null) {
            return null;
        }
        if (DisabledCommand.isBlockedInWorld(player)) {
            return null;
        }
        return player;
    }
}
