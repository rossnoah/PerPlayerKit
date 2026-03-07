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

import dev.noah.perplayerkit.util.StyleManager;
import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;

public final class GuiMenuFactory {
    private GuiMenuFactory() {
    }

    public static Menu createPublicKitRoomMenu() {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Public Kit Room").redraw(true).build();
    }

    public static Menu createKitMenu(int slot) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Kit: " + slot).build();
    }

    public static Menu createPublicKitMenu(String id) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Public Kit: " + id).build();
    }

    public static Menu createECMenu(int slot) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Enderchest: " + slot).build();
    }

    public static Menu createInspectMenu(int slot, String playerName) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Inspecting " + playerName + "'s kit " + slot).build();
    }

    public static Menu createInspectEcMenu(int slot, String playerName) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Inspecting " + playerName + "'s enderchest " + slot).build();
    }

    public static Menu createMainMenu(Player player) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + player.getName() + "'s Kits").build();
    }

    public static Menu createKitRoomMenu() {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Kit Room").redraw(true).build();
    }

    public static Menu createViewPublicKitMenu(String id) {
        return ChestMenu.builder(6).title(StyleManager.get().getPrimaryColor() + "Viewing Public Kit: " + id).redraw(true).build();
    }
}
