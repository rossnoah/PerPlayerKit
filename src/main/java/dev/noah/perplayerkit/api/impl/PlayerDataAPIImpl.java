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
package dev.noah.perplayerkit.api.impl;

import dev.noah.perplayerkit.api.data.PlayerDataAPI;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Implementation of the PlayerDataAPI interface.
 */
public class PlayerDataAPIImpl implements PlayerDataAPI {
    
    private final Plugin plugin;
    private final ConcurrentHashMap<UUID, Map<String, Object>> playerData;
    
    public PlayerDataAPIImpl(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.playerData = new ConcurrentHashMap<>();
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> setData(@NotNull UUID playerId, @NotNull String key, @Nullable Object value) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Object> data = playerData.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Optional<Object>> getData(@NotNull UUID playerId, @NotNull String key) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = playerData.get(playerId);
            return data != null ? Optional.ofNullable(data.get(key)) : Optional.empty();
        });
    }
    
    @Override
    @NotNull
    public <T> CompletableFuture<Optional<T>> getData(@NotNull UUID playerId, @NotNull String key, @NotNull Class<T> type) {
        return getData(playerId, key).thenApply(opt -> {
            if (opt.isPresent() && type.isInstance(opt.get())) {
                return Optional.of(type.cast(opt.get()));
            }
            return Optional.empty();
        });
    }
    
    @Override
    @NotNull
    public <T> CompletableFuture<T> getDataOrDefault(@NotNull UUID playerId, @NotNull String key, @NotNull T defaultValue) {
        return getData(playerId, key, (Class<T>) defaultValue.getClass())
            .thenApply(opt -> opt.orElse(defaultValue));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> removeData(@NotNull UUID playerId, @NotNull String key) {
        return setData(playerId, key, null);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasData(@NotNull UUID playerId, @NotNull String key) {
        return getData(playerId, key).thenApply(Optional::isPresent);
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<String>> getDataKeys(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = playerData.get(playerId);
            return data != null ? new ArrayList<>(data.keySet()) : List.of();
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Map<String, Object>> getAllData(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Object> data = playerData.get(playerId);
            return data != null ? new HashMap<>(data) : Map.of();
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Map<String, Object>> getDataByPrefix(@NotNull UUID playerId, @NotNull String prefix) {
        return getAllData(playerId).thenApply(data -> 
            data.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> clearAllData(@NotNull UUID playerId) {
        return CompletableFuture.runAsync(() -> playerData.remove(playerId));
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> clearDataByPrefix(@NotNull UUID playerId, @NotNull String prefix) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Object> data = playerData.get(playerId);
            if (data != null) {
                data.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
            }
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> copyData(@NotNull UUID fromPlayerId, @NotNull UUID toPlayerId, boolean overwrite) {
        return getAllData(fromPlayerId).thenCompose(sourceData -> {
            if (overwrite) {
                return CompletableFuture.runAsync(() -> {
                    Map<String, Object> targetData = playerData.computeIfAbsent(toPlayerId, k -> new ConcurrentHashMap<>());
                    targetData.putAll(sourceData);
                });
            } else {
                return getAllData(toPlayerId).thenCompose(targetData -> 
                    CompletableFuture.runAsync(() -> {
                        Map<String, Object> target = playerData.computeIfAbsent(toPlayerId, k -> new ConcurrentHashMap<>());
                        sourceData.entrySet().stream()
                            .filter(entry -> !target.containsKey(entry.getKey()))
                            .forEach(entry -> target.put(entry.getKey(), entry.getValue()));
                    })
                );
            }
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> copyData(@NotNull UUID fromPlayerId, @NotNull UUID toPlayerId, @NotNull List<String> keys, boolean overwrite) {
        return getAllData(fromPlayerId).thenCompose(sourceData -> {
            Map<String, Object> filteredData = keys.stream()
                .filter(sourceData::containsKey)
                .collect(Collectors.toMap(key -> key, sourceData::get));
            
            if (overwrite) {
                return CompletableFuture.runAsync(() -> {
                    Map<String, Object> targetData = playerData.computeIfAbsent(toPlayerId, k -> new ConcurrentHashMap<>());
                    targetData.putAll(filteredData);
                });
            } else {
                return getAllData(toPlayerId).thenCompose(targetData -> 
                    CompletableFuture.runAsync(() -> {
                        Map<String, Object> target = playerData.computeIfAbsent(toPlayerId, k -> new ConcurrentHashMap<>());
                        filteredData.entrySet().stream()
                            .filter(entry -> !target.containsKey(entry.getKey()))
                            .forEach(entry -> target.put(entry.getKey(), entry.getValue()));
                    })
                );
            }
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<Integer> getPlayerCount() {
        return CompletableFuture.completedFuture(playerData.size());
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<UUID>> getAllPlayerIds() {
        return CompletableFuture.completedFuture(new ArrayList<>(playerData.keySet()));
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<UUID>> getPlayersWithKey(@NotNull String key) {
        return CompletableFuture.supplyAsync(() -> 
            playerData.entrySet().stream()
                .filter(entry -> entry.getValue().containsKey(key))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<UUID>> getPlayersWithValue(@NotNull String key, @NotNull Object value) {
        return CompletableFuture.supplyAsync(() -> 
            playerData.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().get(key), value))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList())
        );
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> batchOperation(@NotNull List<DataOperation> operations) {
        return CompletableFuture.runAsync(() -> {
            for (DataOperation operation : operations) {
                switch (operation.getType()) {
                    case SET:
                        Map<String, Object> data = playerData.computeIfAbsent(operation.getPlayerId(), k -> new ConcurrentHashMap<>());
                        data.put(operation.getKey(), operation.getValue());
                        break;
                    case REMOVE:
                        Map<String, Object> removeData = playerData.get(operation.getPlayerId());
                        if (removeData != null) {
                            removeData.remove(operation.getKey());
                        }
                        break;
                    case CLEAR_ALL:
                        playerData.remove(operation.getPlayerId());
                        break;
                    case CLEAR_PREFIX:
                        Map<String, Object> prefixData = playerData.get(operation.getPlayerId());
                        if (prefixData != null) {
                            prefixData.entrySet().removeIf(entry -> entry.getKey().startsWith(operation.getKey()));
                        }
                        break;
                }
            }
        });
    }
    
    @Override
    @NotNull
    public DataBuilder forPlayer(@NotNull UUID playerId) {
        return new DataBuilderImpl(playerId);
    }
    
    /**
     * Implementation of DataOperation.
     */
    private static class DataOperationImpl implements DataOperation {
        
        private final OperationType type;
        private final UUID playerId;
        private final String key;
        private final Object value;
        
        public DataOperationImpl(@NotNull OperationType type, @NotNull UUID playerId, @NotNull String key, @Nullable Object value) {
            this.type = type;
            this.playerId = playerId;
            this.key = key;
            this.value = value;
        }
        
        @Override
        @NotNull
        public OperationType getType() {
            return type;
        }
        
        @Override
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        @NotNull
        public String getKey() {
            return key;
        }
        
        @Override
        @Nullable
        public Object getValue() {
            return value;
        }
    }
    
    /**
     * Implementation of DataBuilder.
     */
    private class DataBuilderImpl implements DataBuilder {
        
        private final UUID playerId;
        private final List<DataOperation> operations;
        
        public DataBuilderImpl(@NotNull UUID playerId) {
            this.playerId = playerId;
            this.operations = new ArrayList<>();
        }
        
        @Override
        @NotNull
        public UUID getPlayerId() {
            return playerId;
        }
        
        @Override
        @NotNull
        public DataBuilder set(@NotNull String key, @Nullable Object value) {
            operations.add(new DataOperationImpl(DataOperation.OperationType.SET, playerId, key, value));
            return this;
        }
        
        @Override
        @NotNull
        public DataBuilder setAll(@NotNull Map<String, Object> data) {
            data.forEach(this::set);
            return this;
        }
        
        @Override
        @NotNull
        public DataBuilder remove(@NotNull String key) {
            operations.add(new DataOperationImpl(DataOperation.OperationType.REMOVE, playerId, key, null));
            return this;
        }
        
        @Override
        @NotNull
        public DataBuilder removeAll(@NotNull List<String> keys) {
            keys.forEach(this::remove);
            return this;
        }
        
        @Override
        @NotNull
        public DataBuilder clearPrefix(@NotNull String prefix) {
            operations.add(new DataOperationImpl(DataOperation.OperationType.CLEAR_PREFIX, playerId, prefix, null));
            return this;
        }
        
        @Override
        @NotNull
        public CompletableFuture<Void> apply() {
            return PlayerDataAPIImpl.this.batchOperation(operations);
        }
        
        @Override
        @NotNull
        public CompletableFuture<Map<String, Object>> getCurrentData() {
            return PlayerDataAPIImpl.this.getAllData(playerId);
        }
        
        @Override
        @NotNull
        public DataBuilder reset() {
            operations.clear();
            return this;
        }
    }
}