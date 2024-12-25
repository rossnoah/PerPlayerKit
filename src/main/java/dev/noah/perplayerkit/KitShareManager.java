package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.BroadcastManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.UUID;

public class KitShareManager {



    public static HashMap<String, ItemStack[]> kitShareMap;

    private final Plugin plugin;
    private static KitShareManager instance;

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

    public void sharekit(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasKit(uuid, slot)) {
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if (kitShareMap.putIfAbsent(id, kitManager.getPlayerKit(uuid, slot).clone()) == null) {
                p.sendMessage(ChatColor.GREEN + "Use /copykit " + id + " to share your kit");
                p.sendMessage(ChatColor.GREEN + "Code expires in 15 minutes");


                new BukkitRunnable() {

                    @Override
                    public void run() {
                        kitShareMap.remove(id);
                    }

                }.runTaskLater(plugin, 15 * 60 * 20);


            } else {
                p.sendMessage(ChatColor.RED + "Error, please try again (Kit Code Exists");
            }

        } else {
            p.sendMessage(ChatColor.RED + "Error, that kit does not exist");
        }

    }

    public void copyKit(Player p, String str) {

        String id = str.toUpperCase();
        if (kitShareMap.containsKey(id)) {
            p.getInventory().setContents(kitShareMap.get(id).clone());
            BroadcastManager.get().broadcastPlayerCopiedKit(p);
        } else {
            p.sendMessage(ChatColor.RED + "Error, kit does not exist or has expired");

        }


    }


}
