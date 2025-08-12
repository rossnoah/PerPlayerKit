/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions;

import dev.noah.perplayerkit.gui2.actions.actions.*;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ActionRegistry {
    private final Plugin plugin;
    private final Map<String, Function<Map<String, Object>, ActionHandler>> actionFactories = new HashMap<>();
    
    public ActionRegistry(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public void registerDefaultActions() {
        // Kit management actions
        register("load_kit", params -> {
            int slot = getInt(params, "slot", -1);
            return new LoadKitAction(slot);
        });
        
        // GUI navigation actions
        register("open_gui", params -> {
            String guiName = getString(params, "gui", "main-menu");
            return new OpenGuiAction(guiName);
        });
        
        register("close_menu", params -> new CloseMenuAction());
        
        // Communication actions
        register("message", params -> {
            String message = getString(params, "text", "");
            return new MessageAction(message);
        });
        
        register("broadcast", params -> {
            String message = getString(params, "message", "");
            return new BroadcastAction(message);
        });
        
        // Audio actions
        register("play_sound", params -> {
            String sound = getString(params, "sound", "UI_BUTTON_CLICK");
            float volume = getFloat(params, "volume", 1.0f);
            float pitch = getFloat(params, "pitch", 1.0f);
            return new PlaySoundAction(sound, volume, pitch);
        });
    }
    
    public void register(String type, Function<Map<String, Object>, ActionHandler> factory) {
        actionFactories.put(type.toLowerCase(), factory);
    }
    
    public ActionHandler createAction(String type, Map<String, Object> parameters) {
        Function<Map<String, Object>, ActionHandler> factory = actionFactories.get(type.toLowerCase());
        if (factory == null) {
            plugin.getLogger().warning("Unknown action type: " + type);
            return null;
        }
        
        try {
            return factory.apply(parameters);
        } catch (Exception e) {
            plugin.getLogger().warning("Error creating action '" + type + "': " + e.getMessage());
            return null;
        }
    }
    
    // Helper methods for parameter extraction
    private String getString(Map<String, Object> params, String key, String defaultValue) {
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private float getFloat(Map<String, Object> params, String key, float defaultValue) {
        Object value = params.get(key);
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}