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
package dev.noah.perplayerkit.commands.inspect;

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
     * and finally searches cached offline players and Mojang asynchronously.
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

        return CompletableFuture.supplyAsync(() -> {
            UUID cachedOfflinePlayer = findCachedOfflinePlayerUuid(identifier);
            if (cachedOfflinePlayer != null) {
                return cachedOfflinePlayer;
            }

            return lookupPlayerUuidFromMojang(identifier);
        });
    }

    /**
     * Gets a player's name from their UUID.
     *
     * @param uuid Player UUID
     * @return Player name, or null if the UUID cannot be associated with a known player name
     */
    public static @Nullable String getPlayerName(@NotNull UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName();
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

    private static @Nullable UUID findCachedOfflinePlayerUuid(@NotNull String identifier) {
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            String name = offlinePlayer.getName();
            if (name != null && name.equalsIgnoreCase(identifier)) {
                return offlinePlayer.getUniqueId();
            }
        }
        return null;
    }

    private static @Nullable UUID lookupPlayerUuidFromMojang(@NotNull String identifier) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.mojang.com/users/profiles/minecraft/" + identifier)
                    .build();
            Response response = client.newCall(request).execute();
            try {
                if (!response.isSuccessful() || response.body() == null) {
                    return null;
                }

                String body = response.body().string();
                int idStart = body.indexOf("\"id\":\"");
                if (idStart < 0) {
                    return null;
                }

                idStart += 6;
                int idEnd = body.indexOf("\"", idStart);
                if (idEnd <= idStart) {
                    return null;
                }

                String raw = body.substring(idStart, idEnd);
                String formatted = raw.substring(0, 8) + "-"
                        + raw.substring(8, 12) + "-"
                        + raw.substring(12, 16) + "-"
                        + raw.substring(16, 20) + "-"
                        + raw.substring(20);
                return UUID.fromString(formatted);
            } finally {
                if (response.body() != null) {
                    response.body().close();
                }
            }
        } catch (IOException | IllegalArgumentException ignored) {
            return null;
        }
    }
}
