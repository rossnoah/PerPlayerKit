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
package dev.noah.perplayerkit.commands.share;

import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.commands.core.CommandGuards;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class AbstractShareResponseCommand implements CommandExecutor, TabCompleter {

    private final boolean accept;

    protected AbstractShareResponseCommand(boolean accept) {
        this.accept = accept;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // Accepting can apply items to the player's inventory, so it honors the disabled-world guard.
        Player player = accept ? CommandGuards.requirePlayerInEnabledWorld(sender) : CommandGuards.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        String id = args.length > 0 ? args[0] : null;
        if (accept) {
            KitShareManager.get().acceptRequest(player, id);
        } else {
            KitShareManager.get().declineRequest(player, id);
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        return KitShareManager.get().getPendingRequestIds(player).stream()
                .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }
}
