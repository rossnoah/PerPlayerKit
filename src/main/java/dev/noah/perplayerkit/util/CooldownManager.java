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


    //remove cooldown
    public void removeCooldown(String key) {
        cooldownMap.remove(key);
    }

    public void removeCooldown(UUID uuid) {
        removeCooldown(uuid.toString());
    }

    public void removeCooldown(Player player) {
        removeCooldown(player.getUniqueId());
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


    //get cooldown
    public long getCooldownInSeconds(String key) {
        return (cooldownMap.get(key) + cooldownInSeconds * 1000 - System.currentTimeMillis()) / 1000;
    }

    public long getCooldownInSeconds(UUID uuid) {
        return getCooldownInSeconds(uuid.toString());
    }

    public long getCooldownInSeconds(Player player) {
        return getCooldownInSeconds(player.getUniqueId());
    }



}
