package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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


                    kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid,slot), kit);
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

                    kitByKitIDMap.put(IDUtil.getECId(uuid,slot), kit);
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


                        kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid,slot), ItemFilter.filterItemStack(kit));

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


    public boolean loadkit(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) == null) {
            return false;
        }

        Player player = Bukkit.getPlayer(uuid);

        if (player == null) {
            return false;
        }

        if (kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot)) == null) {
            player.sendMessage(ChatColor.RED + "Kit " + slot + " does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot)));
        BroadcastManager.get().broadcastPlayerLoadedPrivateKit(player);
        player.sendMessage(ChatColor.GREEN + "Kit " + slot + " loaded!");
        lastKitUsedByPlayer.put(uuid, slot);
        return true;

    }

    public boolean loadPublicKit(Player player, String id) {
        if (kitByKitIDMap.get(IDUtil.getPublicKitId(id)) == null) {

            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get(IDUtil.getPublicKitId(id)));
        BroadcastManager.get().broadcastPlayerLoadedPublicKit(player);
        player.sendMessage(ChatColor.GREEN + "Public Kit loaded!");
        player.sendMessage(ChatColor.GRAY + "You can save this kit by importing into the kit editor");
        return true;
    }

    public boolean loadPublicKitSilent(Player player, String id) {
        if (kitByKitIDMap.get(IDUtil.getPublicKitId(id)) == null) {
            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get(IDUtil.getPublicKitId(id)));
        return true;
    }

    public boolean loadEC(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {

            return false;
        }

        if (kitByKitIDMap.get(IDUtil.getECId(uuid,slot)) == null) {


            player.sendMessage(ChatColor.RED + "Enderchest " + slot + " does not exist!");
            return false;
        }
        ItemStack[] ec = new ItemStack[27];
//                    copy into ec
        for (int i = 0; i < 27; i++) {
            if (kitByKitIDMap.get(IDUtil.getECId(uuid,slot))[i] != null) {
                ec[i] = kitByKitIDMap.get(IDUtil.getECId(uuid,slot))[i].clone();
            }
        }
        player.getEnderChest().setContents(ec);
        BroadcastManager.get().broadcastPlayerLoadedEnderChest(player);
        player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " loaded!");
        return true;

    }

    public boolean respawnKitLoad(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) == null) {
            return false;
        }
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return false;
        }

        if (kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot)) == null) {
            player.sendMessage(ChatColor.RED + "Last used kit does not exist");
            return false;

        }

        player.getInventory().setContents(kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot)));
        player.sendMessage(ChatColor.GREEN + "Last kit loaded!");
        lastKitUsedByPlayer.put(uuid, slot);
        return true;

    }


    public boolean hasKit(UUID uuid, int slot) {
        return kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot)) != null;

    }

    public boolean hasPublicKit(String id) {
        return kitByKitIDMap.get(IDUtil.getPublicKitId(id)) != null;

    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            return kitByKitIDMap.get(IDUtil.getPlayerKitId(uuid,slot));
        } else {
            return null;
        }

    }

    public ItemStack[] getPublicKit(String id) {
        if (hasPublicKit(id)) {
            return kitByKitIDMap.get(IDUtil.getPublicKitId(id));
        } else {
            return null;
        }

    }


    public void loadPlayerDataFromDB(UUID uuid) {
        for (int slot = 1; slot < 10; slot++) {
            String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getPlayerKitId(uuid,slot));
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(IDUtil.getPlayerKitId(uuid,slot), ItemFilter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
        for (int slot = 1; slot < 10; slot++) {
            String data = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getECId(uuid,slot));
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(IDUtil.getECId(uuid,slot), ItemFilter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
    }

    public void unloadPlayerData(UUID uuid) {
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

            PerPlayerKit.storageManager.saveKitDataByID(key, Serializer.itemStackArrayToBase64(ItemFilter.filterItemStack(kitByKitIDMap.get(key))));

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
                kitByKitIDMap.put(IDUtil.getPublicKitId(id), ItemFilter.filterItemStack(kit));

            } catch (IOException ignored) {
                PerPlayerKit.getPlugin().getLogger().info("Error loading public kit " + id);
            }
        }
    }


    public boolean deleteKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            kitByKitIDMap.remove(IDUtil.getPlayerKitId(uuid,slot));
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> PerPlayerKit.storageManager.deleteKitByID(IDUtil.getPlayerKitId(uuid,slot)));
            return true;
        }
        return false;
    }


}
