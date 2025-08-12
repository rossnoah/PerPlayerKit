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
 * Exception thrown when kit-related operations fail.
 */
public class KitException extends PerPlayerKitException {
    
    public static final String ERROR_CODE_PREFIX = "KIT_";
    
    /**
     * Constructs a kit exception with the specified message and slot.
     *
     * @param message the detail message
     * @param playerId the player ID
     * @param slot the kit slot number
     */
    public KitException(@NotNull String message, @Nullable UUID playerId, int slot) {
        super(message, ERROR_CODE_PREFIX + "OPERATION_FAILED", playerId, "slot:" + slot);
    }
    
    /**
     * Constructs a kit exception with the specified message, cause, and slot.
     *
     * @param message the detail message
     * @param cause the underlying cause
     * @param playerId the player ID
     * @param slot the kit slot number
     */
    public KitException(@NotNull String message, @Nullable Throwable cause, @Nullable UUID playerId, int slot) {
        super(message, cause, ERROR_CODE_PREFIX + "OPERATION_FAILED", playerId, "slot:" + slot);
    }
    
    /**
     * Exception for invalid kit slots.
     */
    public static class InvalidSlotException extends KitException {
        public InvalidSlotException(int slot, int minSlot, int maxSlot) {
            super(String.format("Invalid kit slot: %d (must be between %d and %d)", slot, minSlot, maxSlot), 
                  null, slot);
        }
        
        public InvalidSlotException(@NotNull String message, @Nullable UUID playerId, int slot) {
            super(message, playerId, slot);
        }
    }
    
    /**
     * Exception for empty kits when non-empty is required.
     */
    public static class EmptyKitException extends KitException {
        public EmptyKitException(@Nullable UUID playerId, int slot) {
            super("Cannot save empty kit", playerId, slot);
        }
    }
    
    /**
     * Exception for kit not found operations.
     */
    public static class KitNotFoundException extends KitException {
        public KitNotFoundException(@Nullable UUID playerId, int slot) {
            super("Kit not found", playerId, slot);
        }
        
        public KitNotFoundException(@NotNull String kitId) {
            super("Kit not found: " + kitId, null, 0);
        }
    }
    
    /**
     * Exception for kit serialization/deserialization failures.
     */
    public static class KitSerializationException extends KitException {
        public KitSerializationException(@NotNull String message, @Nullable Throwable cause, 
                                       @Nullable UUID playerId, int slot) {
            super("Kit serialization failed: " + message, cause, playerId, slot);
        }
    }
}