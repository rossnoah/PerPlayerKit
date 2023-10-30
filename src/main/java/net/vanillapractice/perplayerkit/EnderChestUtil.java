package net.vanillapractice.perplayerkit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class EnderChestUtil {

    public static void saveEnderChest(Player p){
        ItemStack[] kit = p.getEnderChest().getContents();
        boolean notEmpty = false;
        for (ItemStack i : kit) {
            if (i != null) {
                if (!notEmpty) {
                    notEmpty = true;
                }

            }
        }

        if (notEmpty) {
            UUID uuid = p.getUniqueId();
            PerPlayerKit.data.put(uuid.toString() + "enderchest", Filter.filterItemStack(kit));
            p.sendMessage("§aEnder Chest Saved!");
        }


    }

    public static boolean loadEnderChest(UUID uuid){

        if(Bukkit.getPlayer(uuid)!=null) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {

                if(PerPlayerKit.data.get(uuid.toString()+"enderchest")!=null){
                    player.getEnderChest().setContents(PerPlayerKit.data.get(uuid.toString()+"enderchest"));
                    Broadcast.bcEC(player);
                    player.sendMessage(ChatColor.GREEN+"Ender Chest loaded!");

                    return true;
                }
                else{
                    player.sendMessage(ChatColor.RED+"Ender Chest save does not exist!");
                }
            }
        }
        return false;
    }

}
