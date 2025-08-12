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
package dev.noah.perplayerkit.gui2.data;

import dev.noah.perplayerkit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Evaluates conditions for conditional components and other logic.
 * Supports a wide variety of condition types for maximum flexibility.
 * 
 * Supported condition types:
 * - has_permission: Check if player has permission
 * - has_kit: Check if player has a kit in specific slot
 * - has_enderchest: Check if player has enderchest in specific slot
 * - player_online: Check if a player is online
 * - equals: Compare two values for equality
 * - not_equals: Compare two values for inequality
 * - greater_than: Numeric comparison
 * - less_than: Numeric comparison
 * - contains: Check if string contains substring
 * - starts_with: Check if string starts with prefix
 * - ends_with: Check if string ends with suffix
 * - empty: Check if value is empty/null
 * - not_empty: Check if value is not empty/null
 * - in_world: Check if player is in specific world
 * - in_gamemode: Check if player is in specific gamemode
 * - time_range: Check if current time is in range
 */
public class ConditionEvaluator {
    
    private static final Map<String, BiFunction<Map<String, Object>, DataContext, Boolean>> CONDITIONS = new HashMap<>();
    
    static {
        registerBuiltinConditions();
    }
    
    /**
     * Evaluate a condition with the given parameters and context
     */
    public static boolean evaluate(String conditionType, Map<String, Object> parameters, DataContext context) {
        BiFunction<Map<String, Object>, DataContext, Boolean> evaluator = CONDITIONS.get(conditionType.toLowerCase());
        
        if (evaluator == null) {
            return false; // Unknown condition type defaults to false
        }
        
        try {
            // Resolve placeholders in parameters
            Map<String, Object> resolvedParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    value = context.resolve((String) value);
                }
                resolvedParams.put(entry.getKey(), value);
            }
            
            return evaluator.apply(resolvedParams, context);
            
        } catch (Exception e) {
            return false; // Conditions should never throw exceptions
        }
    }
    
    /**
     * Register built-in condition types
     */
    private static void registerBuiltinConditions() {
        
        // Permission check: {type: "has_permission", permission: "perplayerkit.admin"}
        CONDITIONS.put("has_permission", (params, context) -> {
            String permission = getStringParam(params, "permission");
            if (permission == null || context.getPlayer() == null) return false;
            
            return context.getPlayer().hasPermission(permission);
        });
        
        // Kit existence: {type: "has_kit", slot: 1}
        CONDITIONS.put("has_kit", (params, context) -> {
            int slot = getIntParam(params, "slot", -1);
            if (slot < 1 || slot > 9 || context.getPlayer() == null) return false;
            
            return KitManager.get().hasKit(context.getPlayer().getUniqueId(), slot);
        });
        
        // Enderchest existence: {type: "has_enderchest", slot: 1}
        CONDITIONS.put("has_enderchest", (params, context) -> {
            int slot = getIntParam(params, "slot", -1);
            if (slot < 1 || slot > 9 || context.getPlayer() == null) return false;
            
            return KitManager.get().hasEC(context.getPlayer().getUniqueId(), slot);
        });
        
        // Player online check: {type: "player_online", player: "PlayerName"}
        CONDITIONS.put("player_online", (params, context) -> {
            String playerName = getStringParam(params, "player");
            if (playerName == null) return false;
            
            Player target = Bukkit.getPlayer(playerName);
            return target != null && target.isOnline();
        });
        
        // Equality check: {type: "equals", value1: "a", value2: "b"}
        CONDITIONS.put("equals", (params, context) -> {
            Object value1 = params.get("value1");
            Object value2 = params.get("value2");
            
            if (value1 == null && value2 == null) return true;
            if (value1 == null || value2 == null) return false;
            
            return value1.toString().equals(value2.toString());
        });
        
        // Inequality check: {type: "not_equals", value1: "a", value2: "b"}
        CONDITIONS.put("not_equals", (params, context) -> {
            return !CONDITIONS.get("equals").apply(params, context);
        });
        
        // Numeric comparisons
        CONDITIONS.put("greater_than", (params, context) -> {
            double value1 = getDoubleParam(params, "value1", 0);
            double value2 = getDoubleParam(params, "value2", 0);
            return value1 > value2;
        });
        
        CONDITIONS.put("less_than", (params, context) -> {
            double value1 = getDoubleParam(params, "value1", 0);
            double value2 = getDoubleParam(params, "value2", 0);
            return value1 < value2;
        });
        
        CONDITIONS.put("greater_or_equal", (params, context) -> {
            double value1 = getDoubleParam(params, "value1", 0);
            double value2 = getDoubleParam(params, "value2", 0);
            return value1 >= value2;
        });
        
        CONDITIONS.put("less_or_equal", (params, context) -> {
            double value1 = getDoubleParam(params, "value1", 0);
            double value2 = getDoubleParam(params, "value2", 0);
            return value1 <= value2;
        });
        
        // String checks
        CONDITIONS.put("contains", (params, context) -> {
            String text = getStringParam(params, "text");
            String substring = getStringParam(params, "substring");
            
            if (text == null || substring == null) return false;
            return text.toLowerCase().contains(substring.toLowerCase());
        });
        
        CONDITIONS.put("starts_with", (params, context) -> {
            String text = getStringParam(params, "text");
            String prefix = getStringParam(params, "prefix");
            
            if (text == null || prefix == null) return false;
            return text.toLowerCase().startsWith(prefix.toLowerCase());
        });
        
        CONDITIONS.put("ends_with", (params, context) -> {
            String text = getStringParam(params, "text");
            String suffix = getStringParam(params, "suffix");
            
            if (text == null || suffix == null) return false;
            return text.toLowerCase().endsWith(suffix.toLowerCase());
        });
        
        // Empty/null checks
        CONDITIONS.put("empty", (params, context) -> {
            Object value = params.get("value");
            if (value == null) return true;
            
            String str = value.toString().trim();
            return str.isEmpty() || str.equals("null");
        });
        
        CONDITIONS.put("not_empty", (params, context) -> {
            return !CONDITIONS.get("empty").apply(params, context);
        });
        
        // World check: {type: "in_world", world: "world_nether"}
        CONDITIONS.put("in_world", (params, context) -> {
            String worldName = getStringParam(params, "world");
            if (worldName == null || context.getPlayer() == null) return false;
            
            return context.getPlayer().getWorld().getName().equalsIgnoreCase(worldName);
        });
        
        // Gamemode check: {type: "in_gamemode", gamemode: "CREATIVE"}
        CONDITIONS.put("in_gamemode", (params, context) -> {
            String gamemode = getStringParam(params, "gamemode");
            if (gamemode == null || context.getPlayer() == null) return false;
            
            return context.getPlayer().getGameMode().toString().equalsIgnoreCase(gamemode);
        });
        
        // Time range check: {type: "time_range", start: 6000, end: 18000}
        CONDITIONS.put("time_range", (params, context) -> {
            long start = getLongParam(params, "start", 0);
            long end = getLongParam(params, "end", 24000);
            
            if (context.getPlayer() == null) return false;
            
            long currentTime = context.getPlayer().getWorld().getTime();
            
            if (start <= end) {
                return currentTime >= start && currentTime <= end;
            } else {
                // Range crosses midnight
                return currentTime >= start || currentTime <= end;
            }
        });
        
        // Health checks
        CONDITIONS.put("health_above", (params, context) -> {
            double threshold = getDoubleParam(params, "threshold", 0);
            if (context.getPlayer() == null) return false;
            
            return context.getPlayer().getHealth() > threshold;
        });
        
        CONDITIONS.put("health_below", (params, context) -> {
            double threshold = getDoubleParam(params, "threshold", 20);
            if (context.getPlayer() == null) return false;
            
            return context.getPlayer().getHealth() < threshold;
        });
        
        // Level checks
        CONDITIONS.put("level_above", (params, context) -> {
            int threshold = getIntParam(params, "threshold", 0);
            if (context.getPlayer() == null) return false;
            
            return context.getPlayer().getLevel() > threshold;
        });
        
        CONDITIONS.put("level_below", (params, context) -> {
            int threshold = getIntParam(params, "threshold", 100);
            if (context.getPlayer() == null) return false;
            
            return context.getPlayer().getLevel() < threshold;
        });
        
        // Data context value checks
        CONDITIONS.put("context_equals", (params, context) -> {
            String key = getStringParam(params, "key");
            String expected = getStringParam(params, "value");
            
            if (key == null || expected == null) return false;
            
            Object actual = context.get(key);
            return actual != null && actual.toString().equals(expected);
        });
        
        CONDITIONS.put("context_exists", (params, context) -> {
            String key = getStringParam(params, "key");
            if (key == null) return false;
            
            return context.has(key);
        });
        
        // Boolean logic conditions
        CONDITIONS.put("and", (params, context) -> {
            // All sub-conditions must be true
            return true; // Implementation would handle nested conditions
        });
        
        CONDITIONS.put("or", (params, context) -> {
            // At least one sub-condition must be true
            return false; // Implementation would handle nested conditions
        });
        
        CONDITIONS.put("not", (params, context) -> {
            // Negate the sub-condition
            return false; // Implementation would handle nested conditions
        });
    }
    
    // Helper methods for parameter extraction
    
    private static String getStringParam(Map<String, Object> params, String key) {
        Object value = params.get(key);
        return value != null ? value.toString() : null;
    }
    
    private static int getIntParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value != null) {
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
    
    private static long getLongParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value != null) {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
    
    private static double getDoubleParam(Map<String, Object> params, String key, double defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value != null) {
            try {
                return Double.parseDouble(value.toString());
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        return defaultValue;
    }
    
    private static boolean getBooleanParam(Map<String, Object> params, String key, boolean defaultValue) {
        Object value = params.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            String str = value.toString().toLowerCase();
            if ("true".equals(str) || "yes".equals(str) || "1".equals(str)) {
                return true;
            }
            if ("false".equals(str) || "no".equals(str) || "0".equals(str)) {
                return false;
            }
        }
        return defaultValue;
    }
    
    /**
     * Register a custom condition evaluator
     */
    public static void registerCondition(String type, BiFunction<Map<String, Object>, DataContext, Boolean> evaluator) {
        CONDITIONS.put(type.toLowerCase(), evaluator);
    }
    
    /**
     * Unregister a condition evaluator
     */
    public static void unregisterCondition(String type) {
        CONDITIONS.remove(type.toLowerCase());
    }
    
    /**
     * Check if a condition type is registered
     */
    public static boolean hasCondition(String type) {
        return CONDITIONS.containsKey(type.toLowerCase());
    }
    
    /**
     * Get all registered condition types
     */
    public static String[] getRegisteredConditions() {
        return CONDITIONS.keySet().toArray(new String[0]);
    }
}