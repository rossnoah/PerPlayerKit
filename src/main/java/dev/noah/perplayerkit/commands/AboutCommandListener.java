package dev.noah.perplayerkit.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;


public class AboutCommandListener implements Listener {


    private void sendAboutMessage(CommandSender sender) {
        String author = "Noah Ross";
        String source = "https://github.com/rossnoah/PerPlayerKit";
        String license = "AGPL-3.0";


        sender.sendMessage("==========[About]==========");
        sender.sendMessage("PerPlayerKit");
        sender.sendMessage("Author: " + author);
        sender.sendMessage("License: " + license);
        sender.sendMessage("Source Code: " + source);
        sender.sendMessage("===========================");
    }

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.contains("/aboutperplayerkit")) {
            event.setCancelled(true);
            CommandSender sender = event.getPlayer();
            sendAboutMessage(sender);
        }

    }


}
