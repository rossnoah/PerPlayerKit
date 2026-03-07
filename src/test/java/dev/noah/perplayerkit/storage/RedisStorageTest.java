package dev.noah.perplayerkit.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

import java.io.File;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RedisStorageTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("redis.host", "127.0.0.1");
        config.set("redis.port", 6379);
        config.set("redis.password", "");

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(new File("target/test-plugin-data"));
    }

    @Test
    void notConnectedInitially() {
        RedisStorage storage = new RedisStorage(plugin);

        assertFalse(storage.isConnected());
    }

    @Test
    void methodsFailGracefullyWhenPoolNotInitialized() {
        RedisStorage storage = new RedisStorage(plugin);

        assertDoesNotThrow(() -> storage.keepAlive());
        assertDoesNotThrow(() -> storage.saveKitDataByID("kit-1", "payload-1"));
        assertDoesNotThrow(() -> storage.deleteKitByID("kit-1"));

        assertEquals("Error", storage.getKitDataByID("kit-1"));
        assertFalse(storage.doesKitExistByID("kit-1"));
        assertTrue(storage.getAllKitIDs().isEmpty());
    }

    @Test
    void connectInitializesPoolAndCloseIsSafe() throws Exception {
        RedisStorage storage = new RedisStorage(plugin);

        storage.connect();

        JedisPool pool = getPool(storage);
        assertNotNull(pool);
        assertDoesNotThrow(storage::close);
    }

    private JedisPool getPool(RedisStorage storage) throws Exception {
        Field field = RedisStorage.class.getDeclaredField("pool");
        field.setAccessible(true);
        return (JedisPool) field.get(storage);
    }
}
