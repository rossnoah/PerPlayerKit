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

import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.commands.core.SlotArgumentParser;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public abstract class AbstractShareSlotCommand implements CommandExecutor {

    private static final int COOLDOWN_SECONDS = 5;
    private final CooldownManager cooldownManager = new CooldownManager(COOLDOWN_SECONDS);
    private final String missingSlotMessage;
    private final BiConsumer<Player, Integer> shareAction;

    protected AbstractShareSlotCommand(String missingSlotMessage, BiConsumer<Player, Integer> shareAction) {
        this.missingSlotMessage = missingSlotMessage;
        this.shareAction = shareAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = CommandGuards.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + missingSlotMessage);
            SoundManager.playFailure(player);
            return true;
        }

        if (cooldownManager.isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Please don't spam the command (5 second cooldown)");
            SoundManager.playFailure(player);
            return true;
        }

        Integer slot = SlotArgumentParser.parseSlotInRange(args[0], 1, 9);
        if (slot == null) {
            player.sendMessage(ChatColor.RED + "Select a valid kit slot");
            SoundManager.playFailure(player);
            return true;
        }

        shareAction.accept(player, slot);
        cooldownManager.setCooldown(player);
        return true;
    }
}
