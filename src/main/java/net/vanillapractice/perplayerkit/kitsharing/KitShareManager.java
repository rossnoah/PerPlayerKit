package net.vanillapractice.perplayerkit.kitsharing;

import net.vanillapractice.perplayerkit.Broadcast;
import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class KitShareManager {

    private static final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    public static void Sharekit(Player p, int slot){
        UUID uuid = p.getUniqueId();
        if(KitManager.hasKit(uuid,slot)){
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if(PerPlayerKit.kitShareData.putIfAbsent(id,KitManager.getKit(uuid,slot).clone())==null){
                p.sendMessage(ChatColor.GREEN+"Use /copykit "+id+" to share your kit");
                p.sendMessage(ChatColor.GREEN+"Code expires in 5 minutes");


                new BukkitRunnable() {

                    @Override
                    public void run() {
                        PerPlayerKit.kitShareData.remove(id);
                    }

                }.runTaskLater(plugin,5*60*20);


            }else{
                p.sendMessage(ChatColor.RED+"Error, please try again (Kit Code Exists");
            }

        }else{
            p.sendMessage(ChatColor.RED+"Error, that kit does not exist");
        }

    }

    public static void copyKit(Player p, String str){

        String id = str.toUpperCase();
        if(PerPlayerKit.kitShareData.containsKey(id)){
            p.getInventory().setContents(PerPlayerKit.kitShareData.get(id).clone());
            Broadcast.bcKitCopy(p);

        }else{
            p.sendMessage(ChatColor.RED+"Error, kit does not exist or has expired");

        }


    }



}
