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

import dev.noah.perplayerkit.gui2.data.DataContext;
import org.bukkit.inventory.ItemStack;

/**
 * A static component that displays a fixed item.
 * This is the simplest component type - just displays an item with configured properties.
 * 
 * Example configuration:
 * type: "static"
 * material: DIAMOND_SWORD
 * name: "&b&lWelcome!"
 * lore:
 *   - "&7Click me for something cool"
 *   - "&7Your name is: {player_name}"
 */
public class StaticComponent extends BaseComponent {
    
    public StaticComponent() {
        super("static");
    }
    
    @Override
    protected ItemStack renderSpecific(ItemStack baseItem, DataContext context) {
        // Static components don't need any special rendering beyond the base
        return baseItem;
    }
}