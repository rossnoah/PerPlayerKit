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
package dev.noah.perplayerkit.commands.admin;

import dev.noah.perplayerkit.starter.StarterExporter;
import dev.noah.perplayerkit.starter.StarterSetup;
import dev.noah.perplayerkit.storage.StorageMigrator;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.LocationAccess;
import dev.noah.perplayerkit.util.LocationFeature;
import dev.noah.perplayerkit.util.RekitKitResolver;
import dev.noah.perplayerkit.commands.core.CommandGuards;
import org.bukkit.entity.Player;
import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PerPlayerKitCommand implements CommandExecutor, TabCompleter {

    private static final List<String> STORAGE_TYPES = Arrays.asList("sqlite", "mysql", "postgresql", "redis", "yml");

    private final Plugin plugin;

    public PerPlayerKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "location":
                return handleLocation(sender, args);
            case "about":
                Lang.get().send(sender, "command.perplayerkit-about");
                return true;
            case "import":
                return handleImport(sender, args);
            case "migrate":
                return handleMigrate(sender, args);
            case "autosetup":
                return handleAutoSetup(sender, args);
            case "export":
                return handleExport(sender, args);
            default:
                Lang.get().send(sender, "error.invalid-subcommand");
                sendUsage(sender);
                return true;

        }
    }

    /** A bare /perplayerkit used to say "Missing arguments!" and leave it there. */
    private void sendUsage(CommandSender sender) {
        Lang.get().send(sender, "command.perplayerkit-usage-header");
        Lang.get().sendNoPrefix(sender, "command.perplayerkit-usage-autosetup");
        Lang.get().sendNoPrefix(sender, "command.perplayerkit-usage-about");
        Lang.get().sendNoPrefix(sender, "command.perplayerkit-usage-location");
        Lang.get().sendNoPrefix(sender, "command.perplayerkit-usage-import");
        Lang.get().sendNoPrefix(sender, "command.perplayerkit-usage-migrate");
    }

    private boolean handleLocation(CommandSender sender, String[] args) {
        Player player = CommandGuards.requirePlayer(sender);
        if (player == null) return true;
        if (args.length > 2) {
            Lang.get().send(sender, "command.perplayerkit-location-usage");
            return true;
        }
        LocationFeature feature;
        try { feature = args.length == 2 ? LocationFeature.fromKey(args[1].toLowerCase(java.util.Locale.ROOT)) : LocationFeature.GLOBAL; }
        catch (IllegalArgumentException e) {
            Lang.get().send(sender, "command.perplayerkit-location-usage");
            return true;
        }
        LocationAccess access = LocationAccess.get();
        LocationAccess.Decision decision = access.check(player, feature);
        Lang.get().send(sender, "info.location-result", "world", player.getWorld().getName(),
                "feature", feature.key(), "result", decision.allowed() ? "ALLOW" : "DENY",
                "rule", decision.rule(), "reason", decision.reason());
        List<String> regions = List.of();
        boolean regionsAvailable = true;
        try {
            regions = access.regions(player);
            Lang.get().send(sender, "info.location-regions", "regions", regions.isEmpty() ? "None" : String.join(", ", regions));
        } catch (LocationAccess.RegionUnavailableException e) {
            Lang.get().send(sender, "info.location-regions", "regions", e.getMessage());
            regionsAvailable = false;
        }
        String path = switch (feature) {
            case REKIT_RESPAWN -> "rekit.respawn.kits";
            case REKIT_KILL -> "rekit.kill.kits";
            default -> null;
        };
        if (path != null) {
            var mappings = plugin.getConfig().getConfigurationSection(path);
            String kit = !regionsAvailable && RekitKitResolver.hasRegionEntries(mappings, player.getWorld().getName())
                    ? "Unavailable until WorldGuard regions can be checked"
                    : RekitKitResolver.resolveKit(mappings, player.getWorld().getName(), regions);
            Lang.get().send(sender, "info.location-kit", "kit", kit == null ? "Last loaded kit" : kit);
        }
        return true;
    }

    private boolean handleImport(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Lang.get().send(sender, "error.missing-import-type");
            return true;
        }

        if (!args[1].equalsIgnoreCase("kitsx")) {
            Lang.get().send(sender, "error.invalid-import-type");
            return true;
        }

        Lang.get().send(sender, "success.import-starting");
        KitsXImporter importer = new KitsXImporter(plugin, sender);
        if (!importer.checkForFiles()) {
            Lang.get().send(sender, "error.import-files-missing");
            Lang.get().send(sender, "info.import-instructions");
            return true;
        }

        importer.importFiles();
        Lang.get().send(sender, "success.import-attempted");
        return true;
    }

    /**
     * {@code /perplayerkit autosetup} fills any kit room page or public kit that
     * has never been configured. {@code autosetup reset confirm} replaces what is
     * already there, so it asks first.
     */
    private boolean handleAutoSetup(CommandSender sender, String[] args) {
        boolean reset = args.length >= 2 && args[1].equalsIgnoreCase("reset");

        boolean addKits = args.length == 2 && args[1].equalsIgnoreCase("add-kits");
        if (addKits) {
            try {
                int added = StarterSetup.get().addMissingPublicKits();
                Lang.get().send(sender, "info.autosetup-definitions-added", "count", String.valueOf(added));
            } catch (java.io.IOException e) {
                Lang.get().send(sender, "error.autosetup-config-save", "error", e.getMessage());
                return true;
            }
        }
        if (!reset && args.length >= 2 && !addKits) {
            Lang.get().send(sender, "command.perplayerkit-autosetup-usage");
            return true;
        }

        // Reset is the one destructive path here: it replaces every kit room
        // page and public kit. Console only, so it cannot happen by accident in
        // game and an admin account alone is not enough to trigger it.
        if (reset && !(sender instanceof ConsoleCommandSender)) {
            Lang.get().send(sender, "error.autosetup-reset-console-only");
            return true;
        }

        if (reset && (args.length < 3 || !args[2].equalsIgnoreCase("confirm"))) {
            Lang.get().send(sender, "info.autosetup-reset-confirm");
            return true;
        }

        StarterSetup.Result result = StarterSetup.get().apply(reset);

        if (result.isEmpty()) {
            Lang.get().send(sender, "info.autosetup-nothing-to-do");
            return true;
        }

        Lang.get().send(sender, "success.autosetup-applied",
                "pages", String.valueOf(result.kitRoomPages().size()),
                "kits", String.valueOf(result.publicKits().size()));

        if (!result.publicKits().isEmpty()) {
            Lang.get().send(sender, "info.autosetup-public-kits", "kits", String.join(", ", result.publicKits()));
        }
        Lang.get().send(sender, "info.autosetup-next-steps");
        return true;
    }

    /**
     * Writes the live kit room out in the notation the bundled defaults use.
     * A maintainer tool for updating those defaults from a room arranged in
     * game, so it is console only and stays silent unless the server was
     * started with -Dperplayerkit.debug=true.
     */
    private boolean handleExport(CommandSender sender, String[] args) {
        if (!StarterExporter.isEnabled() || !(sender instanceof ConsoleCommandSender)) {
            Lang.get().send(sender, "error.invalid-subcommand");
            return true;
        }

        try {
            // export <player> <slot> pulls one player's own kit out instead, for
            // promoting a kit somebody built into the bundled public kits.
            if (args.length >= 3) {
                java.util.UUID uuid = Bukkit.getOfflinePlayer(args[1]).getUniqueId();
                int slot = Integer.parseInt(args[2]);
                sender.sendMessage("Kit " + slot + " written to "
                        + StarterExporter.exportPlayerKit(plugin, uuid, slot));
                return true;
            }

            sender.sendMessage("Kit room written to " + StarterExporter.export(plugin));
            sender.sendMessage("Public kits written to " + StarterExporter.exportPublicKits(plugin));
        } catch (java.io.IOException | NumberFormatException e) {
            sender.sendMessage("Could not write the export: " + e.getMessage());
        }
        return true;
    }

    private boolean handleMigrate(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sendMigrateUsage(sender);
            return true;
        }

        String sourceType = args[1].toLowerCase();
        String destinationType = args[2].toLowerCase();

        if (!validateStorageType(sender, sourceType, "source")) {
            return true;
        }
        if (!validateStorageType(sender, destinationType, "destination")) {
            return true;
        }
        if (sourceType.equals(destinationType)) {
            Lang.get().send(sender, "error.storage-same");
            return true;
        }

        Lang.get().send(sender, "info.migration-starting", "source", sourceType, "destination", destinationType);
        Lang.get().send(sender, "info.migration-large-dataset");

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> runMigration(sender, sourceType, destinationType));
        return true;
    }

    private void sendMigrateUsage(CommandSender sender) {
        Lang.get().send(sender, "command.perplayerkit-migrate-usage");
        Lang.get().send(sender, "info.available-storage-types");
    }

    private boolean validateStorageType(CommandSender sender, String storageType, String role) {
        if (STORAGE_TYPES.contains(storageType)) {
            return true;
        }

        Lang.get().send(sender, "error.invalid-storage-type", "role", role, "type", storageType);
        Lang.get().send(sender, "info.available-storage-types");
        return false;
    }

    private void runMigration(CommandSender sender, String sourceType, String destinationType) {
        StorageMigrator migrator = new StorageMigrator(plugin);
        StorageMigrator.MigrationResult result = migrator.migrate(
                sourceType,
                destinationType,
                message -> Bukkit.getScheduler().runTask(plugin,
                        () -> Lang.get().send(sender, "info.migration-progress", "message", message))
        );

        Bukkit.getScheduler().runTask(plugin, () -> sendMigrationResult(sender, destinationType, result));
    }

    private void sendMigrationResult(CommandSender sender, String destinationType, StorageMigrator.MigrationResult result) {
        if (result.isSuccess()) {
            Lang.get().send(sender, "success.migration-completed");
            Lang.get().send(sender, "success.migration-count", "count", String.valueOf(result.getMigratedCount()));
            if (result.getFailedCount() > 0) {
                Lang.get().send(sender, "info.migration-failed-count", "count", String.valueOf(result.getFailedCount()));
            }
            Lang.get().send(sender, "info.update-config-storage", "type", destinationType);
            return;
        }

        Lang.get().send(sender, "error.migration-failed", "error", result.getErrorMessage());
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            return List.of("about", "autosetup", "import", "migrate", "location");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("location")) {
            return Arrays.stream(LocationFeature.values()).map(LocationFeature::key).toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return List.of("kitsx");
        }

        // Only the console can reset, so only the console is offered it.
        if (args.length == 2 && args[0].equalsIgnoreCase("autosetup")) {
            return (sender instanceof ConsoleCommandSender) ? List.of("add-kits", "reset") : List.of("add-kits");
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("autosetup") && args[1].equalsIgnoreCase("reset")) {
            return List.of("confirm");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("migrate")) {
            return STORAGE_TYPES.stream()
                    .filter(type -> type.startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("migrate")) {
            String sourceType = args[1].toLowerCase();
            return STORAGE_TYPES.stream()
                    .filter(type -> !type.equals(sourceType))
                    .filter(type -> type.startsWith(args[2].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return null;
    }
}
