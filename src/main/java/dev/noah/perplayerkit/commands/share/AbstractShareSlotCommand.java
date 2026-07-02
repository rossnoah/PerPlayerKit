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
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;

public abstract class AbstractShareSlotCommand implements CommandExecutor {

    @FunctionalInterface
    public interface ShareRequestAction {
        void share(Player sender, int slot, Player target);
    }

    private static final int COOLDOWN_SECONDS = 5;
    private final CooldownManager cooldownManager = new CooldownManager(COOLDOWN_SECONDS);
    private final String missingSlotMessageKey;
    private final BiConsumer<Player, Integer> codeShareAction;
    private final ShareRequestAction requestShareAction;

    protected AbstractShareSlotCommand(String missingSlotMessageKey, BiConsumer<Player, Integer> codeShareAction,
                                       ShareRequestAction requestShareAction) {
        this.missingSlotMessageKey = missingSlotMessageKey;
        this.codeShareAction = codeShareAction;
        this.requestShareAction = requestShareAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = CommandGuards.requirePlayer(sender);
        if (player == null) {
            return true;
        }

        if (args.length < 1) {
            Lang.get().send(player, missingSlotMessageKey);
            SoundManager.playFailure(player);
            return true;
        }

        if (cooldownManager.isOnCooldown(player)) {
            Lang.get().send(player, "error.command-cooldown");
            SoundManager.playFailure(player);
            return true;
        }

        Integer slot = SlotArgumentParser.parseSlotInRange(args[0], 1, KitSlots.maxKits());
        if (slot == null) {
            Lang.get().send(player, "error.invalid-kit-slot");
            SoundManager.playFailure(player);
            return true;
        }

        if (args.length >= 2) {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                Lang.get().send(player, "error.share-player-not-found");
                SoundManager.playFailure(player);
                return true;
            }
            requestShareAction.share(player, slot, target);
        } else {
            codeShareAction.accept(player, slot);
        }
        cooldownManager.setCooldown(player);
        return true;
    }
}
