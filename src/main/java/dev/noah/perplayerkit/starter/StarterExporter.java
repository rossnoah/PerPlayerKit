/*
 * Copyright 2026 Noah Ross
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
package dev.noah.perplayerkit.starter;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.PublicKit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * Dumps the live kit room in the notation {@code defaults/kitroom.yml} uses, so
 * a room arranged in game can be read back into the bundled defaults rather
 * than transcribed by hand.
 *
 * <p>This is a maintainer tool, not a feature: it only answers when the server
 * is started with {@code -Dperplayerkit.debug=true}, so it stays out of the way
 * on a normal install.
 */
public final class StarterExporter {

    private static final String DEBUG_PROPERTY = "perplayerkit.debug";
    private static final String FILE_NAME = "kitroom-export.yml";
    private static final String KITS_FILE_NAME = "publickits-export.yml";

    /** The kit slots that are written by name rather than number. */
    private static final Map<Integer, String> KIT_SLOT_NAMES = Map.of(
            36, "boots",
            37, "leggings",
            38, "chestplate",
            39, "helmet",
            40, "offhand"
    );

    private StarterExporter() {
    }

    /** @return whether the export command should answer at all on this server */
    public static boolean isEnabled() {
        return Boolean.getBoolean(DEBUG_PROPERTY);
    }

    /**
     * @return the file written
     * @throws IOException if the file could not be written
     */
    public static Path export(Plugin plugin) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# Exported from a live kit room. Slot keys are the 0-44 grid of a page.\n");
        out.append("pages:\n");

        for (int page = 0; page < KitRoomDataManager.PAGE_COUNT; page++) {
            out.append("\n  - id: page").append(page + 1).append('\n');
            out.append("    items:\n");
            writePage(out, KitRoomDataManager.get().getKitRoomPage(page));
        }

        Path file = plugin.getDataFolder().toPath().resolve(FILE_NAME);
        Files.createDirectories(file.getParent());
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * The same for the public kits, in the layout {@code defaults/publickits.yml}
     * uses: numbered inventory slots plus the named equipment ones.
     *
     * @return the file written
     */
    public static Path exportPublicKits(Plugin plugin) throws IOException {
        StringBuilder out = new StringBuilder();
        out.append("# Exported from live public kits. 0-8 hotbar, 9-35 inventory,\n");
        out.append("# then the named equipment slots.\n");
        out.append("kits:\n");

        for (PublicKit kit : KitManager.get().getPublicKitList()) {
            out.append("\n  ").append(kit.id).append(":\n    items:\n");
            writeKit(out, KitManager.get().getPublicKit(kit.id));
        }

        Path file = plugin.getDataFolder().toPath().resolve(KITS_FILE_NAME);
        Files.createDirectories(file.getParent());
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        return file;
    }

    /**
     * Writes one player's kit out in the same layout, so a kit somebody built
     * for themselves can be promoted into the bundled public kits.
     *
     * @param slot the kit number as it appears in {@code /kit}
     * @return the file written
     */
    public static Path exportPlayerKit(Plugin plugin, UUID uuid, int slot) throws IOException {
        KitManager.get().loadPlayerDataFromDB(uuid);

        StringBuilder out = new StringBuilder();
        out.append("# Exported from a player's kit ").append(slot).append(".\n");
        out.append("kits:\n\n  kit").append(slot).append(":\n    items:\n");
        writeKit(out, KitManager.get().getPlayerKit(uuid, slot));

        Path file = plugin.getDataFolder().toPath().resolve("playerkit-" + slot + "-export.yml");
        Files.createDirectories(file.getParent());
        Files.writeString(file, out.toString(), StandardCharsets.UTF_8);
        return file;
    }

    private static void writeKit(StringBuilder out, ItemStack[] contents) {
        if (contents == null) {
            out.append("      # kit has no contents saved\n");
            return;
        }

        for (int slot = 0; slot < contents.length && slot < StarterDefaults.KIT_SIZE; slot++) {
            writeSlot(out, KIT_SLOT_NAMES.getOrDefault(slot, String.valueOf(slot)), contents[slot]);
        }
    }

    private static void writePage(StringBuilder out, ItemStack[] contents) {
        if (contents == null) {
            out.append("      # page has never been saved\n");
            return;
        }

        for (int slot = 0; slot < contents.length && slot < StarterDefaults.KIT_ROOM_PAGE_SIZE; slot++) {
            writeSlot(out, String.valueOf(slot), contents[slot]);
        }
    }

    /** One line, or a block with a {@code contents:} list when it is a full shulker. */
    private static void writeSlot(StringBuilder out, String key, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        Map<Integer, ItemStack> inside = ItemSpecWriter.contentsOf(item);
        if (inside.isEmpty()) {
            out.append("      ").append(key).append(": \"").append(ItemSpecWriter.write(item)).append("\"\n");
            return;
        }

        out.append("      ").append(key).append(":\n");
        out.append("        item: \"").append(ItemSpecWriter.write(item)).append("\"\n");
        out.append("        contents:\n");
        inside.forEach((nestedSlot, nested) ->
                out.append("          ").append(nestedSlot).append(": \"")
                        .append(ItemSpecWriter.write(nested)).append("\"\n"));
    }
}
