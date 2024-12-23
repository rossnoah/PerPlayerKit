package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.Broadcast;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.UUID;

public class KitManager {
    private static final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    public static boolean savekit(UUID uuid, int slot) {
        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                return savekit(uuid, slot, player.getInventory().getContents());

            }
        }
        return false;
    }


    public static boolean savekit(UUID uuid, int slot, ItemStack[] kit) {

        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                /*if (PerPlayerKit.data.containsKey(uuid.toString() + slot)) {
                    PerPlayerKit.data.remove(uuid.toString() + slot);
                    player.sendMessage("Kit Cleared (manager)");
                }

                 */

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


                    PerPlayerKit.data.put(uuid.toString() + slot, kit);
                    player.sendMessage(ChatColor.GREEN + "Kit " + slot + " saved!");

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSingleKitToSQL(uuid, slot));
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You cant save an empty kit!");
                }


            }

        }
        return false;

    }

    public static boolean savePublicKit(String id, ItemStack[] kit) {
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

            PerPlayerKit.data.put("public" + id, kit);

            return true;

        }
        return false;
    }

    public static boolean saveEC(UUID uuid, int slot, ItemStack[] kit) {

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

                    PerPlayerKit.data.put(uuid + "ec" + slot, kit);
                    player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " saved!");

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> saveSingleECToSQL(uuid, slot));
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "You cant save an empty enderchest!");
                }


            }

        }
        return false;

    }


    public static boolean savekit(UUID uuid, int slot, ItemStack[] kit, boolean silent) {
        if (silent) {
            if (Bukkit.getPlayer(uuid) != null) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                /*if (PerPlayerKit.data.containsKey(uuid.toString() + slot)) {
                    PerPlayerKit.data.remove(uuid.toString() + slot);
                    player.sendMessage("Kit Cleared (manager)");
                }

                 */

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


                        PerPlayerKit.data.put(uuid.toString() + slot, Filter.filterItemStack(kit));

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


    public static boolean loadkit(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if (PerPlayerKit.data.get(uuid.toString() + slot) != null) {
                    player.getInventory().setContents(PerPlayerKit.data.get(uuid.toString() + slot));
                    Broadcast.get().broadcastPlayerLoadedPrivateKit(player);
                    player.sendMessage(ChatColor.GREEN + "Kit " + slot + " loaded!");
                    PerPlayerKit.lastKit.put(uuid, slot);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Kit " + slot + " does not exist!");
                }
            }
        }
        return false;
    }

    public static boolean loadPublicKit(Player player, String id) {
        if (PerPlayerKit.data.get("public" + id) != null) {
            player.getInventory().setContents(PerPlayerKit.data.get("public" + id));
            Broadcast.get().broadcastPlayerLoadedPublicKit(player);
            player.sendMessage(ChatColor.GREEN + "Public Kit loaded!");
            player.sendMessage(ChatColor.GRAY + "You can save this kit by importing into the kit editor");


            return true;

        } else {
            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

    }

    public static boolean loadPublicKitSilent(Player player, String id) {
        if (PerPlayerKit.data.get("public" + id) != null) {
            player.getInventory().setContents(PerPlayerKit.data.get("public" + id));
//            player.sendMessage(ChatColor.GREEN + "Public Kit loaded!");

            return true;

        } else {
            player.sendMessage(ChatColor.RED + "Kit does not exist!");
            return false;
        }

    }

    public static boolean loadEC(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if (PerPlayerKit.data.get(uuid + "ec" + slot) != null) {

                    ItemStack[] ec = new ItemStack[27];
//                    copy into ec
                    for (int i = 0; i < 27; i++) {
                        if (PerPlayerKit.data.get(uuid + "ec" + slot)[i] != null) {
                            ec[i] = PerPlayerKit.data.get(uuid + "ec" + slot)[i].clone();
                        }
                    }
                    player.getEnderChest().setContents(ec);
                    Broadcast.get().broadcastPlayerLoadedEnderChest(player);
                    player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " loaded!");
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Enderchest " + slot + " does not exist!");
                }
            }
        }
        return false;
    }

    public static boolean respawnKitLoad(UUID uuid, int slot) {

        if (Bukkit.getPlayer(uuid) != null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if (PerPlayerKit.data.get(uuid.toString() + slot) != null) {
                    player.getInventory().setContents(PerPlayerKit.data.get(uuid.toString() + slot));
                    player.sendMessage(ChatColor.GREEN + "Last kit loaded!");
                    PerPlayerKit.lastKit.put(uuid, slot);
                    return true;
                } else {
                    player.sendMessage(ChatColor.RED + "Last used kit does not exist");
                }
            }
        }
        return false;
    }


    public static boolean hasKit(UUID uuid, int slot) {
        return PerPlayerKit.data.get(uuid.toString() + slot) != null;

    }

    public static boolean hasPublicKit(String id) {
        return PerPlayerKit.data.get("public" + id) != null;

    }

    public static ItemStack[] getKit(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            return PerPlayerKit.data.get(uuid.toString() + slot);
        } else {
            return null;
        }

    }

    public static ItemStack[] getPublicKit(String id) {
        if (hasPublicKit(id)) {
            return PerPlayerKit.data.get("public" + id);
        } else {
            return null;
        }

    }


    public static void loadFromSQL(UUID uuid) {
        for (int i = 1; i < 10; i++) {
            String data = PerPlayerKit.dbManager.getMySQLKit(uuid.toString() + i);
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    PerPlayerKit.data.put(uuid.toString() + i, Filter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
        for (int i = 1; i < 10; i++) {
            String data = PerPlayerKit.dbManager.getMySQLKit(uuid + "ec" + i);
            if (!data.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                    PerPlayerKit.data.put(uuid + "ec" + i, Filter.filterItemStack(Serializer.itemStackArrayFromBase64(data)));

                } catch (IOException ignored) {
                }
            }
        }
    }

    public static void saveToSQL(UUID uuid) {
        for (int i = 1; i < 10; i++) {
            if (PerPlayerKit.data.get(uuid.toString() + i) != null) {
                PerPlayerKit.dbManager.saveMySQLKit(uuid.toString() + i, Serializer
                        .itemStackArrayToBase64(PerPlayerKit.data.get(uuid.toString() + i)));
                PerPlayerKit.data.remove(uuid.toString() + i);
            }
        }
        if (PerPlayerKit.data.get(uuid + "enderchest") != null) {
            PerPlayerKit.dbManager.saveMySQLKit(uuid + "enderchest", Serializer
                    .itemStackArrayToBase64(PerPlayerKit.data.get(uuid + "enderchest")));
            PerPlayerKit.data.remove(uuid + "enderchest");
        }
    }

    public static void saveSingleKitToSQL(UUID uuid, int slot) {
        if (PerPlayerKit.data.get(uuid.toString() + slot) != null) {
            PerPlayerKit.dbManager.saveMySQLKit(uuid.toString() + slot, Serializer
                    .itemStackArrayToBase64(Filter.filterItemStack(PerPlayerKit.data.get(uuid.toString() + slot))));
        }
    }

    public static void saveSinglePublicKitToSQL(String id) {
        if (PerPlayerKit.data.get("public" + id) != null) {
            PerPlayerKit.dbManager.saveMySQLKit("public" + id, Serializer
                    .itemStackArrayToBase64(PerPlayerKit.data.get("public" + id)));
        }
    }

    public static void loadSinglePublicKitFromSQL(String id) {
        String data = PerPlayerKit.dbManager.getMySQLKit("public" + id);
        if (!data.equalsIgnoreCase("error")) {
            try {
                ItemStack[] kit = Serializer.itemStackArrayFromBase64(data);
                PerPlayerKit.data.put("public" + id, Filter.filterItemStack(kit));

            } catch (IOException ignored) {
                PerPlayerKit.getPlugin().getLogger().info("Error loading public kit " + id);
            }
        }
    }

    public static void saveSingleECToSQL(UUID uuid, int slot) {
        if (PerPlayerKit.data.get(uuid.toString() + "ec" + slot) != null) {
            PerPlayerKit.dbManager.saveMySQLKit(uuid + "ec" + slot, Serializer
                    .itemStackArrayToBase64(Filter.filterItemStack(PerPlayerKit.data.get(uuid + "ec" + slot))));
        }
    }


    public static boolean deleteKitAll(UUID uuid, int slot) {
        if (hasKit(uuid, slot)) {
            String kitid = uuid.toString() + slot;
            PerPlayerKit.data.remove(kitid);
            new BukkitRunnable() {

                @Override
                public void run() {
                    PerPlayerKit.dbManager.deleteKitSQL(kitid);
                }
            }.runTaskAsynchronously(plugin);
            return true;
        }
        return false;
    }


}
