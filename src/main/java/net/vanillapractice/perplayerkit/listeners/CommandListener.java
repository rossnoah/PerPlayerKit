package net.vanillapractice.perplayerkit.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener implements Listener {


    @EventHandler
    public void onEvent(PlayerCommandPreprocessEvent e){

        if(e.getMessage().length()>1){
            if(e.getMessage().contains("/ ")){
                e.setCancelled(true);
                e.getPlayer().sendMessage("Unknown Command.");

            }
        }
    }





}
