package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.PerPlayerKit;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisStorage implements StorageManager {

    private final String host;
    private final int port;
    private final String password;
    private JedisPool pool;

    public RedisStorage() {
        var plugin = PerPlayerKit.getPlugin();
        this.host = plugin.getConfig().getString("redis.host");
        this.port = plugin.getConfig().getInt("redis.port") == 0 ? Integer.parseInt(plugin.getConfig().getString("redis.port","6379")) : plugin.getConfig().getInt("redis.port");
        this.password = plugin.getConfig().getString("redis.password");
    }

    @Override
    public void connect() {
        if (pool == null) {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            if (password == null || password.isEmpty()) {
                pool = new JedisPool(poolConfig, host, port);
            } else {
                pool = new JedisPool(poolConfig, host, port, 2000, password);
            }
        }
    }

    @Override
    public boolean isConnected() {
        if (pool == null) {
            return false;
        }
        try (Jedis jedis = getConnection()) {
            return "PONG".equals(jedis.ping());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void init() {
        // No specific initialization needed for Redis
    }

    @Override
    public void close() {
        if (pool != null) {
            pool.close();
        }
    }

    @Override
    public void keepAlive() {
        try (Jedis jedis = getConnection()) {
            jedis.ping();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveKitDataByID(String kitID, String data) {
        try (Jedis jedis = getConnection()) {
            jedis.set(kitID, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getKitDataByID(String kitID) {
        try (Jedis jedis = getConnection()) {
            String data = jedis.get(kitID);
            return data == null ? "Error" : data;
        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

    @Override
    public boolean doesKitExistByID(String kitID) {
        try (Jedis jedis = getConnection()) {
            return jedis.exists(kitID);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void deleteKitByID(String kitID) {
        try (Jedis jedis = getConnection()) {
            jedis.del(kitID);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Jedis getConnection() {
        if (pool == null) {
            throw new IllegalStateException("Redis pool is not initialized. Call connect() first.");
        }
        return pool.getResource();
    }
}