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
package dev.noah.perplayerkit.gui2.core;

import dev.noah.perplayerkit.gui2.components.BaseComponent;
import dev.noah.perplayerkit.gui2.data.DataContext;
import dev.noah.perplayerkit.gui2.themes.ComponentStyle;
import dev.noah.perplayerkit.gui2.themes.Theme;
import net.md_5.bungee.api.ChatColor;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;

import java.util.HashMap;
import java.util.Map;

/**
 * Renders GUI configurations into actual Minecraft menus.
 * Handles theme application, component positioning, and menu creation.
 */
public class GuiRenderer {
    
    private final GuiManager guiManager;
    private final Map<String, Menu> activeMenus = new HashMap<>();
    
    public GuiRenderer(GuiManager guiManager) {
        this.guiManager = guiManager;
    }
    
    /**
     * Render a GUI configuration into a Menu
     */
    public Menu render(GuiConfig config, DataContext context) {
        try {
            // Create the base menu
            Menu menu = createBaseMenu(config, context);
            
            // Get theme for styling
            Theme theme = guiManager.getThemeManager().getTheme(config.getTheme());
            
            // Apply templates first
            applyTemplates(menu, config, context, theme);
            
            // Render all components
            renderComponents(menu, config, context, theme);
            
            // Apply theme-specific menu settings
            applyThemeSettings(menu, config, theme);
            
            // Store for potential updates
            activeMenus.put(generateMenuKey(config, context), menu);
            
            return menu;
            
        } catch (Exception e) {
            guiManager.getPlugin().getLogger().severe("Error rendering GUI '" + config.getName() + "': " + e.getMessage());
            e.printStackTrace();
            return createErrorMenu(config.getName());
        }
    }
    
    /**
     * Create the base menu structure
     */
    private Menu createBaseMenu(GuiConfig config, DataContext context) {
        String resolvedTitle = context.resolve(config.getTitle());
        resolvedTitle = ChatColor.translateAlternateColorCodes('&', resolvedTitle);
        
        Menu menu = ChestMenu.builder(config.getSize() / 9)
                .title(resolvedTitle)
                .redraw(true)
                .build();
        
        // Apply basic settings
        Object closeOnClickOutside = config.getSetting("close_on_click_outside", true);
        if (closeOnClickOutside instanceof Boolean && (Boolean) closeOnClickOutside) {
            // Menu already handles this by default
        }
        
        Object allowItemDropping = config.getSetting("allow_item_dropping", false);
        if (allowItemDropping instanceof Boolean && (Boolean) allowItemDropping) {
            menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        }
        
        return menu;
    }
    
    /**
     * Apply template components to the menu
     */
    private void applyTemplates(Menu menu, GuiConfig config, DataContext context, Theme theme) {
        for (String templateName : config.getTemplates()) {
            try {
                var template = guiManager.getTemplateManager().getTemplate(templateName);
                if (template != null) {
                    template.apply(menu, config, context, theme);
                }
            } catch (Exception e) {
                guiManager.getPlugin().getLogger().warning("Error applying template '" + templateName + "': " + e.getMessage());
            }
        }
    }
    
    /**
     * Render all components to their slots
     */
    private void renderComponents(Menu menu, GuiConfig config, DataContext context, Theme theme) {
        // Render components mapped to specific slots
        for (Map.Entry<Integer, String> mapping : config.getSlotMappings().entrySet()) {
            int slot = mapping.getKey();
            String componentName = mapping.getValue();
            
            if (slot < 0 || slot >= config.getSize()) {
                continue; // Skip invalid slots
            }
            
            BaseComponent component = config.getComponents().get(componentName);
            if (component == null) {
                guiManager.getPlugin().getLogger().warning("Component '" + componentName + "' not found for slot " + slot);
                continue;
            }
            
            try {
                // Create slot-specific context
                DataContext slotContext = context.createChild();
                slotContext.set("slot", slot);
                slotContext.set("slot_index", getSlotIndex(slot, config.getSize()));
                slotContext.set("component_name", componentName);
                
                // Get component style from theme
                ComponentStyle style = theme != null ? theme.getComponentStyle(component.getType()) : null;
                
                // Apply component to slot
                component.applyToSlot(menu.getSlot(slot), slotContext, style);
                
            } catch (Exception e) {
                guiManager.getPlugin().getLogger().warning("Error rendering component '" + componentName + "' to slot " + slot + ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Apply theme-specific menu settings
     */
    private void applyThemeSettings(Menu menu, GuiConfig config, Theme theme) {
        if (theme == null) return;
        
        // Apply theme-specific cursor settings, sounds, etc.
        // Implementation would depend on theme capabilities
    }
    
    /**
     * Update a rendered menu with new data
     */
    public void updateMenu(String menuKey, DataContext newContext) {
        Menu menu = activeMenus.get(menuKey);
        if (menu == null) return;
        
        // Implementation would update dynamic components
        // This is complex and would require tracking which components need updates
    }
    
    /**
     * Remove a menu from active tracking
     */
    public void removeMenu(String menuKey) {
        activeMenus.remove(menuKey);
    }
    
    /**
     * Create an error menu when rendering fails
     */
    private Menu createErrorMenu(String guiName) {
        Menu errorMenu = ChestMenu.builder(1)
                .title(ChatColor.RED + "Error: " + guiName)
                .build();
        
        errorMenu.getSlot(4).setItem(new org.bukkit.inventory.ItemStack(org.bukkit.Material.BARRIER));
        
        return errorMenu;
    }
    
    /**
     * Generate a unique key for menu tracking
     */
    private String generateMenuKey(GuiConfig config, DataContext context) {
        return config.getName() + "_" + 
               (context.getPlayer() != null ? context.getPlayer().getUniqueId().toString() : "null") + "_" +
               System.currentTimeMillis();
    }
    
    /**
     * Calculate slot index for grid-based layouts
     */
    private int getSlotIndex(int slot, int guiSize) {
        // For 9-wide inventories, calculate row and column
        int row = slot / 9;
        int col = slot % 9;
        return col + 1; // 1-indexed for user-friendly numbers
    }
    
    /**
     * Get active menu count for monitoring
     */
    public int getActiveMenuCount() {
        return activeMenus.size();
    }
    
    /**
     * Clear all tracked menus (cleanup)
     */
    public void clearAllMenus() {
        activeMenus.clear();
    }
}