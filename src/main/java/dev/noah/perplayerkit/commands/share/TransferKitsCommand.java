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
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.PlayerUtil;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TransferKitsCommand implements CommandExecutor, TabCompleter {

    private static final int COOLDOWN_SECONDS = 5;
    private final CooldownManager cooldownManager = new CooldownManager(COOLDOWN_SECONDS);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = CommandGuards.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 1) {
            Lang.get().send(player, "error.missing-transfer-target");
            SoundManager.playFailure(player);
            return true;
        }

        if (cooldownManager.isOnCooldown(player)) {
            Lang.get().send(player, "error.command-cooldown");
            SoundManager.playFailure(player);
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            Lang.get().send(player, "error.share-player-not-found");
            SoundManager.playFailure(player);
            return true;
        }

        KitShareManager.get().sendTransferRequest(player, target);
        cooldownManager.setCooldown(player);
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return List.of();
        }
        return PlayerUtil.completeOnlinePlayerNames(player, args[0]);
    }
}
