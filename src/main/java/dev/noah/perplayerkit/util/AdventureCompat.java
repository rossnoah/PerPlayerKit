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
package dev.noah.perplayerkit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Adventure API compatibility utility that provides modern text component support
 * while maintaining backward compatibility with legacy ChatColor.
 */
public class AdventureCompat {
    
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    private static final LegacyComponentSerializer SECTION_SERIALIZER = 
        LegacyComponentSerializer.legacySection();
    
    /**
     * Convert a legacy color string (with & codes) to an Adventure Component
     */
    public static Component fromLegacy(String legacyText) {
        if (legacyText == null) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(legacyText);
    }
    
    /**
     * Convert an Adventure Component to legacy string
     */
    public static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(component);
    }
    
    /**
     * Send a message to a CommandSender using Adventure API if available, fallback to legacy
     */
    public static void sendMessage(CommandSender sender, String message) {
        // Use legacy format for compatibility across all server types
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
    
    /**
     * Send a Component message to a CommandSender
     */
    public static void sendMessage(CommandSender sender, Component component) {
        // Always convert to legacy string for compatibility
        String legacyMessage = toLegacy(component);
        sender.sendMessage(legacyMessage);
    }
    
    /**
     * Create common color components
     */
    public static class Colors {
        public static final Component RED = Component.text("").color(NamedTextColor.RED);
        public static final Component GREEN = Component.text("").color(NamedTextColor.GREEN);
        public static final Component BLUE = Component.text("").color(NamedTextColor.BLUE);
        public static final Component YELLOW = Component.text("").color(NamedTextColor.YELLOW);
        public static final Component AQUA = Component.text("").color(NamedTextColor.AQUA);
        public static final Component PURPLE = Component.text("").color(NamedTextColor.LIGHT_PURPLE);
        public static final Component GRAY = Component.text("").color(NamedTextColor.GRAY);
        public static final Component DARK_GRAY = Component.text("").color(NamedTextColor.DARK_GRAY);
        public static final Component WHITE = Component.text("").color(NamedTextColor.WHITE);
        public static final Component GOLD = Component.text("").color(NamedTextColor.GOLD);
        
        public static Component text(String text, NamedTextColor color) {
            return Component.text(text).color(color);
        }
        
        public static Component text(String text, TextColor color) {
            return Component.text(text).color(color);
        }
    }
    
    /**
     * Create formatted text components
     */
    public static class Format {
        
        public static Component success(String message) {
            return Component.text("✓ " + message).color(NamedTextColor.GREEN);
        }
        
        public static Component error(String message) {
            return Component.text("✗ " + message).color(NamedTextColor.RED);
        }
        
        public static Component warning(String message) {
            return Component.text("⚠ " + message).color(NamedTextColor.YELLOW);
        }
        
        public static Component info(String message) {
            return Component.text("ℹ " + message).color(NamedTextColor.AQUA);
        }
        
        public static Component prefix(String prefix, Component message) {
            return Component.text("[" + prefix + "] ")
                .color(NamedTextColor.GRAY)
                .append(message);
        }
        
        public static Component bold(String text) {
            return Component.text(text).decorate(TextDecoration.BOLD);
        }
        
        public static Component italic(String text) {
            return Component.text(text).decorate(TextDecoration.ITALIC);
        }
        
        public static Component underline(String text) {
            return Component.text(text).decorate(TextDecoration.UNDERLINED);
        }
    }
    
    /**
     * Common message formats for the plugin
     */
    public static class Messages {
        
        private static final Component PREFIX = Component.text("[")
            .color(NamedTextColor.GRAY)
            .append(Component.text("Kits").color(NamedTextColor.AQUA))
            .append(Component.text("] ").color(NamedTextColor.GRAY));
        
        public static Component kit(String message) {
            return PREFIX.append(fromLegacy(message));
        }
        
        public static Component kitSuccess(String message) {
            return PREFIX.append(Format.success(message));
        }
        
        public static Component kitError(String message) {
            return PREFIX.append(Format.error(message));
        }
        
        public static Component kitWarning(String message) {
            return PREFIX.append(Format.warning(message));
        }
        
        public static Component kitInfo(String message) {
            return PREFIX.append(Format.info(message));
        }
    }
    
    /**
     * Check if modern Adventure features are available
     */
    public static boolean hasAdventureSupport() {
        try {
            // Test if we can access Adventure API
            Component.text("test");
            return true;
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }
    
    /**
     * Legacy compatibility method - converts ChatColor to NamedTextColor
     */
    public static NamedTextColor fromChatColor(ChatColor chatColor) {
        switch (chatColor) {
            case BLACK: return NamedTextColor.BLACK;
            case DARK_BLUE: return NamedTextColor.DARK_BLUE;
            case DARK_GREEN: return NamedTextColor.DARK_GREEN;
            case DARK_AQUA: return NamedTextColor.DARK_AQUA;
            case DARK_RED: return NamedTextColor.DARK_RED;
            case DARK_PURPLE: return NamedTextColor.DARK_PURPLE;
            case GOLD: return NamedTextColor.GOLD;
            case GRAY: return NamedTextColor.GRAY;
            case DARK_GRAY: return NamedTextColor.DARK_GRAY;
            case BLUE: return NamedTextColor.BLUE;
            case GREEN: return NamedTextColor.GREEN;
            case AQUA: return NamedTextColor.AQUA;
            case RED: return NamedTextColor.RED;
            case LIGHT_PURPLE: return NamedTextColor.LIGHT_PURPLE;
            case YELLOW: return NamedTextColor.YELLOW;
            case WHITE: return NamedTextColor.WHITE;
            default: return NamedTextColor.WHITE;
        }
    }
}