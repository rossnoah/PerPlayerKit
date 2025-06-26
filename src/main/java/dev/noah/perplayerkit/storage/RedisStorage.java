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
package dev.noah.perplayerkit.storage;

import dev.noah.perplayerkit.PerPlayerKit;
import org.bukkit.plugin.Plugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import dev.noah.perplayerkit.ConfigManager;

import java.util.Set;

public class RedisStorage implements StorageManager {

    private final String host;
    private final int port;
    private final String password;
    private JedisPool pool;

    private Plugin plugin;

    public RedisStorage(Plugin plugin) {
        this.plugin = plugin;
        this.host = ConfigManager.get().getRedisHost();
        this.port = ConfigManager.get().getRedisPort();
        this.password = ConfigManager.get().getRedisPassword();
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
