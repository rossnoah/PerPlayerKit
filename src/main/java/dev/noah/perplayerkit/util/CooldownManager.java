package dev.noah.perplayerkit.util;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class CooldownManager {

    private long cooldownInSeconds;
    private HashMap<String,Long> cooldownMap;

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




}
