package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.Broadcast;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

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


                    kitByKitIDMap.put(uuid.toString() + slot, kit);
                    player.sendMessage(ChatColor.GREEN + "Kit " + slot + " saved!");

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveKitToDB(uuid, slot));
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

            kitByKitIDMap.put("public" + id, kit);

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

                    kitByKitIDMap.put(uuid + "ec" + slot, kit);
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


                        kitByKitIDMap.put(uuid.toString() + slot, Filter.filterItemStack(kit));

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

        if (kitByKitIDMap.get(uuid.toString() + slot) == null) {
            player.sendMessage(ChatColor.RED + "Kit " + slot + " does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get(uuid.toString() + slot));
        Broadcast.get().broadcastPlayerLoadedPrivateKit(player);
        player.sendMessage(ChatColor.GREEN + "Kit " + slot + " loaded!");
        lastKitUsedByPlayer.put(uuid, slot);
        return true;

    }

    public boolean loadPublicKit(Player player, String id) {
        if (kitByKitIDMap.get("public" + id) == null) {

            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get("public" + id));
        Broadcast.get().broadcastPlayerLoadedPublicKit(player);
        player.sendMessage(ChatColor.GREEN + "Public Kit loaded!");
        player.sendMessage(ChatColor.GRAY + "You can save this kit by importing into the kit editor");
        return true;
    }

    public boolean loadPublicKitSilent(Player player, String id) {
        if (kitByKitIDMap.get("public" + id) == null) {
            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

        player.getInventory().setContents(kitByKitIDMap.get("public" + id));
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

        if (kitByKitIDMap.get(uuid + "ec" + slot) == null) {


            player.sendMessage(ChatColor.RED + "Enderchest " + slot + " does not exist!");
            return false;
        }
        ItemStack[] ec = new ItemStack[27];
//                    copy into ec
        for (int i = 0; i < 27; i++) {
            if (kitByKitIDMap.get(uuid + "ec" + slot)[i] != null) {
                ec[i] = kitByKitIDMap.get(uuid + "ec" + slot)[i].clone();
            }
        }
        player.getEnderChest().setContents(ec);
        Broadcast.get().broadcastPlayerLoadedEnderChest(player);
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

        if (kitByKitIDMap.get(uuid.toString() + slot) == null) {
            player.sendMessage(ChatColor.RED + "Last used kit does not exist");
            return false;

        }

        player.getInventory().setContents(kitByKitIDMap.get(uuid.toString() + slot));
        player.sendMessage(ChatColor.GREEN + "Last kit loaded!");
        lastKitUsedByPlayer.put(uuid, slot);
        return true;

    }


    public boolean hasKit(UUID uuid, int slot) {
        return kitByKitIDMap.get(uuid.toString() + slot) != null;

    }

    public boolean hasPublicKit(String id) {
        return kitByKitIDMap.get("public" + id) != null;

    }

    public ItemStack[] getPlayerKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            return kitByKitIDMap.get(uuid.toString() + slot);
        } else {
            return null;
        }

    }

    public ItemStack[] getPublicKit(String id) {
        if (hasPublicKit(id)) {
            return kitByKitIDMap.get("public" + id);
        } else {
            return null;
        }

    }


    public void loadPlayerKitsFromDB(UUID uuid) {
        for (int i = 1; i < 10; i++) {
            String data = PerPlayerKit.dbManager.getKitDataByID(uuid.toString() + i);
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(uuid.toString() + i, Filter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
        for (int i = 1; i < 10; i++) {
            String data = PerPlayerKit.dbManager.getKitDataByID(uuid + "ec" + i);
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    kitByKitIDMap.put(uuid + "ec" + i, Filter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
    }

    public void savePlayerKitsToDB(UUID uuid) {
        for (int i = 1; i < 10; i++) {
            if (kitByKitIDMap.get(uuid.toString() + i) != null) {
                PerPlayerKit.dbManager.saveKitDataByID(uuid.toString() + i, Serializer.itemStackArrayToBase64(kitByKitIDMap.get(uuid.toString() + i)));
                kitByKitIDMap.remove(uuid.toString() + i);
            }
        }
        if (kitByKitIDMap.get(uuid + "enderchest") != null) {
            PerPlayerKit.dbManager.saveKitDataByID(uuid + "enderchest", Serializer.itemStackArrayToBase64(kitByKitIDMap.get(uuid + "enderchest")));
            kitByKitIDMap.remove(uuid + "enderchest");
        }
    }

    public void saveKitToDB(UUID uuid, int slot) {
        if (kitByKitIDMap.get(uuid.toString() + slot) != null) {
            PerPlayerKit.dbManager.saveKitDataByID(uuid.toString() + slot, Serializer.itemStackArrayToBase64(Filter.filterItemStack(kitByKitIDMap.get(uuid.toString() + slot))));
        }
    }

    public void saveEnderchestToDB(UUID uuid, int slot) {
        if (kitByKitIDMap.get(uuid.toString() + "ec" + slot) != null) {
            PerPlayerKit.dbManager.saveKitDataByID(uuid + "ec" + slot, Serializer.itemStackArrayToBase64(Filter.filterItemStack(kitByKitIDMap.get(uuid + "ec" + slot))));
        }
    }


    public void savePublicKitToDB(String id) {
        if (kitByKitIDMap.get("public" + id) != null) {
            PerPlayerKit.dbManager.saveKitDataByID("public" + id, Serializer.itemStackArrayToBase64(kitByKitIDMap.get("public" + id)));
        }
    }

    public void loadPublicKitFromDB(String id) {
        String data = PerPlayerKit.dbManager.getKitDataByID("public" + id);
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                kitByKitIDMap.put("public" + id, Filter.filterItemStack(kit));

            } catch (IOException ignored) {
                PerPlayerKit.getPlugin().getLogger().info("Error loading public kit " + id);
            }
        }
    }


    public boolean deleteKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            String kitid = uuid.toString() + slot;
            kitByKitIDMap.remove(kitid);
            new BukkitRunnable() {

                @Override
                public void run() {
                    PerPlayerKit.dbManager.deleteKitByID(kitid);
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }
        return false;
    }


}
