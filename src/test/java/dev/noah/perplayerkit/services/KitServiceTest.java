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
package dev.noah.perplayerkit.services;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.config.ConfigurationManager;
import dev.noah.perplayerkit.exceptions.KitException;
import dev.noah.perplayerkit.logging.PerPlayerKitLogger;
import dev.noah.perplayerkit.metrics.MetricsCollector;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for KitService using modern testing practices.
 * Demonstrates proper mocking, async testing, and comprehensive test coverage.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KitService Tests")
class KitServiceTest {
    
    @Mock
    private PerPlayerKit plugin;
    
    @Mock
    private PerPlayerKitLogger logger;
    
    @Mock
    private ConfigurationManager configManager;
    
    @Mock
    private MetricsCollector metrics;
    
    @Mock
    private MetricsCollector.Timer timer;
    
    private KitService kitService;
    private UUID testPlayerId;
    private ItemStack[] testItems;
    
    @BeforeEach
    void setUp() {
        kitService = new KitService(plugin, logger, configManager, metrics);
        testPlayerId = UUID.randomUUID();
        testItems = createTestItems();
        
        // Setup common mock behavior
        when(metrics.startTimer(anyString())).thenReturn(timer);
        when(timer.stop()).thenReturn(100L);
    }
    
    @Test
    @DisplayName("Should validate player ID when saving kit")
    void shouldValidatePlayerIdWhenSavingKit() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            kitService.saveKitAsync(null, 1, testItems));
    }
    
    @Test
    @DisplayName("Should validate slot number when saving kit")
    void shouldValidateSlotNumberWhenSavingKit() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            kitService.saveKitAsync(testPlayerId, 0, testItems));
        
        assertThrows(IllegalArgumentException.class, () -> 
            kitService.saveKitAsync(testPlayerId, 10, testItems));
    }
    
    @Test
    @DisplayName("Should validate items array when saving kit")
    void shouldValidateItemsArrayWhenSavingKit() {
        // Given
        ItemStack[] emptyItems = new ItemStack[40]; // All nulls
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> 
            kitService.saveKitAsync(testPlayerId, 1, null));
        
        assertThrows(IllegalArgumentException.class, () -> 
            kitService.saveKitAsync(testPlayerId, 1, emptyItems));
    }
    
    @Test
    @DisplayName("Should save kit successfully with valid inputs")
    void shouldSaveKitSuccessfullyWithValidInputs() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<Void> future = kitService.saveKitAsync(testPlayerId, 1, testItems);
        
        // Then
        assertDoesNotThrow(() -> future.get());
        verify(metrics).startTimer("kit.save");
        verify(metrics).incrementCounter("kits.saved");
        verify(logger).info(eq("Kit saved successfully"), eq(testPlayerId));
    }
    
    @Test
    @DisplayName("Should load kit successfully when it exists")
    void shouldLoadKitSuccessfullyWhenItExists() throws ExecutionException, InterruptedException {
        // Given - First save a kit
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        
        // When
        CompletableFuture<Optional<ItemStack[]>> future = kitService.loadKitAsync(testPlayerId, 1);
        Optional<ItemStack[]> result = future.get();
        
        // Then
        assertTrue(result.isPresent());
        assertArrayEquals(testItems, result.get());
        verify(metrics).startTimer("kit.load");
        verify(metrics).incrementCounter("kits.cache.hits");
    }
    
    @Test
    @DisplayName("Should return empty when kit does not exist")
    void shouldReturnEmptyWhenKitDoesNotExist() throws ExecutionException, InterruptedException {
        // When
        CompletableFuture<Optional<ItemStack[]>> future = kitService.loadKitAsync(testPlayerId, 1);
        Optional<ItemStack[]> result = future.get();
        
        // Then
        assertFalse(result.isPresent());
        verify(metrics).startTimer("kit.load");
        verify(metrics).incrementCounter("kits.cache.misses");
    }
    
    @Test
    @DisplayName("Should check kit existence correctly")
    void shouldCheckKitExistenceCorrectly() throws ExecutionException, InterruptedException {
        // Given - Save a kit in slot 1
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        
        // When & Then
        assertTrue(kitService.hasKit(testPlayerId, 1));
        assertFalse(kitService.hasKit(testPlayerId, 2));
    }
    
    @Test
    @DisplayName("Should delete kit successfully")
    void shouldDeleteKitSuccessfully() throws ExecutionException, InterruptedException {
        // Given - First save a kit
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        assertTrue(kitService.hasKit(testPlayerId, 1));
        
        // When
        CompletableFuture<Void> future = kitService.deleteKitAsync(testPlayerId, 1);
        future.get();
        
        // Then
        assertFalse(kitService.hasKit(testPlayerId, 1));
        verify(metrics).startTimer("kit.delete");
        verify(metrics).incrementCounter("kits.deleted");
        verify(logger).info(eq("Kit deleted successfully"), eq(testPlayerId));
    }
    
    @Test
    @DisplayName("Should get occupied slots correctly")
    void shouldGetOccupiedSlotsCorrectly() throws ExecutionException, InterruptedException {
        // Given - Save kits in slots 1, 3, and 5
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        kitService.saveKitAsync(testPlayerId, 3, testItems).get();
        kitService.saveKitAsync(testPlayerId, 5, testItems).get();
        
        // When
        var occupiedSlots = kitService.getOccupiedSlots(testPlayerId).toList();
        
        // Then
        assertEquals(3, occupiedSlots.size());
        assertTrue(occupiedSlots.contains(1));
        assertTrue(occupiedSlots.contains(3));
        assertTrue(occupiedSlots.contains(5));
        assertFalse(occupiedSlots.contains(2));
        assertFalse(occupiedSlots.contains(4));
    }
    
    @Test
    @DisplayName("Should clear player cache successfully")
    void shouldClearPlayerCacheSuccessfully() throws ExecutionException, InterruptedException {
        // Given - Save multiple kits
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        kitService.saveKitAsync(testPlayerId, 2, testItems).get();
        int initialCacheSize = kitService.getCacheSize();
        
        // When
        kitService.clearPlayerCache(testPlayerId);
        
        // Then
        assertTrue(kitService.getCacheSize() < initialCacheSize);
        verify(logger).debug(eq("Player kit cache cleared"), eq(testPlayerId));
    }
    
    @Test
    @DisplayName("Should clear entire cache successfully")
    void shouldClearEntireCacheSuccessfully() throws ExecutionException, InterruptedException {
        // Given - Save some kits
        kitService.saveKitAsync(testPlayerId, 1, testItems).get();
        kitService.saveKitAsync(UUID.randomUUID(), 1, testItems).get();
        assertTrue(kitService.getCacheSize() > 0);
        
        // When
        kitService.clearCache();
        
        // Then
        assertEquals(0, kitService.getCacheSize());
        verify(logger).info("Kit cache cleared");
    }
    
    @Test
    @DisplayName("Should handle concurrent operations safely")
    void shouldHandleConcurrentOperationsSafely() {
        // This test verifies thread safety by running multiple operations concurrently
        CompletableFuture<Void>[] futures = new CompletableFuture[10];
        
        for (int i = 0; i < 10; i++) {
            final int slot = (i % 9) + 1;
            futures[i] = kitService.saveKitAsync(testPlayerId, slot, testItems);
        }
        
        // Wait for all operations to complete
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures);
        
        // Should not throw any exceptions
        assertDoesNotThrow(() -> allOf.get());
    }
    
    /**
     * Creates test items for use in tests.
     */
    private ItemStack[] createTestItems() {
        ItemStack[] items = new ItemStack[40];
        items[0] = new ItemStack(Material.DIAMOND_SWORD);
        items[1] = new ItemStack(Material.GOLDEN_APPLE, 16);
        items[36] = new ItemStack(Material.DIAMOND_BOOTS); // Valid boots in armor slot
        return items;
    }
}