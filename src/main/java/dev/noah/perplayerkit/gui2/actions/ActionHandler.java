/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions;

import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;

public abstract class ActionHandler {
    public abstract void execute(Player player, DataContext context, Object clickInfo);
    
    public static ActionHandler create(Map<?, ?> actionData) {
        String type = actionData.get("type").toString();
        Map<String, Object> params = new HashMap<>();
        
        for (Map.Entry<?, ?> entry : actionData.entrySet()) {
            if (!"type".equals(entry.getKey().toString())) {
                params.put(entry.getKey().toString(), entry.getValue());
            }
        }
        
        return dev.noah.perplayerkit.PerPlayerKit.guiManager.getActionRegistry().createAction(type, params);
    }
    
    public static ActionHandler createFromString(String actionString) {
        // Parse simple action strings like "load_kit:1" or "open_gui:main-menu"
        String[] parts = actionString.split(":", 2);
        String type = parts[0];
        
        Map<String, Object> params = new HashMap<>();
        if (parts.length > 1) {
            // Simple parameter parsing - could be enhanced
            String paramValue = parts[1];
            switch (type.toLowerCase()) {
                case "load_kit":
                    try {
                        params.put("slot", Integer.parseInt(paramValue));
                    } catch (NumberFormatException e) {
                        params.put("slot", paramValue); // Let it resolve as placeholder
                    }
                    break;
                case "open_gui":
                    params.put("gui", paramValue);
                    break;
                case "message":
                    params.put("text", paramValue);
                    break;
                case "play_sound":
                    params.put("sound", paramValue);
                    break;
                default:
                    params.put("value", paramValue);
                    break;
            }
        }
        
        return dev.noah.perplayerkit.PerPlayerKit.guiManager.getActionRegistry().createAction(type, params);
    }
}