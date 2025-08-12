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
package dev.noah.perplayerkit.api.kit;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Metadata information about a kit without loading the full kit contents.
 * 
 * This interface provides lightweight access to kit information for
 * performance-sensitive operations like GUI displays.
 * 
 * @since 2.0.0
 */
public interface KitMetadata {
    
    /**
     * Gets the player UUID this kit belongs to.
     * 
     * @return the player UUID
     */
    @NotNull
    UUID getPlayerId();
    
    /**
     * Gets the kit slot number.
     * 
     * @return the slot number (1-9)
     */
    int getSlot();
    
    /**
     * Gets the timestamp when this kit was created.
     * 
     * @return creation timestamp in milliseconds
     */
    long getCreatedTime();
    
    /**
     * Gets the timestamp when this kit was last modified.
     * 
     * @return last modified timestamp in milliseconds
     */
    long getLastModified();
    
    /**
     * Gets the total number of items in this kit (excluding null/air).
     * 
     * @return item count
     */
    int getItemCount();
    
    /**
     * Gets the total number of inventory slots used (including armor).
     * 
     * @return slot count
     */
    int getSlotCount();
    
    /**
     * Gets a preview of the main items in this kit (first few non-null items).
     * 
     * @return list of preview materials
     */
    @NotNull
    List<Material> getPreviewItems();
    
    /**
     * Gets the primary material that represents this kit (for GUI icons).
     * 
     * @return the representative material
     */
    @NotNull
    Material getRepresentativeMaterial();
    
    /**
     * Checks if this kit contains armor items.
     * 
     * @return true if kit has armor
     */
    boolean hasArmor();
    
    /**
     * Checks if this kit contains weapons.
     * 
     * @return true if kit has weapons
     */
    boolean hasWeapons();
    
    /**
     * Checks if this kit contains tools.
     * 
     * @return true if kit has tools
     */
    boolean hasTools();
    
    /**
     * Checks if this kit contains food items.
     * 
     * @return true if kit has food
     */
    boolean hasFood();
    
    /**
     * Checks if this kit contains potions.
     * 
     * @return true if kit has potions
     */
    boolean hasPotions();
    
    /**
     * Gets a short description of this kit's contents.
     * 
     * @return kit description
     */
    @NotNull
    String getDescription();
    
    /**
     * Gets the custom name for this kit, if any.
     * 
     * @return custom name, or empty if none
     */
    @NotNull
    Optional<String> getCustomName();
    
    /**
     * Gets custom tags associated with this kit.
     * 
     * @return list of tags
     */
    @NotNull
    List<String> getTags();
    
    /**
     * Checks if this kit has a specific tag.
     * 
     * @param tag the tag to check
     * @return true if kit has the tag
     */
    boolean hasTag(@NotNull String tag);
    
    /**
     * Gets the kit's health requirement (minimum health to use).
     * 
     * @return minimum health, or empty if no requirement
     */
    @NotNull
    Optional<Double> getHealthRequirement();
    
    /**
     * Gets the kit's experience cost.
     * 
     * @return experience cost, or empty if no cost
     */
    @NotNull
    Optional<Integer> getExperienceCost();
    
    /**
     * Gets the kit's cooldown period in milliseconds.
     * 
     * @return cooldown period, or empty if no cooldown
     */
    @NotNull
    Optional<Long> getCooldownPeriod();
    
    /**
     * Checks if this kit is marked as favorite by the player.
     * 
     * @return true if favorite
     */
    boolean isFavorite();
    
    /**
     * Checks if this kit is locked (cannot be modified).
     * 
     * @return true if locked
     */
    boolean isLocked();
    
    /**
     * Gets the number of times this kit has been used.
     * 
     * @return usage count
     */
    int getUsageCount();
    
    /**
     * Gets the last time this kit was used.
     * 
     * @return last usage timestamp, or empty if never used
     */
    @NotNull
    Optional<Long> getLastUsed();
    
    /**
     * Estimates the total value of this kit in experience points.
     * 
     * @return estimated experience value
     */
    int getEstimatedValue();
    
    /**
     * Gets the kit's data version (for migration purposes).
     * 
     * @return data version
     */
    int getDataVersion();
    
    /**
     * Creates a builder for modifying kit metadata.
     * 
     * @return metadata builder
     */
    @NotNull
    KitMetadataBuilder toBuilder();
    
    /**
     * Builder for creating or modifying kit metadata.
     */
    interface KitMetadataBuilder {
        
        /**
         * Sets the custom name for this kit.
         * 
         * @param name the custom name, or null to remove
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setCustomName(@Nullable String name);
        
        /**
         * Adds a tag to this kit.
         * 
         * @param tag the tag to add
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder addTag(@NotNull String tag);
        
        /**
         * Removes a tag from this kit.
         * 
         * @param tag the tag to remove
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder removeTag(@NotNull String tag);
        
        /**
         * Sets whether this kit is marked as favorite.
         * 
         * @param favorite true to mark as favorite
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setFavorite(boolean favorite);
        
        /**
         * Sets whether this kit is locked.
         * 
         * @param locked true to lock the kit
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setLocked(boolean locked);
        
        /**
         * Sets the health requirement for this kit.
         * 
         * @param health minimum health, or null for no requirement
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setHealthRequirement(@Nullable Double health);
        
        /**
         * Sets the experience cost for this kit.
         * 
         * @param cost experience cost, or null for no cost
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setExperienceCost(@Nullable Integer cost);
        
        /**
         * Sets the cooldown period for this kit.
         * 
         * @param cooldown cooldown in milliseconds, or null for no cooldown
         * @return this builder
         */
        @NotNull
        KitMetadataBuilder setCooldownPeriod(@Nullable Long cooldown);
        
        /**
         * Builds the kit metadata.
         * 
         * @return the built metadata
         */
        @NotNull
        KitMetadata build();
    }
}