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

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.ERROR_PREFIX;
import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.MAX_SLOT;
import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.MIN_SLOT;
import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.mm;
import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.resolvePlayerIdentifierAsync;
import static dev.noah.perplayerkit.commands.inspect.InspectCommandUtil.showUsage;
import static dev.noah.perplayerkit.util.PlayerUtil.getPlayerName;

public abstract class AbstractInspectCommand implements CommandExecutor, TabCompleter {
    protected final Plugin plugin;

    protected AbstractInspectCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    protected abstract String usageCommand();

    protected abstract boolean hasData(UUID targetUuid, int slot);

    protected abstract void openInspectGui(Player inspector, UUID targetUuid, int slot);

    protected abstract String missingDataMessage(String targetName, int slot);

    protected abstract String loadErrorLogMessage();

    protected abstract String loadErrorUserMessage();

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
            showUsage(player, usageCommand());
            return true;
        }

        int slot = parseSlot(args[1], player);
        if (slot == -1) {
            return true;
        }

        CompletableFuture<Void> future = resolvePlayerIdentifierAsync(args[0])
                .thenCompose(targetUuid -> {
                    if (targetUuid == null) {
                        Bukkit.getScheduler().runTask(plugin, () -> showPlayerNotFound(player));
                        return CompletableFuture.completedFuture(null);
                    }

                    Player targetPlayer = Bukkit.getPlayer(targetUuid);
                    return CompletableFuture.runAsync(() -> {
                        if (targetPlayer == null) {
                            KitManager.get().loadPlayerDataFromDB(targetUuid);
                        }
                    }).thenRun(() -> Bukkit.getScheduler().runTask(plugin, () -> showInspectResult(player, targetUuid, slot)));
                });

        future.exceptionally(ex -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                plugin.getLogger().severe(loadErrorLogMessage() + ": " + ex.getMessage());
                BroadcastManager.get().sendComponentMessage(player,
                        ERROR_PREFIX.append(mm.deserialize(loadErrorUserMessage())));
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
            List<String> completions = new ArrayList<>(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .toList());
            if (input.length() >= 4 && input.contains("-")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream()
                        .map(Player::getUniqueId)
                        .map(UUID::toString)
                        .filter(uuid -> uuid.startsWith(input))
                        .toList());
            }
            return completions;
        }

        if (args.length == 2) {
            return IntStream.rangeClosed(MIN_SLOT, MAX_SLOT)
                    .mapToObj(String::valueOf)
                    .filter(slot -> slot.startsWith(args[1]))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private int parseSlot(String slotArg, Player player) {
        try {
            int slot = Integer.parseInt(slotArg);
            if (slot < MIN_SLOT || slot > MAX_SLOT) {
                throw new NumberFormatException();
            }
            return slot;
        } catch (NumberFormatException e) {
            BroadcastManager.get().sendComponentMessage(player,
                    ERROR_PREFIX.append(
                            mm.deserialize("<red>Slot must be a number between " +
                                    MIN_SLOT + " and " + MAX_SLOT + ".</red>")));
            SoundManager.playFailure(player);
            return -1;
        }
    }

    private void showInspectResult(Player inspector, UUID targetUuid, int slot) {
        if (hasData(targetUuid, slot)) {
            openInspectGui(inspector, targetUuid, slot);
            return;
        }

        String targetName = getPlayerName(targetUuid);
        BroadcastManager.get().sendComponentMessage(inspector,
                ERROR_PREFIX.append(mm.deserialize(missingDataMessage(targetName, slot))));
        SoundManager.playFailure(inspector);
    }

    private void showPlayerNotFound(Player player) {
        BroadcastManager.get().sendComponentMessage(player,
                ERROR_PREFIX.append(
                        mm.deserialize("<red>Could not find a player with that name or UUID.</red>")));
        SoundManager.playFailure(player);
    }
}
