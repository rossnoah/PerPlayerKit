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
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.*;

public class KitManager {
    private static KitManager instance;
    private final PerPlayerKit plugin;
    private final HashMap<String, ItemStack[]> kitByKitIDMap;
    private final HashMap<UUID, Integer> lastKitUsedByPlayer;
    private final List<PublicKit> publicKitList;

    public KitManager(PerPlayerKit plugin) {
        this.plugin = plugin;
        lastKitUsedByPlayer = new HashMap<>();
        publicKitList = new ArrayList<>();
        kitByKitIDMap = new HashMap<>();
        instance = this;
    }

    public static KitManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitManager not initialized");
        }
        return instance;
    }

    public ItemStack[] getItemStackArrayById(String id) {
        return kitByKitIDMap.get(id);
    }

    public List<PublicKit> getPublicKitList() {
        return publicKitList;
    }

    public int getLastKitLoaded(UUID uuid) {
        if (lastKitUsedByPlayer.containsKey(uuid)) {
            return lastKitUsedByPlayer.get(uuid);
        }
        return -1;
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit) {
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

                    kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), kit);
                    player.sendMessage(ChatColor.GREEN + "Kit " + slot + " saved!");

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePlayerKitToDB(uuid, slot));
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You cant save an empty kit!");
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

            kitByKitIDMap.put(IDUtil.getPublicKitId(publickit), kit);
            player.sendMessage(ChatColor.GREEN + "Public Kit " + publickit + " saved!");

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> savePublicKitToDB(publickit));
            return true;
        } else {
            player.sendMessage(ChatColor.RED + "You cant save an empty kit!");
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

            kitByKitIDMap.put(IDUtil.getPublicKitId(id), kit);
            return true;
        }
        return false;
    }

    public boolean saveEC(UUID uuid, int slot, ItemStack[] kit) {
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
                    kitByKitIDMap.put(IDUtil.getECId(uuid, slot), kit);
                    player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " saved!");
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveEnderchestToDB(uuid, slot));
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You cant save an empty enderchest!");
                }
            }
        }
        return false;
    }

    public boolean savekit(UUID uuid, int slot, ItemStack[] kit, boolean silent) {
        if (silent) {
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

                        kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), ItemFilter.get().filterItemStack(kit));
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "You cant save an empty kit!");
                    }
                }
            }
            return false;
        } else {
            return savekit(uuid, slot, kit);
        }
    }

    public boolean regearKit(Player player, int slot) {
        UUID uuid = player.getUniqueId();
        if (kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)) == null) {
            return false;
        }

        boolean invertWhitelist = plugin.getConfig().getBoolean("regear.invert-whitelist", false);
        Set<String> whitelist = new HashSet<>(plugin.getConfig().getStringList("regear.whitelist"));

        ItemStack[] kit = kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot));
        ItemStack[] playerInventory = player.getInventory().getContents();
        for (int i = 0; i < playerInventory.length; i++) {
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

            if (playerInventory[i] == null || playerInventory[i].getType().isAir() || playerInventory[i].getType() == kit[i].getType()) {
                playerInventory[i] = kit[i];
                continue;
            }
        }
        player.getInventory().setContents(playerInventory);
        return true;
    }

    private boolean loadKitInternal(Player player, String kitId, String notFoundMessage, boolean isEnderChest, Runnable afterLoad) {
        if (player == null) {
            return false;
        }

        ItemStack[] kit = kitByKitIDMap.get(kitId);
        if (kit == null) {
            if (notFoundMessage != null) {
                player.sendMessage(ChatColor.RED + notFoundMessage);
            }
            return false;
        }

        if (isEnderChest) {
            player.getEnderChest().setContents(kit);
        } else {
            player.getInventory().setContents(kit);
        }

        if (afterLoad != null) {
            afterLoad.run();
        }

        applyKitLoadEffects(player, isEnderChest);
        return true;
    }

    public boolean loadKit(Player player, int slot) {
        return loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot), "Kit " + slot + " does not exist!", false, () -> {
            BroadcastManager.get().broadcastPlayerLoadedPrivateKit(player);
            player.sendMessage(ChatColor.GREEN + "Kit " + slot + " loaded!");
            lastKitUsedByPlayer.put(player.getUniqueId(), slot);
        });
    }

    public boolean loadKitSilent(Player player, int slot) {
        return loadKitInternal(player, IDUtil.getPlayerKitId(player.getUniqueId(), slot), null, false, null);
    }

    public boolean loadPublicKit(Player player, String id) {
        return loadKitInternal(player, IDUtil.getPublicKitId(id), "Kit does not exist!", false, () -> {
            BroadcastManager.get().broadcastPlayerLoadedPublicKit(player);
            player.sendMessage(ChatColor.GREEN + "Public Kit loaded!");
            player.sendMessage(ChatColor.GRAY + "You can save this kit by importing into the kit editor");
        });
    }

    public boolean loadPublicKitSilent(Player player, String id) {
        return loadKitInternal(player, IDUtil.getPublicKitId(id), null, false, null);
    }

    public boolean loadEnderchest(Player player, int slot) {
        return loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot), "Enderchest " + slot + " does not exist!", true, () -> {
            BroadcastManager.get().broadcastPlayerLoadedEnderChest(player);
            player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " loaded!");
        });
    }

    public boolean loadEnderchestSilent(Player player, int slot) {
        return loadKitInternal(player, IDUtil.getECId(player.getUniqueId(), slot), null, true, null);
    }

    public boolean loadLastKit(Player player) {
        if (lastKitUsedByPlayer.containsKey(player.getUniqueId())) {
            return loadKit(player, lastKitUsedByPlayer.get(player.getUniqueId()));
        }
        return false;
    }

    public boolean hasKit(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot)) != null;
    }

    public boolean hasEC(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getECId(uuid, slot)) != null;
    }

    public ItemStack[] getPlayerEC(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getECId(uuid, slot));
    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid, slot));
    }

    public boolean hasPublicKit(String id) {
        return kitByKitIDMap.get(IDUtil.getPublicKitId(id)) != null;
    }

    public ItemStack[] getPublicKit(String id) {
        return kitByKitIDMap.get(IDUtil.getPublicKitId(id));
    }

    public void loadPlayerDataFromDB(UUID uuid) {
        for (int slot = 1; slot < 10; slot++) {
            String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPlayerKitId(uuid, slot));
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid, slot), ItemFilter.get().filterItemStack(Serializer.itemStackArrayFromBase64(data)));
                } catch (IOException ignored) {
                }
            }
        }
        for (int slot = 1; slot < 10; slot++) {
            String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getECId(uuid, slot));
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(IDUtil.getECId(uuid, slot), ItemFilter.get().filterItemStack(Serializer.itemStackArrayFromBase64(data)));
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void savePlayerKitsToDB(UUID uuid) {
        for (int i = 1; i < 10; i++) {
            saveKitToDB(IDUtil.getPlayerKitId(uuid, i), true);
            saveKitToDB(IDUtil.getECId(uuid, i), true);
        }
    }

    public void savePlayerKitToDB(UUID uuid, int slot) {
        saveKitToDB(IDUtil.getPlayerKitId(uuid, slot), false);
    }

    public void saveEnderchestToDB(UUID uuid, int slot) {
        saveKitToDB(IDUtil.getECId(uuid, slot), false);
    }

    public void savePublicKitToDB(String id) {
        saveKitToDB(IDUtil.getPublicKitId(id), false);
    }

    private void saveKitToDB(String key, boolean removeAfterSave) {
        if (kitByKitIDMap.get(key) != null) {
            PerPlayerKit.storageManager.saveKitDataByID(key, Serializer.itemStackArrayToBase64(ItemFilter.get().filterItemStack(kitByKitIDMap.get(key))));
            if (removeAfterSave) {
                kitByKitIDMap.remove(key);
            }
        }
    }

    public void loadPublicKitFromDB(String id) {
        String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPublicKitId(id));
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                kitByKitIDMap.put(IDUtil.getPublicKitId(id), ItemFilter.get().filterItemStack(kit));
            } catch (IOException ignored) {
                plugin.getLogger().info("Error loading public kit " + id);
            }
        }
    }

    public boolean deleteKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            kitByKitIDMap.remove(IDUtil.getPlayerKitId(uuid, slot));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> PerPlayerKit.storageManager.deleteKitByID(IDUtil.getPlayerKitId(uuid, slot)));
            return true;
        }
        return false;
    }

    public boolean deleteEnderchest(UUID uuid, int slot) {
        if (hasEC(uuid, slot)) {
            kitByKitIDMap.remove(IDUtil.getECId(uuid, slot));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> PerPlayerKit.storageManager.deleteKitByID(IDUtil.getECId(uuid, slot)));
            return true;
        }
        return false;
    }

    private void applyKitLoadEffects(Player player, boolean isEnderChest) {
        if (player.isDead()) {
            return;
        }

        if (isEnderChest) {
            if (plugin.getConfig().getBoolean("feature.heal-on-enderchest-load", false)) {
                player.setHealth(20);
            }
            if (plugin.getConfig().getBoolean("feature.feed-on-enderchest-load", false)) {
                player.setFoodLevel(20);
            }
            if (plugin.getConfig().getBoolean("feature.set-saturation-on-enderchest-load", false)) {
                player.setSaturation(20);
            }
            if (plugin.getConfig().getBoolean("feature.remove-potion-effects-on-enderchest-load", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        } else {
            if (plugin.getConfig().getBoolean("feature.set-health-on-kit-load", false)) {
                player.setHealth(20);
            }
            if (plugin.getConfig().getBoolean("feature.set-hunger-on-kit-load", false)) {
                player.setFoodLevel(20);
            }
            if (plugin.getConfig().getBoolean("feature.set-saturation-on-kit-load", false)) {
                player.setSaturation(20);
            }
            if (plugin.getConfig().getBoolean("feature.remove-potion-effects-on-kit-load", false)) {
                player.getActivePotionEffects().forEach(potionEffect -> player.removePotionEffect(potionEffect.getType()));
            }
        }
    }
}