/*
 * Copyright 2022-2026 Noah Ross
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
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import dev.noah.perplayerkit.storage.StorageWorkQueue;
import java.util.concurrent.CompletableFuture;

public class KitManager {
    private static KitManager instance;
    private final PerPlayerKit plugin;
    private final Map<String, ItemStack[]> kitByKitIDMap;
    private final Map<UUID, KitReference> lastKitUsedByPlayer;
    private final Map<UUID, Integer> lastEnderchest = new ConcurrentHashMap<>();
    private final Map<UUID, Object> sessions = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();
    private final StorageWorkQueue storageWork;
    private static final String DELETED = "!delete";
    private final Map<String, String> failedWrites = new ConcurrentHashMap<>();
    public record KitReference(Integer slot, String publicId) {
        String key(UUID player) { return publicId == null ? IDUtil.getPlayerKitId(player, slot) : IDUtil.getPublicKitId(publicId); }
    }
    private final List<PublicKit> publicKitList;

    public KitManager(PerPlayerKit plugin) {
        this.plugin = plugin;
        lastKitUsedByPlayer = new ConcurrentHashMap<>();
        storageWork = new StorageWorkQueue(plugin.getLogger());
        publicKitList = new ArrayList<>();
        kitByKitIDMap = new ConcurrentHashMap<>();
        instance = this;
    }

    public static KitManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitManager not initialized");
        }
        return instance;
    }

    public ItemStack[] getItemStackArrayById(String id) {
        return ItemFilter.copy(kitByKitIDMap.get(id));
    }

    private void cacheKit(String id, ItemStack[] kit) {
        if (kit == null) {
            kitByKitIDMap.remove(id);
            return;
        }

        kitByKitIDMap.put(id, ItemFilter.copy(kit));
    }

    public List<PublicKit> getPublicKitList() {
        return publicKitList;
    }

    /**
     * Refreshes a cached kit after its stored data was rewritten externally
     * (e.g. by an admin item purge). Passing null removes the cached entry;
     * a non-null kit only replaces an existing entry, so kits of offline
     * players are not pulled into the cache.
     */
    public void updateCachedKit(String id, ItemStack[] kit) {
        if (kit == null) {
            kitByKitIDMap.remove(id);
        } else {
            kitByKitIDMap.computeIfPresent(id, (key, oldKit) -> ItemFilter.copy(kit));
        }
    }

    /** Legacy accessor: returns -1 when the last kit is public. */
    public int getLastKitLoaded(UUID uuid) {
        KitReference ref = lastKitUsedByPlayer.get(uuid);
        return ref == null || ref.slot() == null ? -1 : ref.slot();
    }

    public KitReference getLastKitReference(UUID uuid) { return lastKitUsedByPlayer.get(uuid); }

    public boolean hasLastKit(UUID uuid) {
        KitReference ref = lastKitUsedByPlayer.get(uuid);
        return ref != null && kitByKitIDMap.containsKey(ref.key(uuid));
    }

    public static boolean isPlayerDataLoading(UUID uuid) { return uuid != null && instance != null && instance.isLoading(uuid); }

    public boolean isLoading(UUID uuid) { return loading.contains(uuid); }

    public StorageWorkQueue storageWork() { return storageWork; }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit) {
        if (isLoading(uuid)) return false;
        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                boolean notEmpty = false;
                for (ItemStack i : kit) {
                    if (i != null) {
                        if (!notEmpty) {
                            notEmpty = true;
                        }
                    }
                }

                if (notEmpty) {
                    if (kit[36] != null) {
                        if (!kit[36].getType().toString().contains("BOOTS")) {
                            kit[36] = null;
                        }
                    }
                    if (kit[37] != null) {
                        if (!kit[37].getType().toString().contains("LEGGINGS")) {
                            kit[37] = null;
                        }
                    }
                    if (kit[38] != null) {
                        if (!(kit[38].getType().toString().contains("CHESTPLATE") || kit[38].getType().toString().contains("ELYTRA"))) {
                            kit[38] = null;
                        }
                    }
                    if (kit[39] != null) {
                        if (!kit[39].getType().toString().contains("HELMET")) {
                            kit[39] = null;
                        }
                    }

                    cacheKit(IDUtil.getPlayerKitId(uuid, slot), kit);
                    Lang.get().send(player, "success.kit-saved", "slot", String.valueOf(slot));

                    savePlayerKitToDB(uuid, slot);
                    return true;
                } else {
                    Lang.get().send(player, "error.empty-kit");
                }
            }
        }
        return false;
    }

    public boolean savePublicKit(Player player, String publickit, ItemStack[] kit) {
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i != null) {
                if (!notEmpty) {
                    notEmpty = true;
                }
            }
        }

        if (notEmpty) {
            if (kit[36] != null) {
                if (!kit[36].getType().toString().contains("BOOTS")) {
                    kit[36] = null;
                }
            }
            if (kit[37] != null) {
                if (!kit[37].getType().toString().contains("LEGGINGS")) {
                    kit[37] = null;
                }
            }
            if (kit[38] != null) {
                if (!(kit[38].getType().toString().contains("CHESTPLATE") || kit[38].getType().toString().contains("ELYTRA"))) {
                    kit[38] = null;
                }
            }
            if (kit[39] != null) {
                if (!kit[39].getType().toString().contains("HELMET")) {
                    kit[39] = null;
                }
            }

            cacheKit(IDUtil.getPublicKitId(publickit), kit);
            Lang.get().send(player, "success.public-kit-saved", "kitname", publickit);

            savePublicKitToDB(publickit);
            return true;
        } else {
            Lang.get().send(player, "error.empty-kit");
        }
        return false;
    }

    public boolean savePublicKit(String id, ItemStack[] kit) {
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i != null) {
                if (!notEmpty) {
                    notEmpty = true;
                }
            }
        }

        if (notEmpty) {
            if (kit[36] != null) {
                if (!kit[36].getType().toString().contains("BOOTS")) {
                    kit[36] = null;
                }
            }
            if (kit[37] != null) {
                if (!kit[37].getType().toString().contains("LEGGINGS")) {
                    kit[37] = null;
                }
            }
            if (kit[38] != null) {
                if (!(kit[38].getType().toString().contains("CHESTPLATE") || kit[38].getType().toString().contains("ELYTRA"))) {
                    kit[38] = null;
                }
            }
            if (kit[39] != null) {
                if (!kit[39].getType().toString().contains("HELMET")) {
                    kit[39] = null;
                }
            }

            cacheKit(IDUtil.getPublicKitId(id), kit);
            return true;
        }
        return false;
    }

    public boolean saveEC(UUID uuid, int slot, ItemStack[] kit) {
        if (isLoading(uuid)) return false;
        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                boolean notEmpty = false;
                for (ItemStack i : kit) {
                    if (i != null) {
                        if (!notEmpty) {
                            notEmpty = true;
                        }
                    }
                }

                if (notEmpty) {
                    cacheKit(IDUtil.getECId(uuid, slot), kit);
                    Lang.get().send(player, "success.ec-saved", "slot", String.valueOf(slot));
                    saveEnderchestToDB(uuid, slot);
                    return true;
                } else {
                    Lang.get().send(player, "error.empty-ec");
                }
            }
        }
        return false;
    }

    public boolean saveECSilent(UUID uuid, int slot, ItemStack[] kit) {
        if (isLoading(uuid)) return false;
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i != null) {
                notEmpty = true;
                break;
            }
        }

        if (!notEmpty) {
            return false;
        }

        cacheKit(IDUtil.getECId(uuid, slot), kit);
        saveEnderchestToDB(uuid, slot);
        return true;
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit, boolean silent) {
        if (isLoading(uuid)) return false;
        if (silent) {
            boolean notEmpty = false;
            for (ItemStack i : kit) {
                if (i != null) {
                    notEmpty = true;
                    break;
                }
            }

            if (!notEmpty) {
                return false;
            }

            if (kit[36] != null && !kit[36].getType().toString().contains("BOOTS")) {
                kit[36] = null;
            }
            if (kit[37] != null && !kit[37].getType().toString().contains("LEGGINGS")) {
                kit[37] = null;
            }
            if (kit[38] != null && !(kit[38].getType().toString().contains("CHESTPLATE") || kit[38].getType().toString().contains("ELYTRA"))) {
                kit[38] = null;
            }
            if (kit[39] != null && !kit[39].getType().toString().contains("HELMET")) {
                kit[39] = null;
            }

            cacheKit(IDUtil.getPlayerKitId(uuid, slot), ItemFilter.copy(kit));
            savePlayerKitToDB(uuid, slot);
            return true;
        } else {
            return savekit(uuid, slot, kit);
        }
    }

    public boolean regearKit(Player player, int slot) {
        return regear(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot));
    }

    public boolean regearLastKit(Player player) {
        KitReference ref = lastKitUsedByPlayer.get(player.getUniqueId());
        return ref != null && regear(player, ref.key(player.getUniqueId()));
    }

    private boolean regear(Player player, String key) {
        if (!kitByKitIDMap.containsKey(key) || !filterReady(player)) return false;
        boolean invertWhitelist = plugin.getConfig().getBoolean("regear.invert-whitelist", false);
        Set<String> whitelist = new HashSet<>(plugin.getConfig().getStringList("regear.whitelist"));

        ItemStack[] kit = ItemFilter.get().filterItemStack(kitByKitIDMap.get(key));
        ItemStack[] playerInventory = player.getInventory().getContents();
        for (int i = 0; i < Math.min(playerInventory.length, kit.length); i++) {
            if (kit[i] == null) {
                continue;
            }

            if (invertWhitelist) {
                if (whitelist.contains(kit[i].getType().toString())) {
                    continue;
                }
            } else {
                if (!whitelist.contains(kit[i].getType().toString())) {
                    continue;
                }
            }

            ItemStack kitItem = kit[i].clone();

            // Filter contents of any shulker box (or other container) against the same whitelist,
            // preventing players from bypassing regear restrictions by hiding items inside shulkers.
            if (kitItem.getItemMeta() instanceof BlockStateMeta blockStateMeta
                    && blockStateMeta.getBlockState() instanceof Container container) {
                ItemStack[] contents = container.getInventory().getContents();
                for (int j = 0; j < contents.length; j++) {
                    if (contents[j] == null) continue;
                    boolean onList = whitelist.contains(contents[j].getType().toString());
                    if (invertWhitelist ? onList : !onList) {
                        contents[j] = null;
                    }
                }
                container.getInventory().setContents(contents);
                blockStateMeta.setBlockState(container);
                kitItem.setItemMeta(blockStateMeta);
            }

            if (playerInventory[i] == null || playerInventory[i].getType().isAir() || playerInventory[i].getType() == kitItem.getType()) {
                playerInventory[i] = kitItem;
                continue;
            }
        }
        player.getInventory().setContents(playerInventory);
        // Resync the client so updated slots (including the offhand) render correctly.
        player.updateInventory();
        return true;
    }

    private boolean loadKitInternal(Player player, String kitId, Runnable notFoundMessage, boolean isEnderChest, Runnable afterLoad) {
        if (player == null) {
            return false;
        }

        ItemStack[] kit = kitByKitIDMap.get(kitId);
        if (kit == null) {
            if (notFoundMessage != null) {
                notFoundMessage.run();
                SoundManager.playFailure(player);
            }
            return false;
        }

        if (!filterReady(player)) return false;
        kit = ItemFilter.get().filterItemStack(kit);
        if (isEnderChest) {
            player.getEnderChest().setContents(kit);
        } else {
            player.getInventory().setContents(kit);
            // Force a client-side resync of the whole inventory (including the
            // offhand slot) so the client doesn't keep rendering a stale item,
            // e.g. a "ghost" totem in the offhand on the death screen.
            player.updateInventory();
        }

        if (afterLoad != null) {
            afterLoad.run();
        }
        SoundManager.playSuccess(player);
        applyKitLoadEffects(player, isEnderChest);
        return true;
    }

    public boolean loadKit(Player player, int slot) {
        if (player == null) {
            return false;
        }
        return loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot),
                () -> Lang.get().send(player, "error.kit-slot-not-found", "slot", String.valueOf(slot)),
                false, () -> {
                    BroadcastManager.get().broadcastPlayerLoadedPrivateKit(player, "Kit " + slot);
                    Lang.get().send(player, "success.kit-loaded", "slot", String.valueOf(slot));
                    lastKitUsedByPlayer.put(player.getUniqueId(), new KitReference(slot, null));
                });
    }

    public boolean loadKitSilent(Player player, int slot) {
        if (player == null) {
            return false;
        }
        return loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot), null, false, null);
    }

    public boolean loadPublicKit(Player player, String id) {
        String kitDisplayName = publicKitList.stream()
                .filter(k -> k.id.equals(id))
                .map(k -> k.name)
                .findFirst()
                .orElse(id);
        return loadKitInternal(player, IDUtil.getPublicKitId(id),
                () -> Lang.get().send(player, "error.kit-not-found"),
                false, () -> {
                    lastKitUsedByPlayer.put(player.getUniqueId(), new KitReference(null, id));
                    BroadcastManager.get().broadcastPlayerLoadedPublicKit(player, kitDisplayName);
                    Lang.get().send(player, "success.public-kit-loaded");
                    Lang.get().send(player, "info.custom-version-available");
                });
    }

    public boolean loadPublicKitSilent(Player player, String id) {
        return loadKitInternal(player, IDUtil.getPublicKitId(id), null, false, null);
    }

    public boolean loadEnderchest(Player player, int slot) {
        if (player == null) {
            return false;
        }
        return loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot),
                () -> Lang.get().send(player, "error.kit-slot-not-found", "slot", String.valueOf(slot)),
                true, () -> {
                    lastEnderchest.put(player.getUniqueId(), slot);
                    BroadcastManager.get().broadcastPlayerLoadedEnderChest(player);
                    Lang.get().send(player, "success.ec-loaded", "slot", String.valueOf(slot));
                });
    }

    public boolean loadEnderchestSilent(Player player, int slot) {
        if (player == null) {
            return false;
        }
        return loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot), null, true, null);
    }

    public boolean loadLastKit(Player player) {
        if (player == null) return false;
        KitReference ref = lastKitUsedByPlayer.get(player.getUniqueId());
        if (ref == null) return false;
        return ref.publicId() == null ? loadKitSilent(player, ref.slot()) : loadPublicKitSilent(player, ref.publicId());
    }

    public void restoreLastEnderchest(Player player) {
        Integer slot = lastEnderchest.get(player.getUniqueId());
        if (slot != null) loadEnderchestSilent(player, slot);
    }

    private boolean filterReady(Player player) {
        if (ItemFilter.get().isReady()) return true;
        Lang.get().send(player, "error.kitroom-not-ready");
        return false;
    }

    public boolean hasKit(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)) != null;
    }

    public boolean hasEC(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getECId(uuid, slot)) != null;
    }

    public ItemStack[] getPlayerEC(UUID uuid, int slot) {
        return ItemFilter.copy(kitByKitIDMap.get(IDUtil.getECId(uuid, slot)));
    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        return ItemFilter.copy(kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)));
    }

    public boolean hasPublicKit(String id) {
        return kitByKitIDMap.get(IDUtil.getPublicKitId(id)) != null;
    }

    public ItemStack[] getPublicKit(String id) {
        return ItemFilter.copy(kitByKitIDMap.get(IDUtil.getPublicKitId(id)));
    }

    public void loadPlayerDataFromDB(UUID uuid) {
        for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
            loadPlayerKitFromDB(uuid, slot);
        }
        for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
            loadPlayerEnderchestFromDB(uuid, slot);
        }
    }

    public void loadPlayerKitFromDB(UUID uuid, int slot) {
        loadKitEntryFromDB(IDUtil.getPlayerKitId(uuid, slot));
    }

    public void loadPlayerEnderchestFromDB(UUID uuid, int slot) {
        loadKitEntryFromDB(IDUtil.getECId(uuid, slot));
    }

    private void loadKitEntryFromDB(String id) {
        String data = PerPlayerKit.storageManager.getKitDataByID(id);
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                cacheKit(id, ItemFilter.copy(kit));
            } catch (IOException ignored) {
            }
        }
    }

    public void savePlayerKitsToDB(UUID uuid) {
        for (int i = 1; i <= KitSlots.maxKits(); i++) {
            savePlayerKitToDB(uuid, i);
            saveEnderchestToDB(uuid, i);
        }
    }

    public void savePlayerKitToDB(UUID uuid, int slot) { saveKitToDB(IDUtil.getPlayerKitId(uuid, slot)); }
    public void saveEnderchestToDB(UUID uuid, int slot) { saveKitToDB(IDUtil.getECId(uuid, slot)); }
    public void savePublicKitToDB(String id) { saveKitToDB(IDUtil.getPublicKitId(id)); }

    private void saveKitToDB(String key) {
        ItemStack[] snapshot = ItemFilter.copy(kitByKitIDMap.get(key));
        if (snapshot == null) return;
        String data = Serializer.itemStackArrayToBase64(snapshot);
        queueWrite(key, data);
    }

    public void queueWrite(String key, String data) {
        storageWork.run(() -> {
            try {
                if (DELETED.equals(data)) PerPlayerKit.storageManager.deleteKitByID(key);
                else PerPlayerKit.storageManager.saveKitDataByID(key, data);
                failedWrites.remove(key);
            } catch (RuntimeException e) {
                failedWrites.put(key, data);
                throw e;
            }
        });
    }

    public void retryFailedWrites() {
        failedWrites.forEach((key, data) -> storageWork.run(() -> {
            if (!data.equals(failedWrites.get(key))) return;
            if (DELETED.equals(data)) PerPlayerKit.storageManager.deleteKitByID(key);
            else PerPlayerKit.storageManager.saveKitDataByID(key, data);
            failedWrites.remove(key, data);
        }));
    }

    public void loadPlayerDataAsync(UUID uuid) {
        Object session = new Object();
        sessions.put(uuid, session);
        loading.add(uuid);
        storageWork.supply(() -> {
            Map<String, String> data = new java.util.LinkedHashMap<>();
            for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
                for (String id : List.of(IDUtil.getPlayerKitId(uuid, slot), IDUtil.getECId(uuid, slot)))
                    data.put(id, failedWrites.getOrDefault(id, PerPlayerKit.storageManager.getKitDataByID(id)));
            }
            return data;
        }).whenComplete((data, error) -> {
            if (!plugin.isEnabled()) return;
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (sessions.get(uuid) != session) return;
                if (error != null) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null) Lang.get().send(player, "error.kit-data-unavailable");
                    return; // Keep editing blocked until a successful join load.
                }
                Map<String, ItemStack[]> parsed = new java.util.LinkedHashMap<>();
                for (Map.Entry<String, String> entry : data.entrySet()) {
                    String encoded = entry.getValue();
                    if (encoded == null || encoded.equalsIgnoreCase("error") || DELETED.equals(encoded)) continue;
                    try { parsed.put(entry.getKey(), Serializer.itemStackArrayFromBase64(encoded)); }
                    catch (IOException e) {
                        plugin.getLogger().warning("Cannot load kit " + entry.getKey() + ": " + e.getMessage());
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) Lang.get().send(player, "error.kit-data-unavailable");
                        return;
                    }
                }
                parsed.forEach(kitByKitIDMap::putIfAbsent);
                loading.remove(uuid);
            });
        });
    }

    public void unloadPlayer(UUID uuid) {
        sessions.remove(uuid);
        loading.remove(uuid);
        lastKitUsedByPlayer.remove(uuid);
        lastEnderchest.remove(uuid);
        // Mutations already captured and queued their own saves. Quit must not rewrite stale data.
        for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
            kitByKitIDMap.remove(IDUtil.getPlayerKitId(uuid, slot));
            kitByKitIDMap.remove(IDUtil.getECId(uuid, slot));
        }
    }

    public void shutdown() {
        storageWork.close();
        if (!failedWrites.isEmpty()) {
            org.bukkit.configuration.file.YamlConfiguration recovery = new org.bukkit.configuration.file.YamlConfiguration();
            recovery.set("storage-type", plugin.getConfig().getString("storage.type"));
            failedWrites.forEach((id, data) -> recovery.set((DELETED.equals(data) ? "delete." : "kits.") + id, DELETED.equals(data) ? true : data));
            try {
                java.nio.file.Path file = plugin.getDataFolder().toPath().resolve("failed-kit-writes-" + System.currentTimeMillis() + ".yml");
                ConfigFiles.write(recovery, file);
                plugin.getLogger().severe("Failed kit saves retained in " + file.getFileName() + ". Restore them before using a different backend.");
            } catch (IOException e) { plugin.getLogger().severe("Cannot write kit recovery file: " + e.getMessage()); }
        }
    }

    public void loadPublicKitFromDB(String id) {
        String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPublicKitId(id));
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                cacheKit(IDUtil.getPublicKitId(id), ItemFilter.copy(kit));
            } catch (IOException ignored) {
                plugin.getLogger().info("Error loading public kit " + id);
            }
        }
    }

    public void deletePublicKit(String id) {
        kitByKitIDMap.remove(IDUtil.getPublicKitId(id));
        queueWrite(IDUtil.getPublicKitId(id), DELETED);
    }

    public boolean deleteKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            kitByKitIDMap.remove(IDUtil.getPlayerKitId(uuid, slot));
            queueWrite(IDUtil.getPlayerKitId(uuid, slot), DELETED);
            return true;
        }
        return false;
    }

    public boolean deleteEnderchest(UUID uuid, int slot) {
        if (hasEC(uuid, slot)) {
            kitByKitIDMap.remove(IDUtil.getECId(uuid, slot));
            queueWrite(IDUtil.getECId(uuid, slot), DELETED);
            return true;
        }
        return false;
    }

    private void applyKitLoadEffects(Player player, boolean isEnderChest) {
        if (player.isDead()) {
            return;
        }

        if (isEnderChest) {
            if (plugin.getConfig().getBoolean("enderchests.load.heal", false)) {
                player.setHealth(player.getMaxHealth());
            }
            if (plugin.getConfig().getBoolean("enderchests.load.feed", false)) {
                player.setFoodLevel(20);
            }
            if (plugin.getConfig().getBoolean("enderchests.load.saturate", false)) {
                player.setSaturation(20);
            }
            if (plugin.getConfig().getBoolean("enderchests.load.clear-effects", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        } else {
            if (plugin.getConfig().getBoolean("kits.load.heal", false)) {
                player.setHealth(player.getMaxHealth());
            }
            if (plugin.getConfig().getBoolean("kits.load.feed", false)) {
                player.setFoodLevel(20);
            }
            if (plugin.getConfig().getBoolean("kits.load.saturate", false)) {
                player.setSaturation(20);
            }
            if (plugin.getConfig().getBoolean("kits.load.clear-effects", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        }
    }
}
