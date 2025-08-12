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
package dev.noah.perplayerkit.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * API for managing persistent player data storage.
 * 
 * This interface provides low-level access to player data storage,
 * allowing plugins to store and retrieve custom data associated with players.
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * PlayerDataAPI data = PerPlayerKitAPI.getInstance().data();
 * 
 * // Store custom data
 * data.setData(playerId, "custom.setting", "value")
 *     .thenRun(() -> System.out.println("Data saved!"));
 * 
 * // Retrieve custom data
 * data.getData(playerId, "custom.setting")
 *     .thenAccept(value -> {
 *         if (value.isPresent()) {
 *             System.out.println("Value: " + value.get());
 *         }
 *     });
 * 
 * // Work with typed data
 * data.setData(playerId, "score", 100)
 *     .thenCompose(v -> data.getData(playerId, "score", Integer.class))
 *     .thenAccept(score -> System.out.println("Score: " + score.orElse(0)));
 * }</pre>
 * 
 * @since 2.0.0
 */
public interface PlayerDataAPI {
    
    /**
     * Sets a data value for a player.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @param value the data value
     * @return CompletableFuture that completes when data is saved
     */
    @NotNull
    CompletableFuture<Void> setData(@NotNull UUID playerId, @NotNull String key, @Nullable Object value);
    
    /**
     * Gets a data value for a player.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @return CompletableFuture containing the data value, or empty if not found
     */
    @NotNull
    CompletableFuture<Optional<Object>> getData(@NotNull UUID playerId, @NotNull String key);
    
    /**
     * Gets a typed data value for a player.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @param type the expected type
     * @param <T> the data type
     * @return CompletableFuture containing the typed data value, or empty if not found or wrong type
     */
    @NotNull
    <T> CompletableFuture<Optional<T>> getData(@NotNull UUID playerId, @NotNull String key, @NotNull Class<T> type);
    
    /**
     * Gets a data value with a default fallback.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @param defaultValue the default value if not found
     * @param <T> the data type
     * @return CompletableFuture containing the data value or default
     */
    @NotNull
    <T> CompletableFuture<T> getDataOrDefault(@NotNull UUID playerId, @NotNull String key, @NotNull T defaultValue);
    
    /**
     * Removes a data value for a player.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @return CompletableFuture that completes when data is removed
     */
    @NotNull
    CompletableFuture<Void> removeData(@NotNull UUID playerId, @NotNull String key);
    
    /**
     * Checks if a player has data for a specific key.
     * 
     * @param playerId the player's UUID
     * @param key the data key
     * @return CompletableFuture containing true if data exists
     */
    @NotNull
    CompletableFuture<Boolean> hasData(@NotNull UUID playerId, @NotNull String key);
    
    /**
     * Gets all data keys for a player.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing all data keys
     */
    @NotNull
    CompletableFuture<List<String>> getDataKeys(@NotNull UUID playerId);
    
    /**
     * Gets all data for a player.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture containing all player data
     */
    @NotNull
    CompletableFuture<Map<String, Object>> getAllData(@NotNull UUID playerId);
    
    /**
     * Gets all data for a player with a specific prefix.
     * 
     * @param playerId the player's UUID
     * @param prefix the key prefix to filter by
     * @return CompletableFuture containing filtered player data
     */
    @NotNull
    CompletableFuture<Map<String, Object>> getDataByPrefix(@NotNull UUID playerId, @NotNull String prefix);
    
    /**
     * Removes all data for a player.
     * 
     * @param playerId the player's UUID
     * @return CompletableFuture that completes when all data is removed
     */
    @NotNull
    CompletableFuture<Void> clearAllData(@NotNull UUID playerId);
    
    /**
     * Removes all data for a player with a specific prefix.
     * 
     * @param playerId the player's UUID
     * @param prefix the key prefix to filter by
     * @return CompletableFuture that completes when data is removed
     */
    @NotNull
    CompletableFuture<Void> clearDataByPrefix(@NotNull UUID playerId, @NotNull String prefix);
    
    /**
     * Copies data from one player to another.
     * 
     * @param fromPlayerId the source player's UUID
     * @param toPlayerId the destination player's UUID
     * @param overwrite whether to overwrite existing data
     * @return CompletableFuture that completes when data is copied
     */
    @NotNull
    CompletableFuture<Void> copyData(@NotNull UUID fromPlayerId, @NotNull UUID toPlayerId, boolean overwrite);
    
    /**
     * Copies specific data keys from one player to another.
     * 
     * @param fromPlayerId the source player's UUID
     * @param toPlayerId the destination player's UUID
     * @param keys the data keys to copy
     * @param overwrite whether to overwrite existing data
     * @return CompletableFuture that completes when data is copied
     */
    @NotNull
    CompletableFuture<Void> copyData(@NotNull UUID fromPlayerId, @NotNull UUID toPlayerId, 
                                   @NotNull List<String> keys, boolean overwrite);
    
    /**
     * Gets the total number of players with stored data.
     * 
     * @return CompletableFuture containing the player count
     */
    @NotNull
    CompletableFuture<Integer> getPlayerCount();
    
    /**
     * Gets all player UUIDs that have stored data.
     * 
     * @return CompletableFuture containing all player UUIDs with data
     */
    @NotNull
    CompletableFuture<List<UUID>> getAllPlayerIds();
    
    /**
     * Gets all player UUIDs that have data for a specific key.
     * 
     * @param key the data key
     * @return CompletableFuture containing player UUIDs with the key
     */
    @NotNull
    CompletableFuture<List<UUID>> getPlayersWithKey(@NotNull String key);
    
    /**
     * Gets all player UUIDs that have data matching a specific value.
     * 
     * @param key the data key
     * @param value the data value to match
     * @return CompletableFuture containing matching player UUIDs
     */
    @NotNull
    CompletableFuture<List<UUID>> getPlayersWithValue(@NotNull String key, @NotNull Object value);
    
    /**
     * Performs a batch operation on multiple data entries.
     * 
     * @param operations the batch operations to perform
     * @return CompletableFuture that completes when all operations are done
     */
    @NotNull
    CompletableFuture<Void> batchOperation(@NotNull List<DataOperation> operations);
    
    /**
     * Creates a data builder for complex operations.
     * 
     * @param playerId the player's UUID
     * @return data builder instance
     */
    @NotNull
    DataBuilder forPlayer(@NotNull UUID playerId);
    
    /**
     * Represents a data operation for batch processing.
     */
    interface DataOperation {
        
        /**
         * Gets the operation type.
         * 
         * @return the operation type
         */
        @NotNull
        OperationType getType();
        
        /**
         * Gets the player UUID.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Gets the data key.
         * 
         * @return the data key
         */
        @NotNull
        String getKey();
        
        /**
         * Gets the data value.
         * 
         * @return the data value, or null for remove operations
         */
        @Nullable
        Object getValue();
        
        /**
         * Operation types for batch processing.
         */
        enum OperationType {
            SET,
            REMOVE,
            CLEAR_ALL,
            CLEAR_PREFIX
        }
    }
    
    /**
     * Builder for complex data operations.
     */
    interface DataBuilder {
        
        /**
         * Gets the player UUID associated with this builder.
         * 
         * @return the player UUID
         */
        @NotNull
        UUID getPlayerId();
        
        /**
         * Sets a data value.
         * 
         * @param key the data key
         * @param value the data value
         * @return this builder
         */
        @NotNull
        DataBuilder set(@NotNull String key, @Nullable Object value);
        
        /**
         * Sets multiple data values.
         * 
         * @param data the data map
         * @return this builder
         */
        @NotNull
        DataBuilder setAll(@NotNull Map<String, Object> data);
        
        /**
         * Removes a data value.
         * 
         * @param key the data key
         * @return this builder
         */
        @NotNull
        DataBuilder remove(@NotNull String key);
        
        /**
         * Removes multiple data values.
         * 
         * @param keys the data keys
         * @return this builder
         */
        @NotNull
        DataBuilder removeAll(@NotNull List<String> keys);
        
        /**
         * Clears all data with a specific prefix.
         * 
         * @param prefix the key prefix
         * @return this builder
         */
        @NotNull
        DataBuilder clearPrefix(@NotNull String prefix);
        
        /**
         * Applies all configured operations.
         * 
         * @return CompletableFuture that completes when operations are applied
         */
        @NotNull
        CompletableFuture<Void> apply();
        
        /**
         * Gets the current data without applying changes.
         * 
         * @return CompletableFuture containing current data
         */
        @NotNull
        CompletableFuture<Map<String, Object>> getCurrentData();
        
        /**
         * Resets all pending operations.
         * 
         * @return this builder
         */
        @NotNull
        DataBuilder reset();
    }
}