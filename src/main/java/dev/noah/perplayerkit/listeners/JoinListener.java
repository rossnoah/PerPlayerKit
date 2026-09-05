/*
 * Copyright 2022-2026 Noah Ross
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
package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.UpdateChecker;
import dev.noah.perplayerkit.starter.StarterSetup;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.Lang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class JoinListener implements Listener {

    private final Plugin plugin;
    private final UpdateChecker updateChecker;

    public JoinListener(Plugin plugin, UpdateChecker updateChecker) {
        this.plugin = plugin;
        this.updateChecker = updateChecker;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player player = e.getPlayer();

        if(player.hasPermission("perplayerkit.admin") && plugin.getConfig().getBoolean("updates.notify-admins-on-join",true)){
            updateChecker.sendUpdateMessage(player);
        }

        // While the server has no kit room yet, offer admins the one command that
        // makes one. Sent after the MOTD so it is the last thing left on screen.
        if (player.hasPermission("perplayerkit.admin") && StarterSetup.get().needsAutoSetup()) {
            long motdDelay = plugin.getConfig().getBoolean("motd.enabled")
                    ? plugin.getConfig().getLong("motd.delay-seconds") + 2
                    : 2;
            Bukkit.getScheduler().runTaskLater(plugin, () -> StarterSetup.get().offerAutoSetup(player), motdDelay * 20L);
        }

        UUID uuid = player.getUniqueId();

        KitManager.get().loadPlayerDataAsync(uuid);

        // Check if MOTD is enabled and send MOTD messages
        if (plugin.getConfig().getBoolean("motd.enabled")) {
            List<Component> motdMessages = new ArrayList<>();
            Lang.get().rawList("motd.message").forEach(message -> motdMessages.add(MiniMessage.miniMessage().deserialize(message)));

            // Delay for sending the MOTD
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (player.isOnline()) motdMessages.forEach(message -> BroadcastManager.get().sendComponentMessage(player,message)); }, plugin.getConfig().getLong("motd.delay-seconds") * 20L);
        }
    }


}


