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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Reads the starter content bundled inside the jar under {@code defaults/}.
 *
 * <p>These files are deliberately never written to the plugin folder. They are
 * what a fresh install starts with; after that the kit room and public kits live
 * in storage and are edited in game.
 *
 * <p>Parsing is split from item building so the shipped files can be validated
 * in unit tests, where there is no server to make {@link ItemStack}s with.
 */
public final class StarterDefaults {

    /** Slots on one kit room page: five rows of nine. */
    public static final int KIT_ROOM_PAGE_SIZE = 45;

    /** Slots in a kit: 0-35 inventory, 36-39 armour, 40 offhand. */
    public static final int KIT_SIZE = 41;

    /** Slots inside a shulker box, the only container the defaults put items in. */
    public static final int CONTAINER_SIZE = 27;

    public static final String KIT_ROOM_RESOURCE = "defaults/kitroom.yml";
    public static final String PUBLIC_KITS_RESOURCE = "defaults/publickits.yml";

    private static final Map<String, Integer> KIT_SLOT_ALIASES = Map.of(
            "boots", 36,
            "leggings", 37,
            "chestplate", 38,
            "helmet", 39,
            "offhand", 40
    );

    private StarterDefaults() {
    }

    /**
     * One line of a defaults file: every slot it was written into, and the item
     * itself as a list of alternatives - the first one a server has is the one
     * it gets.
     */
    public record SlotItem(List<Integer> slots, List<ItemSpec> options) {

        /** The item as written, before any fallback for an older server. */
        public ItemSpec spec() {
            return options.get(0);
        }
    }

    /** A kit room page or a public kit: a named, fixed-size grid of items. */
    public record Section(String label, int size, List<SlotItem> items) {
    }

    // Parsing - no Bukkit item classes involved, safe to unit test.

    /**
     * @param warn receives one line per definition that could not be understood
     */
    public static List<Section> parseKitRoomPages(YamlConfiguration config, Consumer<String> warn) {
        List<Section> pages = new ArrayList<>();

        List<Map<?, ?>> rawPages = config.getMapList("pages");
        for (int index = 0; index < rawPages.size(); index++) {
            Map<?, ?> rawPage = rawPages.get(index);
            Object id = rawPage.get("id");
            String label = "page " + (id != null ? id : index + 1);

            Object rawItems = rawPage.get("items");
            if (!(rawItems instanceof Map<?, ?> items)) {
                warn.accept(label + " has no items section, skipping");
                continue;
            }

            pages.add(parseSection(label, KIT_ROOM_PAGE_SIZE, toStringKeys(items), Map.of(), warn));
        }
        return pages;
    }

    /**
     * @return public kit id to its contents, in the order the file lists them
     */
    public static Map<String, Section> parsePublicKits(YamlConfiguration config, Consumer<String> warn) {
        Map<String, Section> kits = new LinkedHashMap<>();

        ConfigurationSection kitsSection = config.getConfigurationSection("kits");
        if (kitsSection == null) {
            warn.accept("no kits section");
            return kits;
        }

        for (String id : kitsSection.getKeys(false)) {
            String label = "kit " + id;
            ConfigurationSection itemsSection = kitsSection.getConfigurationSection(id + ".items");
            if (itemsSection == null) {
                warn.accept(label + " has no items section, skipping");
                continue;
            }

            Map<String, Object> items = new LinkedHashMap<>();
            for (String key : itemsSection.getKeys(false)) {
                items.put(key, itemsSection.get(key));
            }

            kits.put(id.toLowerCase(Locale.ROOT), parseSection(label, KIT_SIZE, items, KIT_SLOT_ALIASES, warn));
        }
        return kits;
    }

    /**
     * A slot is normally one line of item notation. A shulker box is written as
     * a block instead, so it can carry what is inside it:
     *
     * <pre>
     * 13:
     *   item: "PURPLE_SHULKER_BOX"
     *   contents:
     *     0-26: "SPLASH_POTION { potion=STRONG_HEALING }"
     * </pre>
     */
    private static List<ItemSpec> parseValue(String label, Object value, Consumer<String> warn) {
        Map<String, Object> mapping = asStringKeyedMap(value);
        if (mapping == null) {
            return ItemSpecParser.parseAll(String.valueOf(value));
        }

        Object item = mapping.get("item");
        if (item == null) {
            throw new IllegalArgumentException("container has no 'item' line");
        }

        List<ItemSpec> options = ItemSpecParser.parseAll(String.valueOf(item));
        Map<String, Object> contents = asStringKeyedMap(mapping.get("contents"));
        if (contents == null || contents.isEmpty()) {
            return options;
        }

        Map<Integer, ItemSpec> inside = parseContents(label, options.get(0), contents, warn);
        List<ItemSpec> withContents = new ArrayList<>(options.size());
        for (ItemSpec option : options) {
            withContents.add(option.withContents(inside));
        }
        return withContents;
    }

    private static Map<Integer, ItemSpec> parseContents(String label, ItemSpec container,
                                                        Map<String, Object> contents, Consumer<String> warn) {
        Map<Integer, ItemSpec> parsed = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : contents.entrySet()) {
            if (entry.getValue() == null) {
                continue;
            }
            List<Integer> slots;
            ItemSpec spec;
            try {
                slots = ItemSpecParser.parseSlots(entry.getKey(), Map.of());
                spec = ItemSpecParser.parse(String.valueOf(entry.getValue()));
            } catch (IllegalArgumentException e) {
                warn.accept(label + ": inside " + container.getMaterial() + ": " + e.getMessage());
                continue;
            }

            for (int slot : slots) {
                if (slot >= CONTAINER_SIZE) {
                    warn.accept(label + ": " + container.getMaterial() + " has no slot " + slot
                            + ", it holds 0-" + (CONTAINER_SIZE - 1));
                    continue;
                }
                if (parsed.putIfAbsent(slot, spec) != null) {
                    warn.accept(label + ": " + container.getMaterial() + " fills slot " + slot + " twice");
                }
            }
        }
        return parsed;
    }

    /** YAML hands nested blocks back as either a raw Map or a ConfigurationSection. */
    private static Map<String, Object> asStringKeyedMap(Object value) {
        if (value instanceof ConfigurationSection section) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (String key : section.getKeys(false)) {
                map.put(key, section.get(key));
            }
            return map;
        }
        if (value instanceof Map<?, ?> map) {
            return toStringKeys(map);
        }
        return null;
    }

    private static Section parseSection(String label, int size, Map<String, Object> items,
                                        Map<String, Integer> aliases, Consumer<String> warn) {
        List<SlotItem> parsed = new ArrayList<>();

        for (Map.Entry<String, Object> entry : items.entrySet()) {
            String slotKey = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            List<Integer> slots;
            List<ItemSpec> options;
            try {
                slots = ItemSpecParser.parseSlots(slotKey, aliases);
                options = parseValue(label, value, warn);
            } catch (IllegalArgumentException e) {
                warn.accept(label + ": " + e.getMessage());
                continue;
            }

            List<Integer> inRange = new ArrayList<>(slots.size());
            for (int slot : slots) {
                if (slot >= size) {
                    warn.accept(label + ": slot " + slot + " is outside 0-" + (size - 1) + ", skipping");
                    continue;
                }
                inRange.add(slot);
            }

            if (!inRange.isEmpty()) {
                parsed.add(new SlotItem(List.copyOf(inRange), options));
            }
        }

        return new Section(label, size, parsed);
    }

    // Loading - reads the jar resources and builds real items.

    /**
     * @return one entry per page defined in the bundled file, in order
     */
    public static List<ItemStack[]> loadKitRoomPages(Plugin plugin) {
        YamlConfiguration config = load(plugin, KIT_ROOM_RESOURCE);
        if (config == null) {
            return List.of();
        }

        Consumer<String> warn = warning -> plugin.getLogger().warning(KIT_ROOM_RESOURCE + " " + warning);
        List<ItemStack[]> pages = new ArrayList<>();
        for (Section page : parseKitRoomPages(config, warn)) {
            pages.add(build(page, warn));
        }
        return pages;
    }

    /**
     * Builds only the kits asked for. A kit this server does not offer - the
     * mace kit on anything older than 1.21 - is never built, so its gear does
     * not report itself missing on servers that were never going to list it.
     *
     * @param wanted lowercase ids of the kits to build
     * @return public kit id to contents, for every wanted kit the file defines
     */
    public static Map<String, ItemStack[]> loadPublicKits(Plugin plugin, Set<String> wanted) {
        YamlConfiguration config = load(plugin, PUBLIC_KITS_RESOURCE);
        if (config == null) {
            return Map.of();
        }

        Consumer<String> warn = warning -> plugin.getLogger().warning(PUBLIC_KITS_RESOURCE + " " + warning);
        Map<String, ItemStack[]> kits = new LinkedHashMap<>();
        for (Map.Entry<String, Section> entry : parsePublicKits(config, warn).entrySet()) {
            if (wanted.contains(entry.getKey())) {
                kits.put(entry.getKey(), build(entry.getValue(), warn));
            }
        }
        return kits;
    }

    private static ItemStack[] build(Section section, Consumer<String> warn) {
        ItemStack[] contents = new ItemStack[section.size()];

        for (SlotItem slotItem : section.items()) {
            ItemStack item = ItemSpecFactory.create(slotItem.options(),
                    warning -> warn.accept(section.label() + ": " + warning));
            if (item == null) {
                continue;
            }
            for (int slot : slotItem.slots()) {
                contents[slot] = item.clone();
            }
        }

        return contents;
    }

    /** YAML keys are Integers for plain slot numbers and Strings for ranges and names. */
    private static Map<String, Object> toStringKeys(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private static YamlConfiguration load(Plugin plugin, String resource) {
        try (InputStream in = plugin.getResource(resource)) {
            if (in == null) {
                plugin.getLogger().warning("Bundled resource " + resource + " is missing from the jar");
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to read bundled resource " + resource + ": " + e.getMessage());
            return null;
        }
    }
}
