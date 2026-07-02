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
package dev.noah.perplayerkit.commands.admin;

import dev.noah.perplayerkit.ItemPurgeService;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.commands.inspect.InspectCommandUtil;
import dev.noah.perplayerkit.util.Lang;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * /purgeitem &lt;item&gt; &lt;all confirm|player ...&gt;
 * <p>
 * Deletes a specific item type from stored kits and ender chests — for every
 * player in the database or for selected players — including items hidden
 * inside shulker boxes, other container items, and bundles. Works with all
 * storage backends because the purge rewrites the serialized kit blobs.
 */
public class PurgeItemCommand implements CommandExecutor, TabCompleter {

    private final Plugin plugin;
    private final AtomicBoolean purgeInProgress = new AtomicBoolean(false);

    public PurgeItemCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 2) {
            Lang.get().send(sender, "command.purgeitem-usage");
            return true;
        }

        Material item = Material.matchMaterial(args[0]);
        if (item == null || item.isAir()) {
            Lang.get().send(sender, "error.purge-invalid-item", "item", args[0]);
            return true;
        }

        if (args[1].equalsIgnoreCase("all")) {
            handleAll(sender, item, args);
            return true;
        }

        handlePlayers(sender, item, Arrays.stream(args, 1, args.length).distinct().toList());
        return true;
    }

    private void handleAll(CommandSender sender, Material item, String[] args) {
        if (args.length < 3 || !args[2].equalsIgnoreCase("confirm")) {
            Lang.get().send(sender, "info.purge-confirm-required", "item", item.name());
            return;
        }

        if (!purgeInProgress.compareAndSet(false, true)) {
            Lang.get().send(sender, "error.purge-in-progress");
            return;
        }

        Lang.get().send(sender, "info.purge-starting-all", "item", item.name());
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runPurge(sender, item, null));
    }

    private void handlePlayers(CommandSender sender, Material item, List<String> identifiers) {
        if (!purgeInProgress.compareAndSet(false, true)) {
            Lang.get().send(sender, "error.purge-in-progress");
            return;
        }

        resolveTargets(identifiers).whenComplete((resolution, throwable) -> {
            if (throwable != null) {
                purgeInProgress.set(false);
                plugin.getLogger().severe("Failed to resolve purge targets: " + throwable.getMessage());
                runOnMain(sender, current -> Lang.get().send(current, "error.unexpected"));
                return;
            }

            if (!resolution.missing().isEmpty()) {
                // Abort entirely so the admin never assumes a player was purged
                // when their name simply failed to resolve.
                purgeInProgress.set(false);
                runOnMain(sender, current -> {
                    for (String name : resolution.missing()) {
                        Lang.get().send(current, "error.purge-player-not-found", "player", name);
                    }
                });
                return;
            }

            runOnMain(sender, current -> Lang.get().send(current, "info.purge-starting-players",
                    "item", item.name(), "count", String.valueOf(resolution.uuids().size())));
            Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    () -> runPurge(sender, item, resolution.uuids()));
        });
    }

    private void runPurge(CommandSender sender, Material item, @Nullable List<UUID> targets) {
        try {
            ItemPurgeService service = new ItemPurgeService(PerPlayerKit.storageManager, KitManager.get());
            Consumer<String> progress = message -> plugin.getLogger().info("[ItemPurge] " + message);

            ItemPurgeService.PurgeResult result = targets == null
                    ? service.purgeAllPlayers(item, progress)
                    : service.purgePlayers(item, targets, progress);

            plugin.getLogger().info("[ItemPurge] Finished purging " + item.name()
                    + ": removed " + result.itemsRemoved() + " items from " + result.modified()
                    + " of " + result.scanned() + " entries (" + result.deleted() + " emptied entries deleted, "
                    + result.failed() + " failures)");
            runOnMain(sender, current -> sendResult(current, item, result));
        } catch (Exception e) {
            plugin.getLogger().severe("[ItemPurge] Purge of " + item.name() + " failed: " + e.getMessage());
            runOnMain(sender, current -> Lang.get().send(current, "error.unexpected"));
        } finally {
            purgeInProgress.set(false);
        }
    }

    private void sendResult(CommandSender sender, Material item, ItemPurgeService.PurgeResult result) {
        Lang.get().send(sender, "success.purge-completed",
                "items", String.valueOf(result.itemsRemoved()),
                "item", item.name(),
                "modified", String.valueOf(result.modified()));
        Lang.get().send(sender, "info.purge-summary",
                "scanned", String.valueOf(result.scanned()),
                "deleted", String.valueOf(result.deleted()));
        if (result.failed() > 0) {
            Lang.get().send(sender, "info.purge-failed-entries", "count", String.valueOf(result.failed()));
        }
    }

    private CompletableFuture<TargetResolution> resolveTargets(List<String> identifiers) {
        List<CompletableFuture<UUID>> futures = identifiers.stream()
                .map(InspectCommandUtil::resolvePlayerIdentifierAsync)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> {
                    List<UUID> uuids = new ArrayList<>();
                    List<String> missing = new ArrayList<>();
                    for (int i = 0; i < identifiers.size(); i++) {
                        UUID uuid = futures.get(i).join();
                        if (uuid == null) {
                            missing.add(identifiers.get(i));
                        } else if (!uuids.contains(uuid)) {
                            uuids.add(uuid);
                        }
                    }
                    return new TargetResolution(uuids, missing);
                });
    }

    /**
     * Runs the action on the main thread. For player senders the player is
     * re-fetched so nothing is sent to someone who logged off mid-purge.
     */
    private void runOnMain(CommandSender sender, Consumer<CommandSender> action) {
        if (sender instanceof Player player) {
            UUID uuid = player.getUniqueId();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Player current = Bukkit.getPlayer(uuid);
                if (current != null) {
                    action.accept(current);
                }
            });
        } else {
            Bukkit.getScheduler().runTask(plugin, () -> action.accept(sender));
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            String input = args[0].toUpperCase(Locale.ROOT);
            return Arrays.stream(Material.values())
                    .filter(Material::isItem)
                    .map(Enum::name)
                    .filter(name -> !name.startsWith("LEGACY_"))
                    .filter(name -> name.startsWith(input))
                    .sorted()
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String input = args[1].toLowerCase(Locale.ROOT);
            List<String> completions = new ArrayList<>();
            if ("all".startsWith(input)) {
                completions.add("all");
            }
            completions.addAll(onlinePlayerNames(input));
            return completions;
        }

        if (args[1].equalsIgnoreCase("all")) {
            if (args.length == 3 && "confirm".startsWith(args[2].toLowerCase(Locale.ROOT))) {
                return List.of("confirm");
            }
            return List.of();
        }

        return onlinePlayerNames(args[args.length - 1].toLowerCase(Locale.ROOT));
    }

    private List<String> onlinePlayerNames(String prefix) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .sorted()
                .collect(Collectors.toList());
    }

    private record TargetResolution(List<UUID> uuids, List<String> missing) {
    }
}
