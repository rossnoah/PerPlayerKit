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

import org.bukkit.Material;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.ClickOptions;

import static dev.noah.perplayerkit.gui.ItemUtil.createGlassPane;
import static dev.noah.perplayerkit.gui.ItemUtil.createItem;

public final class GuiLayoutUtils {
    public static final int MENU_SIZE = 54;
    public static final int KIT_CONTENT_END = 41;
    public static final int EC_CONTENT_START = 9;
    public static final int EC_CONTENT_END = 36;
    public static final int FOOTER_START = 45;
    public static final int ARMOR_INDICATOR_START = 45;
    public static final int OFFHAND_INDICATOR_SLOT = 49;
    public static final int IMPORT_SLOT = 51;
    public static final int CLEAR_SLOT = 52;
    public static final int BACK_SLOT = 53;

    private GuiLayoutUtils() {
    }

    public static void allowModificationRange(Menu menu, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            menu.getSlot(i).setClickOptions(ClickOptions.ALLOW_ALL);
        }
    }

    public static void setGlassPaneRange(Menu menu, int startInclusive, int endExclusive) {
        for (int i = startInclusive; i < endExclusive; i++) {
            menu.getSlot(i).setItem(createGlassPane());
        }
    }

    public static void setArmorAndOffhandIndicators(Menu menu) {
        menu.getSlot(ARMOR_INDICATOR_START).setItem(createItem(Material.CHAINMAIL_BOOTS, 1, "<gray><b>BOOTS</b></gray>"));
        menu.getSlot(ARMOR_INDICATOR_START + 1).setItem(createItem(Material.CHAINMAIL_LEGGINGS, 1, "<gray><b>LEGGINGS</b></gray>"));
        menu.getSlot(ARMOR_INDICATOR_START + 2).setItem(createItem(Material.CHAINMAIL_CHESTPLATE, 1, "<gray><b>CHESTPLATE</b></gray>"));
        menu.getSlot(ARMOR_INDICATOR_START + 3).setItem(createItem(Material.CHAINMAIL_HELMET, 1, "<gray><b>HELMET</b></gray>"));
        menu.getSlot(OFFHAND_INDICATOR_SLOT).setItem(createItem(Material.SHIELD, 1, "<gray><b>OFFHAND</b></gray>"));
    }
}
