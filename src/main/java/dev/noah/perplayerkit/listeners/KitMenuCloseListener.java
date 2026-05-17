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
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.StyleManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class KitMenuCloseListener implements Listener {

    /**
     * Parse a rendered inventory title back into placeholder values, using the
     * localized template. Returns the placeholder values in order, or null if
     * the title doesn't match the template.
     *
     * If {@code second} is null, the template is treated as containing only one
     * placeholder and the returned array has length 1.
     */
    private static String[] parseTitle(String title, String key, String first, String second) {
        String template = Lang.get().raw(key);
        String firstToken = "{" + first + "}";
        int firstIdx = template.indexOf(firstToken);
        if (firstIdx < 0) {
            return null;
        }

        String fullPrefix = StyleManager.get().getPrimaryColor() + legacyConvert(template.substring(0, firstIdx));
        if (!title.startsWith(fullPrefix)) {
            return null;
        }
        String remainder = title.substring(fullPrefix.length());

        if (second == null) {
            String suffix = legacyConvert(template.substring(firstIdx + firstToken.length()));
            if (!remainder.endsWith(suffix)) {
                return null;
            }
            return new String[]{remainder.substring(0, remainder.length() - suffix.length())};
        }

        String secondToken = "{" + second + "}";
        int secondIdx = template.indexOf(secondToken);
        if (secondIdx < firstIdx + firstToken.length()) {
            return null;
        }
        String infix = legacyConvert(template.substring(firstIdx + firstToken.length(), secondIdx));
        String suffix = legacyConvert(template.substring(secondIdx + secondToken.length()));

        if (!remainder.endsWith(suffix)) {
            return null;
        }
        String body = remainder.substring(0, remainder.length() - suffix.length());
        int infixIdx = body.lastIndexOf(infix);
        if (infixIdx < 0) {
            return null;
        }
        return new String[]{body.substring(0, infixIdx), body.substring(infixIdx + infix.length())};
    }

    private static String legacyConvert(String miniMessage) {
        return LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(miniMessage));
    }

    @EventHandler
    public void onKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }
        InventoryView view = e.getView();
        Integer slot = parseSingleSlot(view.getTitle(), "gui.kit-editor-title", "slot");
        if (slot == null) {
            return;
        }
        Player p = (Player) e.getPlayer();
        ItemStack[] kit = copyContents(e.getInventory().getContents(), 41);
        KitManager.get().savekit(p.getUniqueId(), slot, kit);
    }

    @EventHandler
    public void onPublicKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }
        InventoryView view = e.getView();
        String[] parsed = parseTitle(view.getTitle(), "gui.public-kit-editor-title", "id", null);
        if (parsed == null || parsed.length != 1 || parsed[0].isEmpty()) {
            return;
        }
        Player player = (Player) e.getPlayer();
        ItemStack[] kit = copyContents(e.getInventory().getContents(), 41);
        KitManager.get().savePublicKit(player, parsed[0], kit);
    }

    @EventHandler
    public void onEnderchestEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }
        InventoryView view = e.getView();
        Integer slot = parseSingleSlot(view.getTitle(), "gui.enderchest-editor-title", "slot");
        if (slot == null) {
            return;
        }
        Player p = (Player) e.getPlayer();
        ItemStack[] ec = copyContentsFromOffset(e.getInventory().getContents(), 9, 27);
        KitManager.get().saveEC(p.getUniqueId(), slot, ec);
    }

    @EventHandler
    public void onInspectKitEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }
        InventoryView view = e.getView();
        String[] parsed = parseTitle(view.getTitle(), "gui.inspect-kit-title", "player", "slot");
        if (parsed == null) {
            return;
        }
        Player p = (Player) e.getPlayer();
        if (!p.hasPermission("perplayerkit.admin")) {
            return;
        }
        UUID targetUuid = GUI.getAndRemoveInspectTarget(p.getUniqueId());
        if (targetUuid == null) {
            return;
        }
        String playerName = parsed[0];
        int slot;
        try {
            slot = Integer.parseInt(parsed[1]);
        } catch (NumberFormatException ex) {
            return;
        }
        if (GUI.removeKitDeletionFlag(p)) {
            return;
        }
        ItemStack[] kit = copyContents(e.getInventory().getContents(), 41);
        if (KitManager.get().savekit(targetUuid, slot, kit, true)) {
            Lang.get().send(p, "success.admin-kit-updated", "slot", String.valueOf(slot), "player", playerName);
        } else {
            Lang.get().send(p, "error.failed-to-update-kit", "player", playerName);
        }
    }

    @EventHandler
    public void onInspectEnderchestEditorClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getSize() != 54 || inv.getLocation() != null) {
            return;
        }
        InventoryView view = e.getView();
        String[] parsed = parseTitle(view.getTitle(), "gui.inspect-ec-title", "player", "slot");
        if (parsed == null) {
            return;
        }
        Player p = (Player) e.getPlayer();
        if (!p.hasPermission("perplayerkit.admin")) {
            return;
        }
        UUID targetUuid = GUI.getAndRemoveInspectTarget(p.getUniqueId());
        if (targetUuid == null) {
            return;
        }
        String playerName = parsed[0];
        int slot;
        try {
            slot = Integer.parseInt(parsed[1]);
        } catch (NumberFormatException ex) {
            return;
        }
        if (GUI.removeKitDeletionFlag(p)) {
            return;
        }
        ItemStack[] ec = copyContentsFromOffset(e.getInventory().getContents(), 9, 27);
        if (KitManager.get().saveECSilent(targetUuid, slot, ec)) {
            Lang.get().send(p, "success.admin-ec-updated", "slot", String.valueOf(slot), "player", playerName);
        } else {
            Lang.get().send(p, "error.failed-to-update-ec", "player", playerName);
        }
    }

    private static Integer parseSingleSlot(String title, String key, String placeholder) {
        String[] parsed = parseTitle(title, key, placeholder, null);
        if (parsed == null || parsed.length != 1) {
            return null;
        }
        try {
            return Integer.parseInt(parsed[0].trim());
        } catch (NumberFormatException ex) {
            return null;
        }
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
