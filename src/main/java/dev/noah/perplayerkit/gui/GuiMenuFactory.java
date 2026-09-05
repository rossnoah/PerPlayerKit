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

import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.StyleManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;

public final class GuiMenuFactory {
    private GuiMenuFactory() {
    }

    /** A menu paired with the title it was built with, since canvas doesn't expose it. */
    public record TitledMenu(Menu menu, String title) {
    }

    private static String title(String key) {
        return StyleManager.get().getPrimaryColor()
                + LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(Lang.get().raw(key)));
    }

    private static String title(String key, String... pairs) {
        return StyleManager.get().getPrimaryColor()
                + LegacyComponentSerializer.legacySection().serialize(MiniMessage.miniMessage().deserialize(Lang.get().raw(key, pairs)));
    }

    private static TitledMenu chestMenu(String title) {
        // Redraw makes canvas swap menus inside the already-open inventory, so
        // the client keeps its cursor position instead of recentering on a
        // reopen. The reused inventory keeps its old title, which GUI fixes up
        // afterwards — only possible when the server has InventoryView#setTitle.
        Menu menu = ChestMenu.builder(6).title(title).redraw(GuiCompat.supportsTitleUpdate()).build();
        return new TitledMenu(menu, title);
    }

    public static TitledMenu createPublicKitRoomMenu() {
        return chestMenu(title("gui.public-kit-room-title"));
    }

    public static TitledMenu createKitMenu(int slot) {
        return chestMenu(title("gui.kit-editor-title", "slot", String.valueOf(slot)));
    }

    public static TitledMenu createPublicKitMenu(String id) {
        return chestMenu(title("gui.public-kit-editor-title", "id", id));
    }

    public static TitledMenu createECMenu(int slot) {
        return chestMenu(title("gui.enderchest-editor-title", "slot", String.valueOf(slot)));
    }

    public static TitledMenu createInspectMenu(int slot, String playerName) {
        return chestMenu(title("gui.inspect-kit-title", "player", playerName, "slot", String.valueOf(slot)));
    }

    public static TitledMenu createInspectEcMenu(int slot, String playerName) {
        return chestMenu(title("gui.inspect-ec-title", "player", playerName, "slot", String.valueOf(slot)));
    }

    public static TitledMenu createMainMenu(Player player, int page, int pages) {
        if (pages <= 1) {
            return chestMenu(title("gui.main-menu-title", "player", player.getName()));
        }
        return chestMenu(title("gui.main-menu-title-paged",
                "player", player.getName(),
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(pages)));
    }

    public static TitledMenu createKitRoomMenu() {
        return chestMenu(title("gui.kit-room-title"));
    }

    public static TitledMenu createKitRoomMenu(int page, int pages) {
        return chestMenu(title("gui.kit-room-title") + " (" + page + "/" + pages + ")");
    }

    public static TitledMenu createViewPublicKitMenu(String id) {
        return chestMenu(title("gui.view-public-kit-title", "id", id));
    }
}
