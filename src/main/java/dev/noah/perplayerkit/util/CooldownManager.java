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
package dev.noah.perplayerkit.util;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class CooldownManager {

    private final long cooldownInSeconds;
    private final HashMap<String,Long> cooldownMap;

    public CooldownManager(long cooldownInSeconds) {
        this.cooldownInSeconds = cooldownInSeconds;
        this.cooldownMap = new HashMap<>();
    }

    //check cooldown
    public boolean isOnCooldown(String key) {
        if (!cooldownMap.containsKey(key)) return false;
        return System.currentTimeMillis() - cooldownMap.get(key) < cooldownInSeconds * 1000;
    }

    public boolean isOnCooldown(UUID uuid) {
        return isOnCooldown(uuid.toString());
    }

    public boolean isOnCooldown(Player player) {
        return isOnCooldown(player.getUniqueId());
    }


    //set cooldown
    public void setCooldown(String key) {
        cooldownMap.put(key, System.currentTimeMillis());
    }

    public void setCooldown(UUID uuid) {
        setCooldown(uuid.toString());
    }

    public void setCooldown(Player player) {
        setCooldown(player.getUniqueId());
    }

    //get time left in seconds
    public int getTimeLeft(String key) {
        return (int) (cooldownInSeconds - (System.currentTimeMillis() - cooldownMap.get(key)) / 1000);
    }

    public int getTimeLeft(UUID uuid) {
        return getTimeLeft(uuid.toString());
    }

    public int getTimeLeft(Player player) {
        return getTimeLeft(player.getUniqueId());
    }



}
