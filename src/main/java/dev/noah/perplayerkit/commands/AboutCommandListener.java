package dev.noah.perplayerkit.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AboutCommandListener implements Listener {

    private final Properties buildProperties = new Properties();

    public AboutCommandListener() {
        try (InputStream input = getClass().getResourceAsStream("/build.properties")) {
            if (input != null) {
                buildProperties.load(input);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void sendAboutMessage(CommandSender sender) {
        String author = "Noah Ross";
        String source = "https://github.com/rossnoah/PerPlayerKit";
        String license = "AGPL-3.0";

        String buildTimestamp = buildProperties.getProperty("build.timestamp", "Unknown");
        String pluginVersion = buildProperties.getProperty("plugin.version", "Unknown");


        sender.sendMessage("==========[About]==========");
        sender.sendMessage("PerPlayerKit");
        sender.sendMessage("Author: " + author);
        sender.sendMessage("License: " + license);
        sender.sendMessage("Source Code: " + source);
        sender.sendMessage("Version: " + pluginVersion);
        sender.sendMessage("Build Time: " + buildTimestamp);
        sender.sendMessage("===========================");
    }

    @EventHandler
    public void onPreCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.equalsIgnoreCase("/aboutperplayerkit")) {
            event.setCancelled(true);
            CommandSender sender = event.getPlayer();
            sendAboutMessage(sender);
        }
    }
}
