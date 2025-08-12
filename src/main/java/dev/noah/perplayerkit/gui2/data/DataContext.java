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

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Context object that holds all data available for placeholder resolution and condition evaluation.
 * Contains player information, custom variables, and computed values.
 */
public class DataContext {
    
    private final Map<String, Object> data = new ConcurrentHashMap<>();
    private Player player;
    private DataContext parent;
    
    public DataContext() {
        // Empty context
    }
    
    public DataContext(Player player) {
        this.player = player;
        populatePlayerData();
    }
    
    public DataContext(DataContext parent) {
        this.parent = parent;
        this.player = parent.player;
    }
    
    /**
     * Create a child context that inherits from this one
     */
    public DataContext createChild() {
        return new DataContext(this);
    }
    
    /**
     * Set a data value
     */
    public void set(String key, Object value) {
        data.put(key, value);
    }
    
    /**
     * Get a data value, checking parent contexts if not found
     */
    public Object get(String key) {
        if (data.containsKey(key)) {
            return data.get(key);
        }
        
        if (parent != null) {
            return parent.get(key);
        }
        
        return null;
    }
    
    /**
     * Get a data value with a default
     */
    public Object get(String key, Object defaultValue) {
        Object value = get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * Get a string value
     */
    public String getString(String key) {
        Object value = get(key);
        return value != null ? value.toString() : "";
    }
    
    /**
     * Get a string value with default
     */
    public String getString(String key, String defaultValue) {
        Object value = get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    /**
     * Get an integer value
     */
    public int getInt(String key) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get an integer value with default
     */
    public int getInt(String key, int defaultValue) {
        Object value = get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    /**
     * Get a boolean value
     */
    public boolean getBoolean(String key) {
        Object value = get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * Get a boolean value with default
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    /**
     * Check if a key exists
     */
    public boolean has(String key) {
        return data.containsKey(key) || (parent != null && parent.has(key));
    }
    
    /**
     * Resolve placeholders in a string using this context
     */
    public String resolve(String text) {
        return PlaceholderResolver.resolve(text, this);
    }
    
    /**
     * Get the player associated with this context
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Set the player for this context
     */
    public void setPlayer(Player player) {
        this.player = player;
        populatePlayerData();
    }
    
    /**
     * Get all data as a map (for debugging)
     */
    public Map<String, Object> getAllData() {
        Map<String, Object> allData = new HashMap<>();
        
        // Add parent data first
        if (parent != null) {
            allData.putAll(parent.getAllData());
        }
        
        // Add our data (overriding parent)
        allData.putAll(data);
        
        return allData;
    }
    
    /**
     * Populate basic player data
     */
    private void populatePlayerData() {
        if (player == null) {
            return;
        }
        
        // Basic player info
        set("player_name", player.getName());
        set("player_display_name", player.getDisplayName());
        set("player_uuid", player.getUniqueId().toString());
        set("player_world", player.getWorld().getName());
        set("player_level", player.getLevel());
        set("player_health", (int) player.getHealth());
        set("player_max_health", (int) player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
        set("player_food_level", player.getFoodLevel());
        set("player_gamemode", player.getGameMode().toString());
        
        // Location info
        set("player_x", player.getLocation().getBlockX());
        set("player_y", player.getLocation().getBlockY());
        set("player_z", player.getLocation().getBlockZ());
        
        // Time info
        set("current_time", System.currentTimeMillis());
        set("server_time", player.getWorld().getTime());
        
        // Permission checks will be handled by condition evaluator
        // Kit information will be added by specific components
    }
    
    /**
     * Merge another context into this one
     */
    public void merge(DataContext other) {
        if (other != null) {
            data.putAll(other.data);
        }
    }
    
    /**
     * Create a context with initial data
     */
    public static DataContext create(Player player, Map<String, Object> initialData) {
        DataContext context = new DataContext(player);
        if (initialData != null) {
            context.data.putAll(initialData);
        }
        return context;
    }
    
    @Override
    public String toString() {
        return "DataContext{" +
                "player=" + (player != null ? player.getName() : "null") +
                ", data=" + data.size() + " entries" +
                ", hasParent=" + (parent != null) +
                '}';
    }
}