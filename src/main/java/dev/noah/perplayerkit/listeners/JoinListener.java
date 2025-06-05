/*
 * Copyright 2022-2025 Noah Ross
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
import dev.noah.perplayerkit.util.BroadcastManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import dev.noah.perplayerkit.ConfigManager;

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

        if (player.hasPermission("perplayerkit.admin") && ConfigManager.get().isSendUpdateMessageOnJoinEnabled()) {
            updateChecker.sendUpdateMessage(player);
        }

        UUID uuid = player.getUniqueId();

        // KitManager.loadFromSQL(uuid);

        // async to avoid blocking the login process
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            KitManager.get().loadPlayerDataFromDB(uuid);
        });

        // MOTD
        if (ConfigManager.get().isMOTDEnabled()) {
            List<Component> motdMessages = new ArrayList<>();
            ConfigManager.get().getMOTDMessage()
                    .forEach(message -> motdMessages.add(MiniMessage.miniMessage().deserialize(message)));

            Bukkit.getScheduler().runTaskLater(plugin,
                    () -> motdMessages.forEach(message -> BroadcastManager.get().sendComponentMessage(player, message)),
                    ConfigManager.get().getMOTDDelay() * 20L);
        }
    }

}
