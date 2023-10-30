package net.vanillapractice.perplayerkit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public class Cooldown {


    public static void updateRepairCooldown(UUID uuid) {
        PerPlayerKit.repairBroadcastCooldown.put(uuid, Timestamp.from(Instant.now()));
    }

    public static boolean isOnRepairCooldown(UUID uuid) {
        if (!PerPlayerKit.repairBroadcastCooldown.containsKey(uuid)) return false;
        long dif = Timestamp.from(Instant.now()).getTime() - PerPlayerKit.repairBroadcastCooldown.get(uuid).getTime();
        dif = (PerPlayerKit.repairDelay * 1000) - dif;
        return dif > 0;
    }

    public static void updateKitroomCooldown(UUID uuid) {
        PerPlayerKit.kitRoomBroadcastCooldown.put(uuid, Timestamp.from(Instant.now()));
    }

    public static boolean isOnKitroomCooldown(UUID uuid) {
        if (!PerPlayerKit.kitRoomBroadcastCooldown.containsKey(uuid)) return false;
        long dif = Timestamp.from(Instant.now()).getTime() - PerPlayerKit.kitRoomBroadcastCooldown.get(uuid).getTime();
        dif = (PerPlayerKit.kitRoomDelay * 1000) - dif;
        return dif > 0;
    }

    public static void updateShareCooldown(UUID uuid) {
        PerPlayerKit.shareCooldown.put(uuid, Timestamp.from(Instant.now()));
    }

    public static boolean isOnShareCooldown(UUID uuid) {
        if (!PerPlayerKit.shareCooldown.containsKey(uuid)) return false;
        long dif = Timestamp.from(Instant.now()).getTime() - PerPlayerKit.shareCooldown.get(uuid).getTime();
        dif = (PerPlayerKit.shareDelay * 1000) - dif;
        return dif > 0;
    }




}
