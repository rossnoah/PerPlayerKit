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
package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MiniMessage parsing and conversion to the legacy strings Bukkit's item and
 * inventory APIs take.
 */
public final class TextUtil {

    /**
     * Section-sign serializer that keeps hex colors as §x§R§R§G§G§B§B, the
     * form Bukkit reads. {@code legacySection()} rounds every hex color to one
     * of the 16 named colors, which flattens gradients.
     */
    public static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final Pattern LEGACY_HEX = Pattern.compile("(?i)§x((?:§[0-9a-f]){6})");
    private static final Pattern LEGACY_CODE = Pattern.compile("(?i)§([0-9a-fk-or])");
    private static final Map<Character, String> CODE_TAGS = Map.ofEntries(
            Map.entry('0', "black"), Map.entry('1', "dark_blue"), Map.entry('2', "dark_green"),
            Map.entry('3', "dark_aqua"), Map.entry('4', "dark_red"), Map.entry('5', "dark_purple"),
            Map.entry('6', "gold"), Map.entry('7', "gray"), Map.entry('8', "dark_gray"),
            Map.entry('9', "blue"), Map.entry('a', "green"), Map.entry('b', "aqua"),
            Map.entry('c', "red"), Map.entry('d', "light_purple"), Map.entry('e', "yellow"),
            Map.entry('f', "white"), Map.entry('k', "obfuscated"), Map.entry('l', "bold"),
            Map.entry('m', "strikethrough"), Map.entry('n', "underlined"), Map.entry('o', "italic"),
            Map.entry('r', "reset"));
    // Never produced by MiniMessage or typed in a config, so it can mark the end of a
    // string whose trailing formatting would otherwise be dropped.
    private static final String MARKER = "";

    private TextUtil() {
    }

    /**
     * Parses MiniMessage. Section-sign codes, which arrive through display names
     * and PlaceholderAPI, are converted to tags first because MiniMessage throws
     * on them.
     */
    public static Component parse(String miniMessage) {
        return MM.deserialize(legacyCodesToTags(miniMessage));
    }

    /** MiniMessage to a legacy string for item names, lore and inventory titles. */
    public static String toLegacy(String miniMessage) {
        String tags = legacyCodesToTags(miniMessage);
        String legacy = LEGACY.serialize(MM.deserialize(tags));
        // The serializer drops a leading reset since there's nothing before it to
        // reset, but callers put it there on purpose, so keep it.
        return tags.startsWith("<reset>") ? "§r" + legacy : legacy;
    }

    /**
     * Legacy codes for formatting tags with no text, like {@code <blue><bold>},
     * to put in front of other legacy text.
     */
    public static String formattingPrefix(String miniMessage) {
        String legacy = toLegacy(miniMessage + MARKER);
        return legacy.endsWith(MARKER) ? legacy.substring(0, legacy.length() - MARKER.length()) : legacy;
    }

    static String legacyCodesToTags(String input) {
        if (input == null || input.indexOf('§') < 0) {
            return input;
        }
        Matcher hex = LEGACY_HEX.matcher(input);
        StringBuilder out = new StringBuilder();
        while (hex.find()) {
            String digits = hex.group(1).replace("§", "");
            hex.appendReplacement(out, "<reset><#" + digits.toLowerCase(Locale.ROOT) + ">");
        }
        hex.appendTail(out);

        Matcher code = LEGACY_CODE.matcher(out.toString());
        out = new StringBuilder();
        while (code.find()) {
            char c = Character.toLowerCase(code.group(1).charAt(0));
            String tag = CODE_TAGS.get(c);
            // A legacy color also clears bold, italic and the rest, so reset first.
            boolean color = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f');
            code.appendReplacement(out, color ? "<reset><" + tag + ">" : "<" + tag + ">");
        }
        code.appendTail(out);
        // Drop stray section signs that aren't part of a code.
        return out.toString().replace("§", "");
    }
}
