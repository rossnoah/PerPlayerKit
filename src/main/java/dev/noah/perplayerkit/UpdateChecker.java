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
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.Lang;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.entity.Player;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/** Shares asynchronous lookups between startup and joins; player messages run on the server thread. */
public final class UpdateChecker implements AutoCloseable {
    private static final String UPDATE_URL = "https://hangar.papermc.io/api/v1/projects/PerPlayerKit/latestrelease";
    private static final long SUCCESS_CACHE_NANOS = TimeUnit.HOURS.toNanos(1);
    private static final long FAILURE_CACHE_NANOS = TimeUnit.MINUTES.toNanos(1);
    private static final Pattern RELEASE_VERSION = Pattern.compile("\\d+(?:\\.\\d+)*");
    private static final Pattern CURRENT_VERSION = Pattern.compile("(\\d+(?:\\.\\d+)*)([-+].+)?");

    private final Plugin plugin;
    private final String currentVersion;
    private final OkHttpClient client;
    private final Request request;
    private final LongSupplier clock;
    private volatile boolean closed;
    // Accessed under this instance's lock. Futures are completed outside the lock.
    private CompletableFuture<Optional<Release>> pending;
    private Call activeCall;
    private Release cached;
    private long checkedAt;
    private long cacheDuration;

    private record Release(String version, boolean newer) {}

    public UpdateChecker(Plugin plugin) {
        this(plugin, new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS).callTimeout(10, TimeUnit.SECONDS).build(), UPDATE_URL, System::nanoTime);
    }

    UpdateChecker(Plugin plugin, OkHttpClient client, String url, LongSupplier clock) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.client = client;
        this.request = new Request.Builder().url(url).build();
        this.clock = clock;
    }

    /** Nonblocking compatibility method. Returns false while the first lookup is pending. */
    public boolean checkForUpdate() {
        return latestRelease().getNow(Optional.empty()).map(Release::newer).orElse(false);
    }

    public CompletableFuture<Boolean> checkForUpdateAsync() {
        return latestRelease().thenApply(result -> result.map(Release::newer).orElse(false));
    }

    private synchronized CompletableFuture<Optional<Release>> latestRelease() {
        if (closed) return CompletableFuture.completedFuture(Optional.empty());
        if (pending != null) return pending;
        if (cacheDuration > 0 && clock.getAsLong() - checkedAt < cacheDuration)
            return CompletableFuture.completedFuture(Optional.ofNullable(cached));

        CompletableFuture<Optional<Release>> result = new CompletableFuture<>();
        pending = result;
        Call call = client.newCall(request);
        activeCall = call;
        call.enqueue(new Callback() {
            @Override public void onFailure(Call ignored, IOException error) { finish(call, null, error); }
            @Override public void onResponse(Call ignored, Response response) {
                Release release;
                try (response) {
                    if (!response.isSuccessful()) throw new IOException("HTTP " + response.code());
                    if (response.body() == null) throw new IOException("Empty update response");
                    String version = response.body().string().trim();
                    if (!RELEASE_VERSION.matcher(version).matches()) throw new IOException("Invalid version in update response");
                    release = new Release(version, isSemanticallyNewer(currentVersion, version));
                } catch (IOException | IllegalArgumentException error) {
                    finish(call, null, error);
                    return;
                }
                finish(call, release, null);
            }
        });
        return result;
    }

    private void finish(Call call, Release release, Exception error) {
        CompletableFuture<Optional<Release>> result;
        synchronized (this) {
            if (closed || call != activeCall) return;
            result = pending;
            pending = null;
            activeCall = null;
            cached = release;
            checkedAt = clock.getAsLong();
            cacheDuration = release == null ? FAILURE_CACHE_NANOS : SUCCESS_CACHE_NANOS;
        }
        if (error != null) plugin.getLogger().warning("Could not check for PerPlayerKit updates: " + error.getMessage()
                + ". A later check can retry in one minute.");
        result.complete(Optional.ofNullable(release));
    }

    public void printStartupStatus() {
        latestRelease().thenAccept(result -> {
            if (closed || result.isEmpty()) return;
            Release release = result.get();
            if (release.newer()) {
                plugin.getLogger().info("A new PerPlayerKit version is available: " + release.version() + " (installed: " + currentVersion + ")");
                plugin.getLogger().info("Spigot: https://www.spigotmc.org/resources/perplayerkit.121437/");
                plugin.getLogger().info("Modrinth: https://modrinth.com/plugin/perplayerkit");
                plugin.getLogger().info("Hangar: https://hangar.papermc.io/noah32/PerPlayerKit");
            } else plugin.getLogger().info("No newer PerPlayerKit release found (installed: " + currentVersion
                    + ", published: " + release.version() + ")");
        });
    }

    public void sendUpdateMessage(Player player) {
        UUID uuid = player.getUniqueId();
        latestRelease().thenAccept(result -> {
            if (closed || result.isEmpty() || !result.get().newer() || !plugin.isEnabled()) return;
            try {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (closed || !plugin.isEnabled() || !player.isOnline()
                            || plugin.getServer().getPlayer(uuid) != player
                            || !player.hasPermission("perplayerkit.admin")
                            || !plugin.getConfig().getBoolean("updates.notify-admins-on-join", true)) return;
                    Lang.get().send(player, "update.new-version-available", "current", currentVersion, "latest", result.get().version());
                });
            } catch (IllegalPluginAccessException ignored) {
                // The plugin can be disabled between the enabled check and task registration.
            }
        });
    }

    @Override public void close() {
        Call call;
        CompletableFuture<Optional<Release>> result;
        synchronized (this) {
            if (closed) return;
            closed = true;
            call = activeCall;
            result = pending;
            activeCall = null;
            pending = null;
            cached = null;
        }
        if (call != null) call.cancel();
        if (result != null) result.complete(Optional.empty());
        client.dispatcher().cancelAll();
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
    }

    /** Numeric release components, with missing components treated as zero. */
    static boolean isSemanticallyNewer(String current, String latest) {
        var installed = CURRENT_VERSION.matcher(current.trim());
        if (!installed.matches() || !RELEASE_VERSION.matcher(latest).matches())
            throw new IllegalArgumentException("Cannot compare the installed version with the published release");
        String[] left = installed.group(1).split("\\."), right = latest.split("\\.");
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            BigInteger a = i < left.length ? new BigInteger(left[i]) : BigInteger.ZERO;
            BigInteger b = i < right.length ? new BigInteger(right[i]) : BigInteger.ZERO;
            int comparison = b.compareTo(a);
            if (comparison != 0) return comparison > 0;
        }
        // A published stable release supersedes a prerelease of the same numeric version.
        return installed.group(2) != null && installed.group(2).startsWith("-");
    }
}
