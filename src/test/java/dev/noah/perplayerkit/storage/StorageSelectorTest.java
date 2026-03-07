package dev.noah.perplayerkit.storage;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StorageSelectorTest {

    private Plugin plugin;

    @BeforeEach
    void setUp() {
        plugin = mock(Plugin.class);
        YamlConfiguration config = new YamlConfiguration();

        when(plugin.getDataFolder()).thenReturn(new File("target/test-plugin-data"));
        when(plugin.getConfig()).thenReturn(config);
        config.set("mysql.host", "localhost");
        config.set("mysql.port", "3306");
        config.set("mysql.dbname", "ppk");
        config.set("mysql.username", "user");
        config.set("mysql.password", "pass");
        config.set("mysql.useSSL", false);

        config.set("redis.host", "localhost");
        config.set("redis.port", 6379);
        config.set("redis.password", "");
    }

    @Test
    void yamlTypeReturnsYamlStorage() {
        StorageManager manager = new StorageSelector(plugin, "yaml").getDbManager();

        assertInstanceOf(YAMLStorage.class, manager);
    }

    @Test
    void ymlTypeReturnsYamlStorage() {
        StorageManager manager = new StorageSelector(plugin, "yml").getDbManager();

        assertInstanceOf(YAMLStorage.class, manager);
    }

    @Test
    void redisTypeReturnsRedisStorage() {
        StorageManager manager = new StorageSelector(plugin, "redis").getDbManager();

        assertInstanceOf(RedisStorage.class, manager);
    }

    @Test
    void mysqlTypeReturnsSqlStorage() {
        StorageManager manager = new StorageSelector(plugin, "mysql").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }

    @Test
    void sqliteTypeReturnsSqlStorage() {
        StorageManager manager = new StorageSelector(plugin, "sqlite").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }

    @Test
    void unknownTypeDefaultsToSqliteStorage() {
        StorageManager manager = new StorageSelector(plugin, "something-else").getDbManager();

        assertInstanceOf(SQLStorage.class, manager);
    }
}
