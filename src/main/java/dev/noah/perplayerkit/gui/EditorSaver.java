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

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitContents;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.commands.core.ActionGuards;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.UUID;

/** Saves editors on close and on canvas redraw navigation, which does not fire a close event. */
public final class EditorSaver {
    private EditorSaver() {}

    public static void save(Player player, GUI.EditorContext context, Inventory inventory) {
        boolean cleared = GUI.takeClearFlag(player);
        boolean inspection = context.type() == GUI.EditorType.INSPECT_KIT || context.type() == GUI.EditorType.INSPECT_ENDERCHEST;
        if (inspection) {
            boolean deleted = GUI.removeKitDeletionFlag(player);
            // Staff inspection is read-only; closing it is not an attempted admin action.
            if (deleted || context.target() == null || !player.hasPermission("perplayerkit.admin")) return;
        }
        String permission = switch (context.type()) {
            case KIT -> "perplayerkit.kit";
            case ENDERCHEST -> "perplayerkit.enderchest";
            default -> "perplayerkit.admin";
        };
        if (!ActionGuards.allowed(player, permission)) return;
        ItemStack[] view = inventory.getContents();
        if (view == null || view.length != GuiLayoutUtils.MENU_SIZE) return;

        switch (context.type()) {
            case KIT, ENDERCHEST -> savePersonal(player, context.slot(), view, context.type() == GUI.EditorType.ENDERCHEST, cleared);
            case PUBLIC_KIT -> savePublicKit(player, context.id(), view);
            case INSPECT_KIT, INSPECT_ENDERCHEST -> saveInspection(player, context, view);
        }
    }

    private static ItemStack[] editableContents(ItemStack[] view, boolean enderchest) {
        return KitContents.copyRange(view, enderchest ? GuiLayoutUtils.EC_CONTENT_START : 0,
                enderchest ? KitContents.ENDERCHEST_SIZE : KitContents.INVENTORY_SIZE);
    }

    private static void savePersonal(Player player, int slot, ItemStack[] view, boolean enderchest, boolean cleared) {
        KitManager kits = KitManager.get();
        UUID uuid = player.getUniqueId();
        ItemStack[] items = editableContents(view, enderchest);
        ItemStack[] stored = enderchest ? kits.getPlayerEC(uuid, slot) : kits.getPlayerKit(uuid, slot);
        if (!cleared && Arrays.equals(items, ItemFilter.get().filterItemStack(stored))) return;
        if (KitContents.isEmpty(items)) {
            String permission = enderchest ? "perplayerkit.deleteenderchest" : "perplayerkit.deletekit";
            if (stored != null && ActionGuards.allowed(player, permission)) {
                if (enderchest) kits.deleteEnderchest(uuid, slot);
                else kits.deleteKit(uuid, slot);
                Lang.get().send(player, enderchest ? "success.ec-deleted" : "success.kit-deleted", "slot", String.valueOf(slot));
            }
        } else if (enderchest) kits.saveEC(uuid, slot, items);
        else kits.savekit(uuid, slot, items);
    }

    private static void savePublicKit(Player player, String id, ItemStack[] view) {
        if (id == null || id.isEmpty()) return;
        KitManager kits = KitManager.get();
        ItemStack[] items = editableContents(view, false);
        if (KitContents.isEmpty(items)) kits.deletePublicKit(id);
        else if (!Arrays.equals(items, kits.getPublicKit(id))) kits.savePublicKit(player, id, items);
    }

    private static void saveInspection(Player player, GUI.EditorContext context, ItemStack[] view) {
        boolean enderchest = context.type() == GUI.EditorType.INSPECT_ENDERCHEST;
        ItemStack[] items = editableContents(view, enderchest);
        KitManager kits = KitManager.get();
        ItemStack[] stored = enderchest ? kits.getPlayerEC(context.target(), context.slot()) : kits.getPlayerKit(context.target(), context.slot());
        if (Arrays.equals(items, stored)) return;
        boolean saved = enderchest ? kits.saveECSilent(context.target(), context.slot(), items)
                : kits.savekit(context.target(), context.slot(), items, true);
        String success = enderchest ? "success.admin-ec-updated" : "success.admin-kit-updated";
        String failure = enderchest ? "error.failed-to-update-ec" : "error.failed-to-update-kit";
        Lang.get().send(player, saved ? success : failure, "slot", String.valueOf(context.slot()), "player", context.playerName());
    }
}
