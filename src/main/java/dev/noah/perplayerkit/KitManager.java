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
        return KitContents.copy(kitByKitIDMap.get(id));
    }

    /** Takes ownership of a prepared or decoded snapshot; outward reads always copy it. */
    private void cacheKit(String id, ItemStack[] kit) {
        if (kit == null) {
            kitByKitIDMap.remove(id);
            return;
        }

        kitByKitIDMap.put(id, kit);
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
            kitByKitIDMap.computeIfPresent(id, (key, oldKit) -> KitContents.copy(kit));
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
        return savePersonalKit(uuid, slot, kit, false, false);
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit, boolean silent) {
        return savePersonalKit(uuid, slot, kit, false, silent);
    }

    public boolean saveEC(UUID uuid, int slot, ItemStack[] kit) {
        return savePersonalKit(uuid, slot, kit, true, false);
    }

    public boolean saveECSilent(UUID uuid, int slot, ItemStack[] kit) {
        return savePersonalKit(uuid, slot, kit, true, true);
    }

    private boolean savePersonalKit(UUID uuid, int slot, ItemStack[] contents, boolean enderchest, boolean silent) {
        if (uuid == null || slot < KitSlots.MIN_LIMIT || slot > KitSlots.MAX_LIMIT || isLoading(uuid)) return false;
        Player player = silent ? null : Bukkit.getPlayer(uuid);
        if (!silent && player == null) return false;

        ItemStack[] prepared = KitContents.prepare(contents, enderchest);
        if (prepared == null) {
            if (player != null) Lang.get().send(player, enderchest ? "error.empty-ec" : "error.empty-kit");
            return false;
        }
        String id = enderchest ? IDUtil.getECId(uuid, slot) : IDUtil.getPlayerKitId(uuid, slot);
        cacheKit(id, prepared);
        saveKitToDB(id);
        if (player != null) Lang.get().send(player, enderchest ? "success.ec-saved" : "success.kit-saved", "slot", String.valueOf(slot));
        return true;
    }

    public boolean savePublicKit(Player player, String id, ItemStack[] contents) {
        if (player == null) return false;
        if (!savePublicKit(id, contents)) {
            Lang.get().send(player, "error.empty-kit");
            return false;
        }
        savePublicKitToDB(id);
        Lang.get().send(player, "success.public-kit-saved", "kitname", id);
        return true;
    }

    /** Caches a public kit. Call savePublicKitToDB after preparing a batch of public kits. */
    public boolean savePublicKit(String id, ItemStack[] contents) {
        if (id == null || id.isBlank()) return false;
        ItemStack[] prepared = KitContents.prepare(contents, false);
        if (prepared == null) return false;
        cacheKit(IDUtil.getPublicKitId(id), prepared);
        return true;
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
                    rememberKit(player.getUniqueId(), new KitReference(slot, null));
                });
    }

    private void rememberKit(UUID uuid, KitReference reference) {
        lastKitUsedByPlayer.put(uuid, reference);
        queueWrite(KitSelection.kitKey(uuid), KitSelection.encode(reference));
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
                    rememberKit(player.getUniqueId(), new KitReference(null, id));
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
                    queueWrite(KitSelection.enderchestKey(player.getUniqueId()), Integer.toString(slot));
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
        return KitContents.copy(kitByKitIDMap.get(IDUtil.getECId(uuid, slot)));
    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        return KitContents.copy(kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)));
    }

    public boolean hasPublicKit(String id) {
        return kitByKitIDMap.get(IDUtil.getPublicKitId(id)) != null;
    }

    public ItemStack[] getPublicKit(String id) {
        return KitContents.copy(kitByKitIDMap.get(IDUtil.getPublicKitId(id)));
    }

    public void loadPlayerDataFromDB(UUID uuid) {
        for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
            loadPlayerKitFromDB(uuid, slot);
        }
        for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
            loadPlayerEnderchestFromDB(uuid, slot);
        }
        restoreSelections(uuid, readStoredData(KitSelection.kitKey(uuid)), readStoredData(KitSelection.enderchestKey(uuid)));
    }

    public void loadPlayerKitFromDB(UUID uuid, int slot) {
        loadKitEntryFromDB(IDUtil.getPlayerKitId(uuid, slot), KitContents.INVENTORY_SIZE);
    }

    public void loadPlayerEnderchestFromDB(UUID uuid, int slot) {
        loadKitEntryFromDB(IDUtil.getECId(uuid, slot), KitContents.ENDERCHEST_SIZE);
    }

    private static boolean isStoredData(String data) {
        return data != null && !data.equalsIgnoreCase("error") && !DELETED.equals(data);
    }

    private static ItemStack[] decodeKit(String data, int size) throws IOException {
        ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
        if (!KitContents.hasSize(kit, size)) throw new IOException("Expected " + size + " inventory slots");
        return kit;
    }

    private void loadKitEntryFromDB(String id, int size) {
        String data = PerPlayerKit.storageManager.getKitDataByID(id);
        if (!isStoredData(data)) return;
        try {
            cacheKit(id, decodeKit(data, size));
        } catch (IOException e) {
            plugin.getLogger().warning("Cannot load kit " + id + ": " + e.getMessage());
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
        ItemStack[] snapshot = KitContents.copy(kitByKitIDMap.get(key));
        if (snapshot == null) return;
        String data = Serializer.itemStackArrayToBase64(snapshot);
        queueWrite(key, data);
    }

    private void writeStoredData(String key, String data) {
        if (DELETED.equals(data)) PerPlayerKit.storageManager.deleteKitByID(key);
        else PerPlayerKit.storageManager.saveKitDataByID(key, data);
    }

    public void queueWrite(String key, String data) {
        storageWork.run(() -> {
            try {
                writeStoredData(key, data);
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
            writeStoredData(key, data);
            failedWrites.remove(key, data);
        }));
    }

    private record LoadedPlayerData(Map<String, String> kits, String inventorySelection, String enderchestSelection) {}

    private String readStoredData(String id) {
        String pending = failedWrites.get(id);
        return pending != null ? pending : PerPlayerKit.storageManager.getKitDataByID(id);
    }

    public void loadPlayerDataAsync(UUID uuid) {
        Object session = new Object();
        sessions.put(uuid, session);
        loading.add(uuid);
        storageWork.supply(() -> {
            Map<String, String> data = new java.util.LinkedHashMap<>();
            for (int slot = 1; slot <= KitSlots.maxKits(); slot++) {
                for (String id : List.of(IDUtil.getPlayerKitId(uuid, slot), IDUtil.getECId(uuid, slot)))
                    data.put(id, readStoredData(id));
            }
            return new LoadedPlayerData(data, readStoredData(KitSelection.kitKey(uuid)), readStoredData(KitSelection.enderchestKey(uuid)));
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
                for (Map.Entry<String, String> entry : data.kits().entrySet()) {
                    String encoded = entry.getValue();
                    if (!isStoredData(encoded)) continue;
                    int size = entry.getKey().startsWith(uuid + "ec") ? KitContents.ENDERCHEST_SIZE : KitContents.INVENTORY_SIZE;
                    try { parsed.put(entry.getKey(), decodeKit(encoded, size)); }
                    catch (IOException e) {
                        plugin.getLogger().warning("Cannot load kit " + entry.getKey() + ": " + e.getMessage());
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null) Lang.get().send(player, "error.kit-data-unavailable");
                        return;
                    }
                }
                parsed.forEach(kitByKitIDMap::putIfAbsent);
                restoreSelections(uuid, data.inventorySelection(), data.enderchestSelection());
                loading.remove(uuid);
            });
        });
    }

    private void restoreSelections(UUID uuid, String inventory, String enderchest) {
        // A corrupt preference must not prevent access to otherwise valid saved kits.
        if (isStoredData(inventory)) {
            try { lastKitUsedByPlayer.putIfAbsent(uuid, KitSelection.decodeKit(inventory)); }
            catch (IllegalArgumentException error) { plugin.getLogger().warning("Cannot restore remembered kit for " + uuid + ": " + error.getMessage()); }
        }
        if (isStoredData(enderchest)) {
            try { lastEnderchest.putIfAbsent(uuid, KitSelection.decodeSlot(enderchest)); }
            catch (IllegalArgumentException error) { plugin.getLogger().warning("Cannot restore remembered enderchest for " + uuid + ": " + error.getMessage()); }
        }
    }

    public void unloadPlayer(UUID uuid) {
        sessions.remove(uuid);
        loading.remove(uuid);
        lastKitUsedByPlayer.remove(uuid);
        lastEnderchest.remove(uuid);
        // Contents and selections are already queued. Quit only clears this session's cache;
        // it must not overwrite a selection saved by another server using the same database.
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
        loadKitEntryFromDB(IDUtil.getPublicKitId(id), KitContents.INVENTORY_SIZE);
    }

    public void deletePublicKit(String id) {
        String key = IDUtil.getPublicKitId(id);
        kitByKitIDMap.remove(key);
        queueWrite(key, DELETED);
    }

    public boolean deleteKit(UUID uuid, int slot) {
        return deleteCachedKit(IDUtil.getPlayerKitId(uuid, slot));
    }

    public boolean deleteEnderchest(UUID uuid, int slot) {
        return deleteCachedKit(IDUtil.getECId(uuid, slot));
    }

    private boolean deleteCachedKit(String id) {
        if (kitByKitIDMap.remove(id) == null) return false;
        queueWrite(id, DELETED);
        return true;
    }

    private void applyKitLoadEffects(Player player, boolean isEnderChest) {
        if (player.isDead()) return;
        String prefix = isEnderChest ? "enderchests.load." : "kits.load.";
        if (plugin.getConfig().getBoolean(prefix + "heal", false)) player.setHealth(player.getMaxHealth());
        if (plugin.getConfig().getBoolean(prefix + "feed", false)) player.setFoodLevel(20);
        if (plugin.getConfig().getBoolean(prefix + "saturate", false)) player.setSaturation(20);
        if (plugin.getConfig().getBoolean(prefix + "clear-effects", false))
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
    }
}
