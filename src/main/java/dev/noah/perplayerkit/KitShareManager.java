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
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.PlayerUtil;
import dev.noah.perplayerkit.util.SoundManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class KitShareManager {

    public static final int REQUEST_EXPIRY_SECONDS = 120;

    public static HashMap<String, ItemStack[]> kitShareMap;
    private static KitShareManager instance;
    private final Plugin plugin;
    private final Map<String, ShareRequest> shareRequestsById = new HashMap<>();

    public KitShareManager(Plugin plugin) {
        this.plugin = plugin;
        kitShareMap = new HashMap<>();
        instance = this;
    }

    public static KitShareManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitShareManager has not been initialized");
        }
        return instance;
    }

    public List<String> getKitSlots(Player p) {
        ArrayList<String> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (KitManager.get().hasKit(p.getUniqueId(), i)) {
                slots.add(String.valueOf(i));
            }
        }
        return slots;
    }

    public List<String> getECSlots(Player p) {
        ArrayList<String> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (KitManager.get().hasEC(p.getUniqueId(), i)) {
                slots.add(String.valueOf(i));
            }
        }
        return slots;
    }

    public void shareKit(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasKit(uuid, slot)) {
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if (kitShareMap.putIfAbsent(id, kitManager.getPlayerKit(uuid, slot).clone()) == null) {
                Lang.get().send(p, "info.share-kit-code", "code", id);
                Lang.get().send(p, "info.share-code-expiry");
                Lang.get().send(p, "info.share-kit-direct-hint");
                SoundManager.playSuccess(p);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        kitShareMap.remove(id);
                    }
                }.runTaskLater(plugin, 15 * 60 * 20);

            } else {
                Lang.get().send(p, "error.unexpected");
                SoundManager.playFailure(p);
            }

        } else {
            Lang.get().send(p, "error.kit-not-found");
            SoundManager.playFailure(p);
        }
    }


    public void shareEC(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasEC(uuid, slot)) {
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if (kitShareMap.putIfAbsent(id, kitManager.getPlayerEC(uuid, slot).clone()) == null) {
                Lang.get().send(p, "info.share-ec-code", "code", id);
                Lang.get().send(p, "info.share-code-expiry");
                Lang.get().send(p, "info.share-ec-direct-hint");
                SoundManager.playSuccess(p);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        kitShareMap.remove(id);
                    }
                }.runTaskLater(plugin, 15 * 60 * 20);

            } else {
                Lang.get().send(p, "error.unexpected");
                SoundManager.playFailure(p);
            }

        } else {
            Lang.get().send(p, "error.ec-not-found");
            SoundManager.playFailure(p);
        }
    }


    public void copyKit(Player p, String str) {

        String id = str.toUpperCase();
        if (!kitShareMap.containsKey(id)) {
            Lang.get().send(p, "error.kit-expired");
            SoundManager.playFailure(p);
            return;
        }

        ItemStack[] data = kitShareMap.get(id);

        if (data.length == 27) {
            p.getEnderChest().setContents(kitShareMap.get(id));
            BroadcastManager.get().broadcastPlayerCopiedEC(p);
            SoundManager.playSuccess(p);

        } else if (data.length == 41) {
            p.getInventory().setContents(kitShareMap.get(id));
            // Resync the client (including the offhand slot) so it doesn't render stale items.
            p.updateInventory();
            BroadcastManager.get().broadcastPlayerCopiedKit(p);
            SoundManager.playSuccess(p);
        } else {
            Lang.get().send(p, "error.unexpected");
            SoundManager.playFailure(p);
        }
    }

    public void sendKitShareRequest(Player sender, int slot, Player target) {
        if (!validateTarget(sender, target)) {
            return;
        }

        KitManager kitManager = KitManager.get();
        if (!kitManager.hasKit(sender.getUniqueId(), slot)) {
            Lang.get().send(sender, "error.kit-not-found");
            SoundManager.playFailure(sender);
            return;
        }

        ShareRequest request = ShareRequest.kit(nextRequestId(), sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), kitManager.getPlayerKit(sender.getUniqueId(), slot).clone());
        registerAndAnnounce(request, sender, target, "info.share-request-kit-received");
    }

    public void sendECShareRequest(Player sender, int slot, Player target) {
        if (!validateTarget(sender, target)) {
            return;
        }

        KitManager kitManager = KitManager.get();
        if (!kitManager.hasEC(sender.getUniqueId(), slot)) {
            Lang.get().send(sender, "error.ec-not-found");
            SoundManager.playFailure(sender);
            return;
        }

        ShareRequest request = ShareRequest.enderchest(nextRequestId(), sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), kitManager.getPlayerEC(sender.getUniqueId(), slot).clone());
        registerAndAnnounce(request, sender, target, "info.share-request-ec-received");
    }

    public void sendTransferRequest(Player sender, Player target) {
        if (!validateTarget(sender, target)) {
            return;
        }

        KitManager kitManager = KitManager.get();
        Map<Integer, ItemStack[]> kits = new LinkedHashMap<>();
        Map<Integer, ItemStack[]> enderchests = new LinkedHashMap<>();
        for (int slot = 1; slot <= 9; slot++) {
            if (kitManager.hasKit(sender.getUniqueId(), slot)) {
                kits.put(slot, kitManager.getPlayerKit(sender.getUniqueId(), slot).clone());
            }
            if (kitManager.hasEC(sender.getUniqueId(), slot)) {
                enderchests.put(slot, kitManager.getPlayerEC(sender.getUniqueId(), slot).clone());
            }
        }

        if (kits.isEmpty() && enderchests.isEmpty()) {
            Lang.get().send(sender, "error.nothing-to-transfer");
            SoundManager.playFailure(sender);
            return;
        }

        ShareRequest request = ShareRequest.transfer(nextRequestId(), sender.getUniqueId(), sender.getName(),
                target.getUniqueId(), kits, enderchests);
        registerAndAnnounce(request, sender, target, "info.share-request-transfer-received");
    }

    public void acceptRequest(Player target, @Nullable String idArg) {
        ShareRequest request = resolveRequest(target, idArg);
        if (request == null) {
            return;
        }

        request.cancelExpiryTask();
        shareRequestsById.remove(request.getId());

        Player sender = Bukkit.getPlayer(request.getSenderId());
        switch (request.getType()) {
            case KIT -> {
                target.getInventory().setContents(request.getContents());
                // Resync the client (including the offhand slot) so it doesn't render stale items.
                target.updateInventory();
                BroadcastManager.get().broadcastPlayerCopiedKit(target);
                if (sender != null) {
                    Lang.get().send(sender, "success.share-request-accepted", "player", target.getName());
                }
            }
            case ENDERCHEST -> {
                target.getEnderChest().setContents(request.getContents());
                BroadcastManager.get().broadcastPlayerCopiedEC(target);
                if (sender != null) {
                    Lang.get().send(sender, "success.share-request-accepted", "player", target.getName());
                }
            }
            case TRANSFER -> {
                KitManager kitManager = KitManager.get();
                request.getKits().forEach((slot, kit) -> kitManager.savekit(target.getUniqueId(), slot, kit, true));
                request.getEnderchests().forEach((slot, ec) -> kitManager.saveECSilent(target.getUniqueId(), slot, ec));
                Lang.get().send(target, "success.transfer-received",
                        "player", request.getSenderName(),
                        "kits", String.valueOf(request.getKits().size()),
                        "ecs", String.valueOf(request.getEnderchests().size()));
                if (sender != null) {
                    Lang.get().send(sender, "success.transfer-sent", "player", target.getName());
                }
            }
        }
        SoundManager.playSuccess(target);
    }

    public void declineRequest(Player target, @Nullable String idArg) {
        ShareRequest request = resolveRequest(target, idArg);
        if (request == null) {
            return;
        }

        request.cancelExpiryTask();
        shareRequestsById.remove(request.getId());

        Lang.get().send(target, "info.share-request-declined-target", "player", request.getSenderName());
        Player sender = Bukkit.getPlayer(request.getSenderId());
        if (sender != null) {
            Lang.get().send(sender, "info.share-request-declined-sender", "player", target.getName());
            SoundManager.playFailure(sender);
        }
    }

    public List<String> getPendingRequestIds(Player target) {
        List<String> ids = new ArrayList<>();
        for (ShareRequest request : shareRequestsById.values()) {
            if (request.getTargetId().equals(target.getUniqueId())) {
                ids.add(request.getId());
            }
        }
        return ids;
    }

    private boolean validateTarget(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            Lang.get().send(sender, "error.share-with-self");
            SoundManager.playFailure(sender);
            return false;
        }
        return true;
    }

    private void registerAndAnnounce(ShareRequest request, Player sender, Player target, String receivedMessageKey) {
        // A newer request from the same sender to the same target replaces the old one.
        Iterator<ShareRequest> iterator = shareRequestsById.values().iterator();
        while (iterator.hasNext()) {
            ShareRequest existing = iterator.next();
            if (existing.getSenderId().equals(request.getSenderId())
                    && existing.getTargetId().equals(request.getTargetId())
                    && existing.getType() == request.getType()) {
                existing.cancelExpiryTask();
                iterator.remove();
            }
        }

        shareRequestsById.put(request.getId(), request);

        BukkitTask expiryTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (shareRequestsById.remove(request.getId()) == null) {
                    return;
                }
                Player sender = Bukkit.getPlayer(request.getSenderId());
                if (sender != null) {
                    Lang.get().send(sender, "info.share-request-expired", "player", PlayerUtil.getPlayerName(request.getTargetId()));
                }
            }
        }.runTaskLater(plugin, REQUEST_EXPIRY_SECONDS * 20L);
        request.setExpiryTask(expiryTask);

        Lang.get().send(sender, "info.share-request-sent",
                "player", target.getName(),
                "seconds", String.valueOf(REQUEST_EXPIRY_SECONDS));
        SoundManager.playSuccess(sender);

        Lang.get().send(target, receivedMessageKey, "player", sender.getName());
        Lang.get().sendNoPrefix(target, "info.share-request-buttons", "id", request.getId());
        SoundManager.playSuccess(target);
    }

    private @Nullable ShareRequest resolveRequest(Player target, @Nullable String idArg) {
        if (idArg == null || idArg.isBlank()) {
            List<ShareRequest> pending = findRequestsForTarget(shareRequestsById, target.getUniqueId());
            if (pending.isEmpty()) {
                Lang.get().send(target, "error.share-no-pending-requests");
                SoundManager.playFailure(target);
                return null;
            }
            if (pending.size() > 1) {
                Lang.get().send(target, "error.share-multiple-pending-requests");
                SoundManager.playFailure(target);
                return null;
            }
            return pending.get(0);
        }

        ShareRequest request = findRequestById(shareRequestsById, target.getUniqueId(), idArg);
        if (request == null) {
            Lang.get().send(target, "error.share-request-not-found");
            SoundManager.playFailure(target);
            return null;
        }
        return request;
    }

    static List<ShareRequest> findRequestsForTarget(Map<String, ShareRequest> requests, UUID targetId) {
        List<ShareRequest> pending = new ArrayList<>();
        for (ShareRequest request : requests.values()) {
            if (request.getTargetId().equals(targetId)) {
                pending.add(request);
            }
        }
        return pending;
    }

    static @Nullable ShareRequest findRequestById(Map<String, ShareRequest> requests, UUID targetId, String id) {
        ShareRequest request = requests.get(id.toUpperCase());
        if (request == null || !request.getTargetId().equals(targetId)) {
            return null;
        }
        return request;
    }

    private String nextRequestId() {
        String id;
        do {
            id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        } while (shareRequestsById.containsKey(id));
        return id;
    }
}
