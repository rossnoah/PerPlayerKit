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
package dev.noah.perplayerkit.gui;

import dev.noah.perplayerkit.gui.configurable.ConfigurableGuiService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GUI {
    private static final Set<UUID> kitDeletionFlag = new HashSet<>();
    private static final Map<UUID, UUID> inspectTargets = new HashMap<>();

    public GUI(Plugin plugin) {
    }

    public static void setInspectTarget(UUID inspector, UUID target) {
        inspectTargets.put(inspector, target);
    }

    public static UUID getAndRemoveInspectTarget(UUID inspector) {
        return inspectTargets.remove(inspector);
    }

    public static boolean removeKitDeletionFlag(Player player) {
        return kitDeletionFlag.remove(player.getUniqueId());
    }

    public void OpenKitMenu(Player player, int slot) {
        ConfigurableGuiService.get().openPlayerKitEditor(player, slot);
    }

    public void OpenPublicKitEditor(Player player, String kitId) {
        ConfigurableGuiService.get().openPublicKitEditor(player, kitId);
    }

    public void OpenECKitKenu(Player player, int slot) {
        ConfigurableGuiService.get().openEnderchestEditor(player, slot);
    }

    public void InspectKit(Player player, UUID target, int slot) {
        setInspectTarget(player.getUniqueId(), target);
        ConfigurableGuiService.get().openInspectKit(player, target, slot);
    }

    public void InspectEc(Player player, UUID target, int slot) {
        setInspectTarget(player.getUniqueId(), target);
        ConfigurableGuiService.get().openInspectEnderchest(player, target, slot);
    }

    public void OpenMainMenu(Player player) {
        ConfigurableGuiService.get().openMainMenu(player);
    }

    public void OpenKitRoom(Player player) {
        ConfigurableGuiService.get().openKitRoom(player);
    }

    public void OpenKitRoom(Player player, int page) {
        ConfigurableGuiService.get().openKitRoom(player, page);
    }

    public Menu ViewPublicKitMenu(Player player, String id) {
        Menu menu = ConfigurableGuiService.get().createPublicKitViewer(player, id);
        if (menu != null) {
            menu.open(player);
        }
        return menu;
    }

    public void OpenPublicKitMenu(Player player) {
        ConfigurableGuiService.get().openPublicKitMenu(player);
    }
}
