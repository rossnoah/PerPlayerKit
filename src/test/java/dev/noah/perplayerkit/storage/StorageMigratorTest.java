package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorageMigratorTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        Logger logger = Logger.getLogger("StorageMigratorTest");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.OFF);
        when(plugin.getLogger()).thenReturn(logger);
    }

    @Test
    void migrateRejectsSameSourceAndDestination() {
        StorageMigrator migrator = new StorageMigrator(plugin);

        StorageMigrator.MigrationResult result = migrator.migrate("sqlite", "sqlite", null);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getMigratedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals("Source and destination storage types are the same.", result.getErrorMessage());
    }

    @Test
    void migrateReturnsNoDataWhenSourceIsEmptyAndClosesConnections() throws Exception {
        StorageManager source = mock(StorageManager.class);
        StorageManager destination = mock(StorageManager.class);
        when(source.getAllKitIDs()).thenReturn(new HashSet<>());

        StorageMigrator migrator = new TestableStorageMigrator(plugin, source, destination);

        StorageMigrator.MigrationResult result = migrator.migrate("sqlite", "mysql", null);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getMigratedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals("No data to migrate.", result.getErrorMessage());

        verify(source).connect();
        verify(source).init();
        verify(destination).connect();
        verify(destination).init();
        verify(source).close();
        verify(destination).close();
    }

    @Test
    void migrateCountsMigratedAndFailedEntries() throws Exception {
        StorageManager source = mock(StorageManager.class);
        StorageManager destination = mock(StorageManager.class);

        when(source.getAllKitIDs()).thenReturn(new HashSet<>(Arrays.asList("a", "b", "c", "d")));
        when(source.getKitDataByID("a")).thenReturn("data-a");
        when(source.getKitDataByID("b")).thenReturn(null);
        when(source.getKitDataByID("c")).thenReturn("Error");
        when(source.getKitDataByID("d")).thenThrow(new RuntimeException("read failure"));

        StorageMigrator migrator = new TestableStorageMigrator(plugin, source, destination);

        StorageMigrator.MigrationResult result = migrator.migrate("sqlite", "redis", null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getMigratedCount());
        assertEquals(3, result.getFailedCount());
        assertEquals(null, result.getErrorMessage());

        verify(destination, times(1)).saveKitDataByID("a", "data-a");
        verify(source).close();
        verify(destination).close();
    }

    @Test
    void migrateHandlesConnectionErrorsAndStillClosesManagers() throws Exception {
        StorageManager source = mock(StorageManager.class);
        StorageManager destination = mock(StorageManager.class);
        doThrow(new StorageConnectionException("boom")).when(source).connect();

        StorageMigrator migrator = new TestableStorageMigrator(plugin, source, destination);

        StorageMigrator.MigrationResult result = migrator.migrate("sqlite", "mysql", null);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getMigratedCount());
        assertEquals(0, result.getFailedCount());
        assertTrue(result.getErrorMessage().startsWith("Connection error:"));

        verify(source).close();
        verify(destination).close();
    }

    private static class TestableStorageMigrator extends StorageMigrator {
        private final StorageManager source;
        private final StorageManager destination;

        private TestableStorageMigrator(Plugin plugin, StorageManager source, StorageManager destination) {
            super(plugin);
            this.source = source;
            this.destination = destination;
        }

        @Override
        StorageManager createStorageManager(String storageType) {
            return "source".equals(storageType) ? source : destination;
        }

        @Override
        public MigrationResult migrate(String sourceType, String destinationType, java.util.function.Consumer<String> progressCallback) {
            return super.migrate("source", "destination", progressCallback);
        }
    }
}
