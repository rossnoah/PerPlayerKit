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
package dev.noah.perplayerkit.validation;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Comprehensive validation utilities for PerPlayerKit operations.
 * Provides fluent API for parameter validation with meaningful error messages.
 */
public final class Validator {
    
    private Validator() {
        // Utility class
    }
    
    /**
     * Validates that a value is not null.
     *
     * @param value the value to check
     * @param parameterName the name of the parameter for error messages
     * @param <T> the type of the value
     * @return the validated value
     * @throws IllegalArgumentException if the value is null
     */
    @NotNull
    public static <T> T requireNonNull(@Nullable T value, @NotNull String parameterName) {
        if (value == null) {
            throw new IllegalArgumentException(parameterName + " cannot be null");
        }
        return value;
    }
    
    /**
     * Validates that a string is not null or empty.
     *
     * @param value the string to check
     * @param parameterName the name of the parameter for error messages
     * @return the validated string
     * @throws IllegalArgumentException if the string is null or empty
     */
    @NotNull
    public static String requireNonEmpty(@Nullable String value, @NotNull String parameterName) {
        requireNonNull(value, parameterName);
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException(parameterName + " cannot be empty");
        }
        return value;
    }
    
    /**
     * Validates that a player is online and not null.
     *
     * @param player the player to check
     * @return the validated player
     * @throws IllegalArgumentException if the player is null or offline
     */
    @NotNull
    public static Player requireOnlinePlayer(@Nullable Player player) {
        requireNonNull(player, "player");
        if (!player.isOnline()) {
            throw new IllegalArgumentException("Player " + player.getName() + " is not online");
        }
        return player;
    }
    
    /**
     * Validates that a kit slot is within valid range (1-9).
     *
     * @param slot the slot number to check
     * @return the validated slot
     * @throws IllegalArgumentException if the slot is out of range
     */
    public static int requireValidKitSlot(int slot) {
        return requireInRange(slot, 1, 9, "kit slot");
    }
    
    /**
     * Validates that a value is within a specified range.
     *
     * @param value the value to check
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param parameterName the name of the parameter for error messages
     * @return the validated value
     * @throws IllegalArgumentException if the value is out of range
     */
    public static int requireInRange(int value, int min, int max, @NotNull String parameterName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(String.format(
                "%s must be between %d and %d, but was %d", 
                parameterName, min, max, value));
        }
        return value;
    }
    
    /**
     * Validates that a UUID is not null.
     *
     * @param uuid the UUID to check
     * @param parameterName the name of the parameter for error messages
     * @return the validated UUID
     * @throws IllegalArgumentException if the UUID is null
     */
    @NotNull
    public static UUID requireValidUuid(@Nullable UUID uuid, @NotNull String parameterName) {
        return requireNonNull(uuid, parameterName);
    }
    
    /**
     * Validates that an ItemStack array is not null and contains at least one non-null item.
     *
     * @param items the ItemStack array to check
     * @return the validated array
     * @throws IllegalArgumentException if the array is null or contains only nulls
     */
    @NotNull
    public static ItemStack[] requireNonEmptyKit(@Nullable ItemStack[] items) {
        requireNonNull(items, "kit items");
        
        boolean hasNonNullItem = false;
        for (ItemStack item : items) {
            if (item != null) {
                hasNonNullItem = true;
                break;
            }
        }
        
        if (!hasNonNullItem) {
            throw new IllegalArgumentException("Kit cannot be empty (must contain at least one item)");
        }
        
        return items;
    }
    
    /**
     * Validates a custom condition with a meaningful error message.
     *
     * @param condition the condition to check
     * @param errorMessage the error message if condition fails
     * @throws IllegalArgumentException if the condition is false
     */
    public static void require(boolean condition, @NotNull String errorMessage) {
        if (!condition) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
    
    /**
     * Validates a value against a custom predicate.
     *
     * @param value the value to check
     * @param predicate the predicate to test
     * @param errorMessage the error message if validation fails
     * @param <T> the type of the value
     * @return the validated value
     * @throws IllegalArgumentException if the predicate returns false
     */
    @NotNull
    public static <T> T requireThat(@Nullable T value, @NotNull Predicate<T> predicate, 
                                   @NotNull String errorMessage) {
        requireNonNull(value, "value");
        if (!predicate.test(value)) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }
    
    /**
     * Builder for complex validation chains.
     */
    public static final class ValidationBuilder<T> {
        private final T value;
        private final String parameterName;
        
        private ValidationBuilder(@Nullable T value, @NotNull String parameterName) {
            this.value = value;
            this.parameterName = parameterName;
        }
        
        /**
         * Validates that the value is not null.
         *
         * @return this builder for chaining
         * @throws IllegalArgumentException if the value is null
         */
        @NotNull
        public ValidationBuilder<T> notNull() {
            requireNonNull(value, parameterName);
            return this;
        }
        
        /**
         * Validates the value against a custom predicate.
         *
         * @param predicate the predicate to test
         * @param errorMessage the error message if validation fails
         * @return this builder for chaining
         * @throws IllegalArgumentException if the predicate returns false
         */
        @NotNull
        public ValidationBuilder<T> that(@NotNull Predicate<T> predicate, @NotNull String errorMessage) {
            requireThat(value, predicate, errorMessage);
            return this;
        }
        
        /**
         * Returns the validated value.
         *
         * @return the validated value
         */
        @NotNull
        public T get() {
            return Objects.requireNonNull(value);
        }
    }
    
    /**
     * Creates a validation builder for the specified value and parameter name.
     *
     * @param value the value to validate
     * @param parameterName the parameter name for error messages
     * @param <T> the type of the value
     * @return a new validation builder
     */
    @NotNull
    public static <T> ValidationBuilder<T> validate(@Nullable T value, @NotNull String parameterName) {
        return new ValidationBuilder<>(value, parameterName);
    }
}