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
package dev.noah.perplayerkit.exceptions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Exception thrown when GUI-related operations fail.
 */
public class GuiException extends PerPlayerKitException {
    
    public static final String ERROR_CODE_PREFIX = "GUI_";
    
    /**
     * Constructs a GUI exception with the specified message and GUI name.
     *
     * @param message the detail message
     * @param playerId the player ID
     * @param guiName the GUI name
     */
    public GuiException(@NotNull String message, @Nullable UUID playerId, @Nullable String guiName) {
        super(message, ERROR_CODE_PREFIX + "OPERATION_FAILED", playerId, "gui:" + guiName);
    }
    
    /**
     * Constructs a GUI exception with the specified message, cause, and GUI name.
     *
     * @param message the detail message
     * @param cause the underlying cause
     * @param playerId the player ID
     * @param guiName the GUI name
     */
    public GuiException(@NotNull String message, @Nullable Throwable cause, 
                       @Nullable UUID playerId, @Nullable String guiName) {
        super(message, cause, ERROR_CODE_PREFIX + "OPERATION_FAILED", playerId, "gui:" + guiName);
    }
    
    /**
     * Exception for GUI configuration errors.
     */
    public static class ConfigurationException extends GuiException {
        public ConfigurationException(@NotNull String message, @Nullable String guiName) {
            super("GUI configuration error: " + message, null, guiName);
        }
        
        public ConfigurationException(@NotNull String message, @Nullable Throwable cause, 
                                    @Nullable String guiName) {
            super("GUI configuration error: " + message, cause, null, guiName);
        }
    }
    
    /**
     * Exception for GUI not found operations.
     */
    public static class GuiNotFoundException extends GuiException {
        public GuiNotFoundException(@NotNull String guiName) {
            super("GUI not found: " + guiName, null, guiName);
        }
    }
    
    /**
     * Exception for GUI rendering failures.
     */
    public static class RenderingException extends GuiException {
        public RenderingException(@NotNull String message, @Nullable UUID playerId, 
                                 @Nullable String guiName) {
            super("GUI rendering failed: " + message, playerId, guiName);
        }
        
        public RenderingException(@NotNull String message, @Nullable Throwable cause, 
                                 @Nullable UUID playerId, @Nullable String guiName) {
            super("GUI rendering failed: " + message, cause, playerId, guiName);
        }
    }
}