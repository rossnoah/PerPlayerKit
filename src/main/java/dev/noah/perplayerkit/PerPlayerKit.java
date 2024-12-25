package dev.noah.perplayerkit;

import dev.noah.perplayerkit.commands.*;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.storage.StorageSelector;
import dev.noah.perplayerkit.storage.exceptions.StorageConnectionException;
import dev.noah.perplayerkit.storage.exceptions.StorageOperationException;
import dev.noah.perplayerkit.listeners.*;
import dev.noah.perplayerkit.tabcompleter.KitRoomTab;
import dev.noah.perplayerkit.util.BroadcastManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;


public final class PerPlayerKit extends JavaPlugin {

    public static Plugin plugin;

    public static StorageManager storageManager;
    public static String prefix = ChatColor.translateAlternateColorCodes('&', "&7[&bKits&7] ");

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();

        new ItemFilter();
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

        if(!attemptDatabaseConnection(true)){
            return;
        }

        try {
            storageManager.init();
        } catch (StorageOperationException e) {
            this.getLogger().warning("Failed to initialize the database. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
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

        }, 30 * 20, 30 * 20); //runs every 30 seconds

        loadDatabaseData();
        getLogger().info("Database data loaded");

        registerCommands();
        getLogger().info("Commands registered");
        registerListeners();
        getLogger().info("Listeners registered");

        BroadcastManager.get().startScheduledBroadcast();

    }

    @Override
    public void onDisable() {
        closeDatabaseConnection();
    }


    private void loadPublicKitsIdsFromConfig() {
        // generate list of public kits from the config
        ConfigurationSection publicKitsSection = this.getConfig().getConfigurationSection("publickits");

        if (publicKitsSection == null) {
            this.getLogger().warning("No public kits found in config!");
        } else {

            publicKitsSection.getKeys(false).forEach(key -> {
                String name = PerPlayerKit.getPlugin().getConfig().getString("publickits." + key + ".name");
                Material icon = Material.valueOf(PerPlayerKit.getPlugin().getConfig().getString("publickits." + key + ".icon"));
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


    private void registerCommands() {
        this.getCommand("kit").setExecutor(new MainMenuCommand());
        this.getCommand("sharekit").setExecutor(new ShareKitCommand());
        this.getCommand("copykit").setExecutor(new CopyKitCommand());
        this.getCommand("kitroom").setExecutor(new KitRoomCommands());
        this.getCommand("kitroom").setTabCompleter(new KitRoomTab());
        this.getCommand("swapkit").setExecutor(new SwapKitCommand());
        this.getCommand("deletekit").setExecutor(new DeleteKitCommand());
        this.getCommand("inspectkit").setExecutor(new InspectKitCommand());
        this.getCommand("enderchest").setExecutor(new EnderchestCommand());
        this.getCommand("savepublickit").setExecutor(new SavePublicKitCommand());
        this.getCommand("savepublickit").setTabCompleter(new SavePublicKitCommand());
        this.getCommand("publickit").setExecutor(new PublicKitCommand());

        for (int i = 1; i <= 9; i++) {
            this.getCommand("k" + i).setExecutor(new ShortKitCommand());
        }

        for (int i = 1; i <= 9; i++) {
            this.getCommand("ec" + i).setExecutor(new ShortECCommand());
        }

    }


    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new JoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitMenuCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitRoomSaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new RespawnListener(), this);
    }

    private boolean attemptDatabaseConnection(boolean disableOnFail) {
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
        }return true;
    }

    private void closeDatabaseConnection() {
        try {
            storageManager.close();
        } catch (StorageConnectionException e) {
//            retry once
            try {
                storageManager.close();
            } catch (StorageConnectionException ex) {
                this.getLogger().warning("Failed to close the database connection: " + e.getMessage());
            }
        }
    }

}
