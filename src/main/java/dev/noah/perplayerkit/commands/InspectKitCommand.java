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
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.util.BroadcastManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.noah.perplayerkit.util.SoundManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class InspectKitCommand implements CommandExecutor, TabCompleter {
    private static final int MIN_SLOT = 1;
    private static final int MAX_SLOT = 9;
    private static final MiniMessage mm = MiniMessage.miniMessage();
    private static final Component ERROR_PREFIX = mm.deserialize("<red>Error:</red> ");

    private final Plugin plugin;

    public InspectKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ERROR_PREFIX.append(
                    mm.deserialize("<red>This command can only be executed by players.</red>")).toString());
            return true;
        }

        if (!player.hasPermission("perplayerkit.inspect")) {
            BroadcastManager.get().sendComponentMessage(player,
                    ERROR_PREFIX.append(
                            mm.deserialize("<red>You don't have permission to use this command.</red>")));
            SoundManager.playFailure(player);
            return true;
        }

        if (args.length < 2) {
            showUsage(player);
            return true;
        }

        // Try to resolve UUID from player name or UUID string
        UUID targetUuid = resolvePlayerIdentifier(args[0]);
        if (targetUuid == null) {
            BroadcastManager.get().sendComponentMessage(player,
                    ERROR_PREFIX.append(
                            mm.deserialize("<red>Could not find a player with that name or UUID.</red>")));
            SoundManager.playFailure(player);
            return true;
        }

        // Parse slot number
        int slot;
        try {
            slot = Integer.parseInt(args[1]);
            if (slot < MIN_SLOT || slot > MAX_SLOT) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            BroadcastManager.get().sendComponentMessage(player,
                    ERROR_PREFIX.append(
                            mm.deserialize("<red>Slot must be a number between " +
                                    MIN_SLOT + " and " + MAX_SLOT + ".</red>")));
            SoundManager.playFailure(player);
            return true;
        }

        // Check if player is online first
        Player targetPlayer = Bukkit.getPlayer(targetUuid);

        // Load player data asynchronously
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            if (targetPlayer == null) {
                // Only load from DB if player is offline
                KitManager.get().loadPlayerDataFromDB(targetUuid);
            }
        }).thenRun(() -> {
            // Run on the main thread after data is loaded
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (KitManager.get().hasKit(targetUuid, slot)) {
                    GUI.get().InspectKit(player, targetUuid, slot);
                } else {
                    String targetName = getPlayerName(targetUuid);

                    BroadcastManager.get().sendComponentMessage(player,
                            ERROR_PREFIX.append(
                                    mm.deserialize("<red>" + targetName +
                                            " does not have a kit in slot " + slot + "</red>")));
                    SoundManager.playFailure(player);
                }
            });
        });

        // Handle exceptions
        future.exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().severe("Error loading kit data: " + ex.getMessage());
                BroadcastManager.get().sendComponentMessage(player,
                        ERROR_PREFIX.append(
                                mm.deserialize("<red>An error occurred while loading kit data. " +
                                        "See console for details.</red>")));
                SoundManager.playFailure(player);
            });
            return null;
        });

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player) || !sender.hasPermission("perplayerkit.inspect")) {
            return List.of();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            // Add online player names
            List<String> completions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList());

            // Add UUIDs if the input looks like it might be a UUID
            if (input.length() >= 4 && input.contains("-")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getUniqueId)
                        .map(UUID::toString)
                        .filter(uuid -> uuid.startsWith(input))
                        .toList());
            }

            return completions;
        } else if (args.length == 2) {
            // Return slot numbers for second argument
            return IntStream.rangeClosed(MIN_SLOT, MAX_SLOT)
                    .mapToObj(String::valueOf)
                    .filter(slot -> slot.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    /**
     * Attempts to resolve a player identifier (name or UUID) to a UUID.
     *
     * @param identifier Player name or UUID string
     * @return UUID if found, null otherwise
     */
    private UUID resolvePlayerIdentifier(String identifier) {
        // First try to parse as UUID
        try {
            return UUID.fromString(identifier);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, try as player name
        }

        // Try to find online player
        Player onlinePlayer = Bukkit.getPlayerExact(identifier);
        if (onlinePlayer != null) {
            return onlinePlayer.getUniqueId();
        }

        // Try to find offline player
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (identifier.equalsIgnoreCase(offlinePlayer.getName())) {
                return offlinePlayer.getUniqueId();
            }
        }

        // Could not resolve
        return null;
    }

    /**
     * Gets a player's name from their UUID, falling back to UUID string if name is
     * not available.
     *
     * @param uuid Player UUID
     * @return Player name or UUID string
     */
    private String getPlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }

    private void showUsage(Player player) {
        BroadcastManager.get().sendComponentMessage(player,
                ERROR_PREFIX.append(
                        mm.deserialize("<red>Usage: /inspectkit <player|uuid> <slot></red>")));
    }
}
