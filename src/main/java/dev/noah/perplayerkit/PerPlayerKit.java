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
package dev.noah.perplayerkit;

import dev.noah.perplayerkit.commands.admin.AboutCommandListener;
import dev.noah.perplayerkit.commands.admin.KitRoomCommand;
import dev.noah.perplayerkit.commands.admin.PerPlayerKitCommand;
import dev.noah.perplayerkit.commands.admin.PurgeItemCommand;
import dev.noah.perplayerkit.commands.admin.SavePublicKitCommand;
import dev.noah.perplayerkit.commands.completion.ECSlotTabCompleter;
import dev.noah.perplayerkit.commands.completion.KitSlotTabCompleter;
import dev.noah.perplayerkit.commands.features.HealCommand;
import dev.noah.perplayerkit.commands.features.RegearCommand;
import dev.noah.perplayerkit.commands.features.RepairCommand;
import dev.noah.perplayerkit.commands.inspect.InspectEcCommand;
import dev.noah.perplayerkit.commands.inspect.InspectKitCommand;
import dev.noah.perplayerkit.commands.kits.DeleteKitCommand;
import dev.noah.perplayerkit.commands.kits.EnderchestCommand;
import dev.noah.perplayerkit.commands.kits.MainMenuCommand;
import dev.noah.perplayerkit.commands.kits.PublicKitCommand;
import dev.noah.perplayerkit.commands.kits.SwapKitCommand;
import dev.noah.perplayerkit.commands.share.CopyKitCommand;
import dev.noah.perplayerkit.commands.share.ShareAcceptCommand;
import dev.noah.perplayerkit.commands.share.ShareDeclineCommand;
import dev.noah.perplayerkit.commands.share.ShareECKitCommand;
import dev.noah.perplayerkit.commands.share.ShareKitCommand;
import dev.noah.perplayerkit.commands.share.TransferKitsCommand;
import dev.noah.perplayerkit.commands.shortcuts.ShortcutCommandRegistrar;
import dev.noah.perplayerkit.listeners.*;
import dev.noah.perplayerkit.listeners.antiexploit.CommandListener;
import dev.noah.perplayerkit.listeners.antiexploit.ShulkerDropItemsListener;
import dev.noah.perplayerkit.listeners.features.OldDeathDropListener;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.StorageSelector;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.util.BackupManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.StyleManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.util.List;
import java.util.UUID;

public final class PerPlayerKit extends JavaPlugin {

    public static Plugin plugin;
    public static StorageManager storageManager;
    private BackupManager backupManager;

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        notice();

        int bstatsId = 24380;
        Metrics metrics = new Metrics(this, bstatsId);

        plugin = this;
        new ConfigMigrator(this).migrate();
        ConfigManager configManager = new ConfigManager(this);
        configManager.loadConfig();
        reloadConfig();

        KitSlots.init(this);

        new Lang(this);
        new StyleManager(this);

        new ItemFilter(this);
        new BroadcastManager(this);

        new KitManager(this);
        new KitShareManager(this);
        new KitRoomDataManager(this);

        loadPublicKitsIdsFromConfig();
        getLogger().info("Public Kit Configuration Loaded");

        String dbType = this.getConfig().getString("storage.type");

        if (dbType == null) {
            this.getLogger().warning("Database type not found in config, fix your config to continue!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        storageManager = new StorageSelector(this, dbType).getDbManager();
        this.getLogger().info("Using storage type: " + storageManager.getClass().getName());

        if (storageManager == null) {
            this.getLogger().warning("Database error occurred, please check your config!");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        attemptDatabaseConnection(true);

        try {
            storageManager.init();
        } catch (StorageOperationException e) {
            this.getLogger().warning("Failed to initialize the database. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Initialize backup system for file-based storage methods
        if (isFileBasedStorage(dbType)) {
            backupManager = new BackupManager(this);
            if (backupManager.isEnabled()) {
                getLogger().info("Backup system initialized for file-based storage");
            } else {
                getLogger().info("Backup system disabled in configuration");
            }
        } else {
            getLogger().info("Backup system not needed for non-file-based storage: " + dbType);
        }

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {

            if (storageManager.isConnected()) {
                try {
                    storageManager.keepAlive();
                } catch (StorageConnectionException e) {
                    this.getLogger().warning("Database keep alive failed: " + e.getMessage());
                }
            } else {
                this.getLogger().warning("Database connection failed. Attempting to reconnect.");
                attemptDatabaseConnection(false);
            }

        }, 30 * 20, 30 * 20); // runs every 30 seconds

        loadDatabaseData();
        getLogger().info("Database data loaded");

        UpdateChecker updateChecker = new UpdateChecker(this);

        // REGISTER THINGS START
        KitSlotTabCompleter kitSlotTabCompleter = new KitSlotTabCompleter();

        this.getCommand("kit").setExecutor(new MainMenuCommand(plugin));

        this.getCommand("sharekit").setExecutor(new ShareKitCommand());
        this.getCommand("sharekit").setTabCompleter(new KitSlotTabCompleter(true));

        this.getCommand("shareec").setExecutor(new ShareECKitCommand());
        this.getCommand("shareec").setTabCompleter(new ECSlotTabCompleter(true));

        this.getCommand("copykit").setExecutor(new CopyKitCommand());

        TransferKitsCommand transferKitsCommand = new TransferKitsCommand();
        this.getCommand("transferkits").setExecutor(transferKitsCommand);
        this.getCommand("transferkits").setTabCompleter(transferKitsCommand);

        ShareAcceptCommand shareAcceptCommand = new ShareAcceptCommand();
        this.getCommand("shareaccept").setExecutor(shareAcceptCommand);
        this.getCommand("shareaccept").setTabCompleter(shareAcceptCommand);

        ShareDeclineCommand shareDeclineCommand = new ShareDeclineCommand();
        this.getCommand("sharedecline").setExecutor(shareDeclineCommand);
        this.getCommand("sharedecline").setTabCompleter(shareDeclineCommand);

        KitRoomCommand kitRoomCommand = new KitRoomCommand();
        this.getCommand("kitroom").setExecutor(kitRoomCommand);
        this.getCommand("kitroom").setTabCompleter(kitRoomCommand);

        this.getCommand("swapkit").setExecutor(new SwapKitCommand());
        this.getCommand("swapkit").setTabCompleter(kitSlotTabCompleter);

        this.getCommand("deletekit").setExecutor(new DeleteKitCommand());
        this.getCommand("deletekit").setTabCompleter(kitSlotTabCompleter);

        this.getCommand("inspectkit").setExecutor(new InspectKitCommand(plugin));
        this.getCommand("inspectkit").setTabCompleter(new InspectKitCommand(plugin));

        this.getCommand("inspectec").setExecutor(new InspectEcCommand(plugin));
        this.getCommand("inspectec").setTabCompleter(new InspectEcCommand(plugin));

        this.getCommand("enderchest").setExecutor(new EnderchestCommand());

        SavePublicKitCommand savePublicKitCommand = new SavePublicKitCommand();
        this.getCommand("savepublickit").setExecutor(savePublicKitCommand);
        this.getCommand("savepublickit").setTabCompleter(savePublicKitCommand);

        PublicKitCommand publicKitCommand = new PublicKitCommand(plugin);
        this.getCommand("publickit").setExecutor(publicKitCommand);
        this.getCommand("publickit").setTabCompleter(publicKitCommand);

        ShortcutCommandRegistrar.registerAll(this);

        RegearCommand regearCommand = new RegearCommand(this);
        this.getCommand("regear").setExecutor(regearCommand);

        this.getCommand("heal").setExecutor(new HealCommand());
        this.getCommand("repair").setExecutor(new RepairCommand());
        this.getCommand("perplayerkit").setExecutor(new PerPlayerKitCommand(this));

        PurgeItemCommand purgeItemCommand = new PurgeItemCommand(this);
        this.getCommand("purgeitem").setExecutor(purgeItemCommand);
        this.getCommand("purgeitem").setTabCompleter(purgeItemCommand);

        Bukkit.getPluginManager().registerEvents(regearCommand, this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, updateChecker), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitMenuCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitRoomSaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new AutoRekitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AboutCommandListener(), this);

        // features
        if (getConfig().getBoolean("feature.old-death-drops", false)) {
            Bukkit.getPluginManager().registerEvents(new OldDeathDropListener(), this);
        }

        if (getConfig().getBoolean("anti-exploit.block-spaces-in-commands", false)) {
            Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        }

        if (getConfig().getBoolean("anti-exploit.prevent-shulkers-dropping-items", false)) {
            Bukkit.getPluginManager().registerEvents(new ShulkerDropItemsListener(), this);
        }

        // REGISTER THINGS END

        BroadcastManager.get().startScheduledBroadcast();
        updateChecker.printStartupStatus();

    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();

        // Shutdown backup manager if it exists
        if (backupManager != null) {
            backupManager.shutdown();
        }
    }

    /**
     * Check if the storage type is file-based (requires backups)
     * 
     * @param storageType The storage type from configuration
     * @return true if file-based storage, false otherwise
     */
    private boolean isFileBasedStorage(String storageType) {
        return storageType.equalsIgnoreCase("sqlite") ||
                storageType.equalsIgnoreCase("yml") ||
                storageType.equalsIgnoreCase("yaml");
    }

    private void loadPublicKitsIdsFromConfig() {
        // generate list of public kits from the config
        ConfigurationSection publicKitsSection = getConfig().getConfigurationSection("publickits");

        if (publicKitsSection == null) {
            this.getLogger().warning("No public kits found in config!");
        } else {

            publicKitsSection.getKeys(false).forEach(key -> {
                String name = getConfig().getString("publickits." + key + ".name");
                Material icon = Material.valueOf(getConfig().getString("publickits." + key + ".icon"));
                PublicKit kit = new PublicKit(key, name, icon);
                KitManager.get().getPublicKitList().add(kit);
            });
        }
    }

    private void loadDatabaseData() {
        KitRoomDataManager.get().loadFromDB();
        KitManager.get().getPublicKitList().forEach(kit -> KitManager.get().loadPublicKitFromDB(kit.id));
        // Only relevant when the plugin is (re-)enabled with players online.
        // Load off the main thread like JoinListener does — with a large
        // max-kits this is many storage queries per player.
        List<UUID> onlineUuids = Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList();
        if (!onlineUuids.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(this,
                    () -> onlineUuids.forEach(uuid -> KitManager.get().loadPlayerDataFromDB(uuid)));
        }
    }

    private void attemptDatabaseConnection(boolean disableOnFail) {
        try {
            storageManager.connect();
            if (!storageManager.isConnected()) {
                throw new StorageConnectionException("Expected to be connected to the database, but failed.");
            }
        } catch (StorageConnectionException e) {
            if (disableOnFail) {
                this.getLogger().warning("Database connection failed: " + e.getMessage());
                this.getLogger().warning("Disabling plugin.");
                Bukkit.getPluginManager().disablePlugin(this);
            } else {
                this.getLogger().warning("Database connection failed: " + e.getMessage());
            }
        }
    }

    private void closeDatabaseConnection() {
        try {
            storageManager.close();
        } catch (StorageConnectionException e) {
            // retry once
            try {
                storageManager.close();
            } catch (StorageConnectionException ex) {
                this.getLogger().warning("Failed to close the database connection: " + e.getMessage());
            }
        }
    }

    private void notice() {
        String notice = """
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
                * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.""";

        String otherInfo = """
                * All users must be provided with the source code of the software, as per the AGPL-3.0 license.
                * If you are using a modified version of PerPlayerKit, you must make the source code of your
                * modified version available to all users, as per the AGPL-3.0 license.
                * Consider modifying the /aboutperplayerkit command to include a link to your modified source code.
                """;

        getLogger().info(notice);
        getLogger().info(otherInfo);
    }
}
