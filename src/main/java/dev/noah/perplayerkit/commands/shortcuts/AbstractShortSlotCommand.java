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
package dev.noah.perplayerkit.commands.shortcuts;

import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public abstract class AbstractShortSlotCommand implements CommandExecutor {

    private final String shortPrefix;
    private final String longPrefix;

    protected AbstractShortSlotCommand(String shortPrefix, String longPrefix) {
        this.shortPrefix = shortPrefix;
        this.longPrefix = longPrefix;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            Lang.get().sendNoPrefix(sender, "error.players-only");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }

        Integer slot = parseSlot(label);
        if (slot == null) {
            Lang.get().send(player, "error.invalid-command-label");
            return true;
        }

        if (slot > KitSlots.maxKits()) {
            // Statically registered commands like /k9 stay available even when
            // max-kits is lowered below 9, so give a range error, not a label error.
            Lang.get().send(player, "error.invalid-slot-range",
                    "min", String.valueOf(KitSlots.MIN_LIMIT), "max", String.valueOf(KitSlots.maxKits()));
            return true;
        }

        executeForSlot(player, slot);
        return true;
    }

    protected abstract void executeForSlot(Player player, int slot);

    private Integer parseSlot(String label) {
        // Strip the plugin namespace so /perplayerkit:k10 works
        String normalizedLabel = label.substring(label.indexOf(':') + 1).toLowerCase(Locale.ROOT);
        Integer fromShort = parseSlotSuffix(normalizedLabel, shortPrefix);
        if (fromShort != null) {
            return fromShort;
        }
        return parseSlotSuffix(normalizedLabel, longPrefix);
    }

    private Integer parseSlotSuffix(String label, String prefix) {
        if (!label.startsWith(prefix)) {
            return null;
        }
        return KitSlots.parseSlotSuffix(label.substring(prefix.length()));
    }
}
