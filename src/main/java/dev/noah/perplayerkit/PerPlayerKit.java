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

import dev.noah.perplayerkit.commands.*;
import dev.noah.perplayerkit.commands.extracommands.HealCommand;
import dev.noah.perplayerkit.commands.extracommands.RepairCommand;
import dev.noah.perplayerkit.commands.tabcompleters.ECSlotTabCompleter;
import dev.noah.perplayerkit.commands.tabcompleters.KitSlotTabCompleter;
import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.listeners.*;
import dev.noah.perplayerkit.listeners.antiexploit.CommandListener;
import dev.noah.perplayerkit.listeners.antiexploit.ShulkerDropItemsListener;
import dev.noah.perplayerkit.listeners.features.OldDeathDropListener;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.StorageSelector;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.util.BroadcastManager;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;

public final class PerPlayerKit extends JavaPlugin {

    public static Plugin plugin;
    public static StorageManager storageManager;

    // Task tracking for reload functionality
    private BukkitTask databaseKeepAliveTask;
    private ConfigManager configManager;

    // Track conditional listeners for proper reload support
    private final List<Listener> conditionalListeners = new ArrayList<>();

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        notice();

        int bstatsId = 24380;
        Metrics metrics = new Metrics(this, bstatsId);

        plugin = this;
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        new ItemFilter(this);
        new BroadcastManager(this);

        new KitManager(this);
        new KitShareManager(this);
        new KitRoomDataManager(this);
        new GUI(this);

        loadPublicKitsIdsFromConfig();
        getLogger().info("Public Kit Configuration Loaded");

        String dbType = ConfigManager.get().getStorageType();

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

        startDatabaseKeepAliveTask();

        loadDatabaseData();
        getLogger().info("Database data loaded");

        UpdateChecker updateChecker = new UpdateChecker(this);

        registerCommands();
        registerListeners(updateChecker);

        BroadcastManager.get().startScheduledBroadcast();
        updateChecker.printStartupStatus();
    }

    @Override
    public void onDisable() {
        // Unregister conditional listeners
        unregisterConditionalListeners();

        // Close database connection
        closeDatabaseConnection();
    }

    private void loadPublicKitsIdsFromConfig() {
        // generate list of public kits from the config
        ConfigurationSection publicKitsSection = ConfigManager.get().getPublicKitsSection();

        if (publicKitsSection == null) {
            this.getLogger().warning("No public kits found in config!");
        } else {

            publicKitsSection.getKeys(false).forEach(key -> {
                String name = ConfigManager.get().getPublicKitName(key);
                Material icon = Material.valueOf(ConfigManager.get().getPublicKitIcon(key));
                PublicKit kit = new PublicKit(key, name, icon);
                KitManager.get().getPublicKitList().add(kit);
            });
        }
    }

    private void loadDatabaseData() {
        KitRoomDataManager.get().loadFromDB();
        KitManager.get().getPublicKitList().forEach(kit -> KitManager.get().loadPublicKitFromDB(kit.id));
        Bukkit.getOnlinePlayers().forEach(player -> KitManager.get().loadPlayerDataFromDB(player.getUniqueId()));

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

    private void startDatabaseKeepAliveTask() {
        databaseKeepAliveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
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
    }

    private void registerCommands() {
        // REGISTER THINGS START
        KitSlotTabCompleter kitSlotTabCompleter = new KitSlotTabCompleter();
        ECSlotTabCompleter ecSlotTabCompleter = new ECSlotTabCompleter();

        this.getCommand("kit").setExecutor(new MainMenuCommand(plugin));

        this.getCommand("sharekit").setExecutor(new ShareKitCommand());
        this.getCommand("sharekit").setTabCompleter(kitSlotTabCompleter);

        this.getCommand("shareec").setExecutor(new ShareECKitCommand());
        this.getCommand("shareec").setTabCompleter(ecSlotTabCompleter);

        this.getCommand("copykit").setExecutor(new CopyKitCommand());

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

        for (int i = 1; i <= 9; i++) {
            this.getCommand("k" + i).setExecutor(new ShortKitCommand());
        }

        for (int i = 1; i <= 9; i++) {
            this.getCommand("ec" + i).setExecutor(new ShortECCommand());
        }

        RegearCommand regearCommand = new RegearCommand(this);
        this.getCommand("regear").setExecutor(regearCommand);

        this.getCommand("heal").setExecutor(new HealCommand());
        this.getCommand("repair").setExecutor(new RepairCommand());

        PerPlayerKitCommand perPlayerKitCommand = new PerPlayerKitCommand(this);
        this.getCommand("perplayerkit").setExecutor(perPlayerKitCommand);
        this.getCommand("perplayerkit").setTabCompleter(perPlayerKitCommand);
    }

    private void registerListeners(UpdateChecker updateChecker) {
        Bukkit.getPluginManager().registerEvents(new RegearCommand(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinListener(this, updateChecker), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitMenuCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitRoomSaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new AutoRekitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AboutCommandListener(), this);

        // Register conditional listeners and track them for reload support
        registerConditionalListeners();

        // REGISTER THINGS END
    }

    /**
     * Registers conditional listeners based on configuration and tracks them for
     * reload support
     */
    private void registerConditionalListeners() {
        // Clear any existing conditional listeners first
        unregisterConditionalListeners();

        getLogger().info("Registering conditional listeners based on current configuration...");

        // Register conditional listeners based on current config
        if (ConfigManager.get().isOldDeathDropsEnabled()) {
            Listener listener = new OldDeathDropListener();
            Bukkit.getPluginManager().registerEvents(listener, this);
            conditionalListeners.add(listener);
            getLogger().info("✓ Registered OldDeathDropListener");
        } else {
            getLogger().info("✗ OldDeathDropListener disabled in config");
        }

        if (ConfigManager.get().isBlockSpacesInCommandsEnabled()) {
            Listener listener = new CommandListener();
            Bukkit.getPluginManager().registerEvents(listener, this);
            conditionalListeners.add(listener);
            getLogger().info("✓ Registered CommandListener (anti-exploit)");
        } else {
            getLogger().info("✗ CommandListener disabled in config");
        }

        if (ConfigManager.get().isPreventShulkersDroppingItemsEnabled()) {
            Listener listener = new ShulkerDropItemsListener();
            Bukkit.getPluginManager().registerEvents(listener, this);
            conditionalListeners.add(listener);
            getLogger().info("✓ Registered ShulkerDropItemsListener (anti-exploit)");
        } else {
            getLogger().info("✗ ShulkerDropItemsListener disabled in config");
        }

        getLogger()
                .info("Conditional listener registration complete. Active listeners: " + conditionalListeners.size());
    }

    /**
     * Unregisters all tracked conditional listeners
     */
    private void unregisterConditionalListeners() {
        if (!conditionalListeners.isEmpty()) {
            getLogger().info("Unregistering " + conditionalListeners.size() + " conditional listeners");
        }

        for (Listener listener : conditionalListeners) {
            HandlerList.unregisterAll(listener);
        }
        conditionalListeners.clear();
    }

    /**
     * Reloads the plugin configuration and reinitializes components that depend on
     * it
     */
    public void reloadPlugin() {
        getLogger().info("Starting plugin reload...");

        // Cancel existing tasks
        if (databaseKeepAliveTask != null && !databaseKeepAliveTask.isCancelled()) {
            databaseKeepAliveTask.cancel();
        }
        Bukkit.getScheduler().cancelTasks(this);

        // Reload configuration
        reloadConfig();
        configManager.loadConfig();

        // Reinitialize components that depend on config
        new ItemFilter(this);
        new BroadcastManager(this);
        new GUI(this);

        // Reload configuration for existing components
        ItemFilter.get().reloadConfig();
        GUI.get().reloadConfig();

        // Reload RegearCommand configuration
        if (RegearCommand.getInstance() != null) {
            RegearCommand.getInstance().reloadConfig();
        }

        // Reload public kits configuration
        KitManager.get().getPublicKitList().clear();
        loadPublicKitsIdsFromConfig();

        // Re-register conditional listeners based on updated configuration
        registerConditionalListeners();

        // Restart database keep alive task
        startDatabaseKeepAliveTask();

        // Restart broadcast manager
        BroadcastManager.get().startScheduledBroadcast();

        getLogger().info("Plugin reload completed successfully!");
        getLogger().info("All features including conditional listeners have been reloaded.");
    }

}
