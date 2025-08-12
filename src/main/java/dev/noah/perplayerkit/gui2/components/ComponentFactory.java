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
package dev.noah.perplayerkit.gui2.components;

import dev.noah.perplayerkit.gui2.core.GuiManager;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Factory for creating GUI components from configuration data.
 * Maintains a registry of component types and their constructors.
 */
public class ComponentFactory {
    
    private static final Map<String, Function<GuiManager, BaseComponent>> componentRegistry = new HashMap<>();
    
    static {
        // Register built-in component types
        registerComponent("static", manager -> new StaticComponent());
        registerComponent("kit_slot", manager -> new KitSlotComponent(manager));
        registerComponent("enderchest_slot", manager -> new EnderchestSlotComponent(manager));
        registerComponent("player_head", manager -> new PlayerHeadComponent());
        registerComponent("conditional", manager -> new ConditionalComponent());
        registerComponent("template", manager -> new TemplateComponent(manager));
        registerComponent("action_button", manager -> new ActionButtonComponent());
        registerComponent("dynamic_item", manager -> new DynamicItemComponent());
        registerComponent("progress_bar", manager -> new ProgressBarComponent());
        registerComponent("list", manager -> new ListComponent(manager));
        registerComponent("border", manager -> new BorderComponent());
    }
    
    /**
     * Create a component from configuration data
     */
    public static BaseComponent createComponent(ConfigurationSection config, GuiManager guiManager) {
        String type = config.getString("type", "static");
        
        Function<GuiManager, BaseComponent> constructor = componentRegistry.get(type.toLowerCase());
        if (constructor == null) {
            guiManager.getPlugin().getLogger().warning("Unknown component type: " + type);
            return null;
        }
        
        try {
            BaseComponent component = constructor.apply(guiManager);
            component.configure(config);
            return component;
        } catch (Exception e) {
            guiManager.getPlugin().getLogger().severe("Error creating component of type '" + type + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Register a new component type
     */
    public static void registerComponent(String type, Function<GuiManager, BaseComponent> constructor) {
        componentRegistry.put(type.toLowerCase(), constructor);
    }
    
    /**
     * Check if a component type is registered
     */
    public static boolean isRegistered(String type) {
        return componentRegistry.containsKey(type.toLowerCase());
    }
    
    /**
     * Get all registered component types
     */
    public static String[] getRegisteredTypes() {
        return componentRegistry.keySet().toArray(new String[0]);
    }
    
    /**
     * Unregister a component type (for plugins that want to override built-ins)
     */
    public static void unregisterComponent(String type) {
        componentRegistry.remove(type.toLowerCase());
    }
}