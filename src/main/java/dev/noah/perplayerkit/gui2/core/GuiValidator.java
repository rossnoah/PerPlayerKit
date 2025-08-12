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

import java.util.ArrayList;
import java.util.List;

/**
 * Validates GUI configurations for correctness and completeness.
 */
public class GuiValidator {
    
    public boolean validate(GuiConfig config) {
        List<String> errors = new ArrayList<>();
        
        // Basic validation
        if (config.getSize() % 9 != 0 || config.getSize() < 9 || config.getSize() > 54) {
            errors.add("Invalid GUI size: " + config.getSize());
        }
        
        if (config.getTitle() == null || config.getTitle().isEmpty()) {
            errors.add("GUI title cannot be empty");
        }
        
        // Validate slot mappings
        for (Integer slot : config.getSlotMappings().keySet()) {
            if (slot < 0 || slot >= config.getSize()) {
                errors.add("Slot " + slot + " is outside GUI bounds");
            }
        }
        
        // Validate components exist
        for (String componentName : config.getSlotMappings().values()) {
            if (!config.getComponents().containsKey(componentName)) {
                errors.add("Component '" + componentName + "' is referenced but not defined");
            }
        }
        
        // Log errors if any
        if (!errors.isEmpty()) {
            // Would log to plugin logger
            return false;
        }
        
        return true;
    }
}