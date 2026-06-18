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

import dev.noah.perplayerkit.storage.StorageMigrator;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
            Lang.get().send(sender, "error.missing-arguments");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "about":
                Lang.get().send(sender, "command.perplayerkit-about");
                return true;
            case "import":
                return handleImport(sender, args);
            case "migrate":
                return handleMigrate(sender, args);
            default:
                Lang.get().send(sender, "error.invalid-subcommand");
                return true;

        }
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
            return List.of("about", "import", "migrate");
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return List.of("kitsx");
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
