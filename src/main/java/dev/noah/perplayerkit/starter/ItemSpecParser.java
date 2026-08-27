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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the compact item notation used by the bundled starter defaults:
 *
 * <pre>MATERIAL [xAMOUNT] [{ key=value, ... }]</pre>
 *
 * {@code potion} and {@code name} are recognised attributes; every other key is
 * treated as an enchantment id. Deliberately free of Bukkit types so it can be
 * unit tested without a server - see {@link ItemSpecFactory} for the half that
 * needs one.
 */
public final class ItemSpecParser {

    private static final String POTION_KEY = "potion";
    private static final String NAME_KEY = "name";

    private ItemSpecParser() {
    }

    /**
     * Parses a slot's definition, which may offer alternatives separated by
     * {@code ||}: the first one the running server actually has is the one that
     * gets built, so newer gear can be listed first and still land on servers
     * too old to have it.
     *
     * <pre>MACE { density=5 } || NETHERITE_AXE { sharpness=5 }</pre>
     *
     * @return one spec per alternative, best first
     * @throws IllegalArgumentException if any alternative is malformed
     */
    public static List<ItemSpec> parseAll(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Empty item definition");
        }

        List<ItemSpec> specs = new ArrayList<>();
        for (String alternative : splitAlternatives(input)) {
            specs.add(parse(alternative));
        }
        if (specs.isEmpty()) {
            throw new IllegalArgumentException("Empty item definition");
        }
        return specs;
    }

    /** Splits on the {@code ||} that separates alternatives, ignoring quoted names. */
    private static List<String> splitAlternatives(String input) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                current.append(c);
            } else if (!quoted && c == '|' && i + 1 < input.length() && input.charAt(i + 1) == '|') {
                addIfPresent(parts, current);
                current.setLength(0);
                i++;
            } else {
                current.append(c);
            }
        }
        addIfPresent(parts, current);
        return parts;
    }

    /**
     * @throws IllegalArgumentException if the notation is malformed
     */
    public static ItemSpec parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Empty item definition");
        }

        String text = input.trim();
        String head = text;
        String attributes = null;

        int open = text.indexOf('{');
        if (open >= 0) {
            int close = text.lastIndexOf('}');
            if (close < open) {
                throw new IllegalArgumentException("Unclosed '{' in item definition: " + input);
            }
            head = text.substring(0, open).trim();
            attributes = text.substring(open + 1, close).trim();
        }

        String[] headParts = head.split("\\s+");
        if (headParts.length == 0 || headParts[0].isEmpty()) {
            throw new IllegalArgumentException("Missing material in item definition: " + input);
        }

        String material = headParts[0].toUpperCase(Locale.ROOT);
        int amount = 1;
        for (int i = 1; i < headParts.length; i++) {
            String part = headParts[i];
            if (part.length() < 2 || (part.charAt(0) != 'x' && part.charAt(0) != 'X')) {
                throw new IllegalArgumentException("Unexpected token '" + part + "' in item definition: " + input);
            }
            try {
                amount = Integer.parseInt(part.substring(1));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid amount '" + part + "' in item definition: " + input);
            }
            if (amount < 1) {
                throw new IllegalArgumentException("Amount must be at least 1 in item definition: " + input);
            }
        }

        String potionType = null;
        String displayName = null;
        Map<String, Integer> enchantments = new LinkedHashMap<>();

        if (attributes != null && !attributes.isEmpty()) {
            for (String entry : splitAttributes(attributes)) {
                int equals = entry.indexOf('=');
                if (equals < 0) {
                    throw new IllegalArgumentException("Attribute '" + entry + "' is missing '=' in item definition: " + input);
                }
                String key = entry.substring(0, equals).trim().toLowerCase(Locale.ROOT);
                String value = unquote(entry.substring(equals + 1).trim());
                if (key.isEmpty()) {
                    throw new IllegalArgumentException("Attribute with no name in item definition: " + input);
                }

                switch (key) {
                    case POTION_KEY -> potionType = value.toUpperCase(Locale.ROOT);
                    case NAME_KEY -> displayName = value;
                    default -> {
                        try {
                            enchantments.put(key, Integer.parseInt(value));
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException("Enchantment '" + key + "' needs a numeric level in item definition: " + input);
                        }
                    }
                }
            }
        }

        return new ItemSpec(material, amount, potionType, displayName, enchantments);
    }

    /**
     * Expands a slot key into the slots it covers. Accepts a single slot
     * ({@code "12"}), an inclusive range ({@code "9-11"}) or one of the caller's
     * aliases such as {@code "helmet"}.
     *
     * @param aliases named slots for this section, may be empty
     * @throws IllegalArgumentException if the key is not a slot, range or alias
     */
    public static List<Integer> parseSlots(String key, Map<String, Integer> aliases) {
        String text = key == null ? "" : key.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Empty slot key");
        }

        Integer alias = aliases.get(text.toLowerCase(Locale.ROOT));
        if (alias != null) {
            return List.of(alias);
        }

        int dash = text.indexOf('-', 1);
        if (dash < 0) {
            return List.of(parseSlotNumber(text, key));
        }

        int from = parseSlotNumber(text.substring(0, dash).trim(), key);
        int to = parseSlotNumber(text.substring(dash + 1).trim(), key);
        if (to < from) {
            throw new IllegalArgumentException("Slot range '" + key + "' ends before it starts");
        }

        List<Integer> slots = new ArrayList<>(to - from + 1);
        for (int slot = from; slot <= to; slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private static int parseSlotNumber(String text, String key) {
        try {
            int slot = Integer.parseInt(text);
            if (slot < 0) {
                throw new IllegalArgumentException("Slot '" + key + "' is negative");
            }
            return slot;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Slot '" + key + "' is not a number, range or named slot");
        }
    }

    /** Splits on commas that are not inside double quotes, so names may contain commas. */
    private static List<String> splitAttributes(String attributes) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < attributes.length(); i++) {
            char c = attributes.charAt(i);
            if (c == '"') {
                quoted = !quoted;
                current.append(c);
            } else if (c == ',' && !quoted) {
                addIfPresent(parts, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfPresent(parts, current);
        return parts;
    }

    private static void addIfPresent(List<String> parts, StringBuilder current) {
        String trimmed = current.toString().trim();
        if (!trimmed.isEmpty()) {
            parts.add(trimmed);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
