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

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import dev.noah.perplayerkit.util.BroadcastManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class InspectCommandUtil {
    public static final int MIN_SLOT = 1;
    public static final int MAX_SLOT = 9;
    public static final MiniMessage mm = MiniMessage.miniMessage();
    public static final Component ERROR_PREFIX = mm.deserialize("<red>Error:</red> ");

    private InspectCommandUtil() {
        // Utility class
    }

    /**
     * Attempts to resolve a player identifier (name or UUID) to a UUID asynchronously.
     * This method first tries to parse as UUID, then checks online players synchronously,
     * and finally searches offline players asynchronously.
     *
     * @param identifier Player name or UUID string
     * @return CompletableFuture containing UUID if found, null otherwise
     */
    public static CompletableFuture<UUID> resolvePlayerIdentifierAsync(String identifier) {
        // First try to parse as UUID
        try {
            UUID uuid = UUID.fromString(identifier);
            return CompletableFuture.completedFuture(uuid);
        } catch (IllegalArgumentException ignored) {
            // Not a UUID, continue
        }

        // Try to find online player (this is fast and safe to do synchronously)
        Player onlinePlayer = Bukkit.getPlayerExact(identifier);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(onlinePlayer.getUniqueId());
        }

        // Look up UUID via Mojang API (avoids the very slow Bukkit.getOfflinePlayers() scan)
        return CompletableFuture.supplyAsync(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url("https://api.mojang.com/users/profiles/minecraft/" + identifier)
                        .build();
                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String body = response.body().string();
                        // Parse the "id" field: {"id":"<uuid-no-dashes>","name":"<name>"}
                        int idStart = body.indexOf("\"id\":\"") + 6;
                        int idEnd = body.indexOf("\"", idStart);
                        if (idStart > 5 && idEnd > idStart) {
                            String raw = body.substring(idStart, idEnd);
                            // Insert dashes into the 32-char UUID string
                            String formatted = raw.substring(0, 8) + "-"
                                    + raw.substring(8, 12) + "-"
                                    + raw.substring(12, 16) + "-"
                                    + raw.substring(16, 20) + "-"
                                    + raw.substring(20);
                            return UUID.fromString(formatted);
                        }
                    }
                }
            } catch (IOException ignored) {
                // Fall through to offline UUID computation
            }
            // Fallback for offline/cracked-mode servers: compute deterministic offline UUID
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + identifier).getBytes(StandardCharsets.UTF_8));
        });
    }

    /**
     * Gets a player's name from their UUID, falling back to UUID string if name is not available.
     *
     * @param uuid Player UUID
     * @return Player name or UUID string
     */
    public static String getPlayerName(@NotNull UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        String name = offlinePlayer.getName();
        return name != null ? name : uuid.toString();
    }

    /**
     * Shows command usage message to the player.
     *
     * @param player Player to send message to
     * @param commandName Name of the command (e.g., "inspectkit" or "inspectec")
     */
    public static void showUsage(@NotNull Player player, @NotNull String commandName) {
        BroadcastManager.get().sendComponentMessage(player,
                ERROR_PREFIX.append(
                        mm.deserialize("<red>Usage: /" + commandName + " <player|uuid> <slot></red>")));
    }
}
