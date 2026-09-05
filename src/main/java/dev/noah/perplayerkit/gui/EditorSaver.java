/*
 * Copyright 2022-2026 Noah Ross
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
package dev.noah.perplayerkit.gui;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.commands.core.ActionGuards;
import java.util.Arrays;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Persists the contents of a kit/enderchest editor menu. Invoked both when the
 * inventory actually closes (KitMenuCloseListener) and when the player
 * navigates to another menu that reuses the open inventory, where no
 * InventoryCloseEvent ever fires (GUI redraw navigation).
 */
public final class EditorSaver {

    private EditorSaver() {
    }

    public static void save(Player player, GUI.EditorContext context, Inventory inv) {
        String permission = switch (context.type()) {
            case KIT -> "perplayerkit.kit";
            case ENDERCHEST -> "perplayerkit.enderchest";
            default -> "perplayerkit.admin";
        };
        if (!ActionGuards.allowed(player, permission)) { GUI.takeClearFlag(player); return; }
        switch (context.type()) {
            case KIT -> saveKit(player, context.slot(), inv);
            case PUBLIC_KIT -> savePublicKit(player, context.id(), inv);
            case ENDERCHEST -> saveEnderchest(player, context.slot(), inv);
            case INSPECT_KIT -> saveInspectedKit(player, context, inv);
            case INSPECT_ENDERCHEST -> saveInspectedEnderchest(player, context, inv);
        }
    }

    private static void saveKit(Player player, int slot, Inventory inv) {
        ItemStack[] kit = copyContents(inv.getContents(), 41);
        boolean cleared = GUI.takeClearFlag(player);
        ItemStack[] stored = KitManager.get().getPlayerKit(player.getUniqueId(), slot);
        if (!cleared && Arrays.equals(kit, ItemFilter.get().filterItemStack(stored))) return;
        if (isEmpty(kit)) {
            if (stored != null && ActionGuards.allowed(player, "perplayerkit.deletekit")) {
                KitManager.get().deleteKit(player.getUniqueId(), slot);
                Lang.get().send(player, "success.kit-deleted", "slot", String.valueOf(slot));
            }
        } else KitManager.get().savekit(player.getUniqueId(), slot, kit);
    }

    private static void savePublicKit(Player player, String id, Inventory inv) {
        if (id == null || id.isEmpty()) {
            return;
        }
        ItemStack[] kit = copyContents(inv.getContents(), 41);
        GUI.takeClearFlag(player);
        if (isEmpty(kit)) KitManager.get().deletePublicKit(id);
        else if (!Arrays.equals(kit, KitManager.get().getPublicKit(id))) KitManager.get().savePublicKit(player, id, kit);
    }

    private static void saveEnderchest(Player player, int slot, Inventory inv) {
        ItemStack[] ec = copyContentsFromOffset(inv.getContents(), 9, 27);
        boolean cleared = GUI.takeClearFlag(player);
        ItemStack[] stored = KitManager.get().getPlayerEC(player.getUniqueId(), slot);
        if (!cleared && Arrays.equals(ec, ItemFilter.get().filterItemStack(stored))) return;
        if (isEmpty(ec)) {
            if (stored != null && ActionGuards.allowed(player, "perplayerkit.deleteenderchest")) {
                KitManager.get().deleteEnderchest(player.getUniqueId(), slot);
                Lang.get().send(player, "success.ec-deleted", "slot", String.valueOf(slot));
            }
        } else KitManager.get().saveEC(player.getUniqueId(), slot, ec);
    }

    private static void saveInspectedKit(Player player, GUI.EditorContext context, Inventory inv) {
        if (!player.hasPermission("perplayerkit.admin") || context.target() == null || GUI.removeKitDeletionFlag(player)) {
            return;
        }
        UUID targetUuid = context.target();
        int slot = context.slot();
        String playerName = context.playerName();
        ItemStack[] kit = copyContents(inv.getContents(), 41);
        if (KitManager.get().savekit(targetUuid, slot, kit, true)) {
            Lang.get().send(player, "success.admin-kit-updated", "slot", String.valueOf(slot), "player", playerName);
        } else {
            Lang.get().send(player, "error.failed-to-update-kit", "player", playerName);
        }
    }

    private static void saveInspectedEnderchest(Player player, GUI.EditorContext context, Inventory inv) {
        if (!player.hasPermission("perplayerkit.admin") || context.target() == null || GUI.removeKitDeletionFlag(player)) {
            return;
        }
        UUID targetUuid = context.target();
        int slot = context.slot();
        String playerName = context.playerName();
        ItemStack[] ec = copyContentsFromOffset(inv.getContents(), 9, 27);
        if (KitManager.get().saveECSilent(targetUuid, slot, ec)) {
            Lang.get().send(player, "success.admin-ec-updated", "slot", String.valueOf(slot), "player", playerName);
        } else {
            Lang.get().send(player, "error.failed-to-update-ec", "player", playerName);
        }
    }

    private static boolean isEmpty(ItemStack[] items) {
        return Arrays.stream(items).allMatch(item -> item == null || item.getType().isAir());
    }

    private static ItemStack[] copyContents(ItemStack[] source, int count) {
        ItemStack[] out = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            out[i] = source[i] == null ? null : source[i].clone();
        }
        return out;
    }

    private static ItemStack[] copyContentsFromOffset(ItemStack[] source, int offset, int count) {
        ItemStack[] out = new ItemStack[count];
        for (int i = 0; i < count; i++) {
            out[i] = source[i + offset] == null ? null : source[i + offset].clone();
        }
        return out;
    }
}
