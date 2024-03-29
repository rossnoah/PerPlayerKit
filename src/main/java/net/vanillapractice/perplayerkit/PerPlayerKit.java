package net.vanillapractice.perplayerkit;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.vanillapractice.perplayerkit.commands.*;
import net.vanillapractice.perplayerkit.gui.ItemUtil;
import net.vanillapractice.perplayerkit.listeners.*;
import net.vanillapractice.perplayerkit.sql.MySQL;
import net.vanillapractice.perplayerkit.sql.PerPlayerKitDatabase;
import net.vanillapractice.perplayerkit.sql.SQLGetter;
import net.vanillapractice.perplayerkit.sql.SQLite;
import net.vanillapractice.perplayerkit.tabcompleter.KitRoomTab;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.ipvp.canvas.MenuFunctionListener;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


public final class PerPlayerKit extends JavaPlugin {

    public static SQLGetter sqldata;
    public static HashMap<String, ItemStack[]> data = new HashMap<>();
    public static HashMap<String, ItemStack[]> kitShareData = new HashMap<>();
    public static int bcDistance = 500;
    public static String prefix = ChatColor.translateAlternateColorCodes('&', "&7[&bKits&7] ");
    public static HashMap<UUID, Timestamp> repairBroadcastCooldown = new HashMap<>();
    public static int repairDelay = 5;
    public static HashMap<UUID, Timestamp> kitRoomBroadcastCooldown = new HashMap<>();
    public static int kitRoomDelay = 15;
    public static ArrayList<ItemStack[]> kitroomData = new ArrayList<>();
    public static int shareDelay = 30;
    public static HashMap<UUID, Timestamp> shareCooldown = new HashMap<>();
    public static ArrayList<String> whitelist = new ArrayList<>();
    public static Plugin plugin;
    public static HashMap<UUID, Integer> lastKit = new HashMap<>();
    public static List<PublicKit> publicKitList = new ArrayList<>();
    public PerPlayerKitDatabase database;

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;

        this.saveDefaultConfig();


        // Plugin startup logic
        ItemStack[] defaultPage = new ItemStack[45];
        defaultPage[0] = ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, "&bDefault Kit Room Item");
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);


        //generate list of public kits from the config
        PerPlayerKit.getPlugin().getConfig().getConfigurationSection("publickits").getKeys(false).forEach(key -> {
            String name = PerPlayerKit.getPlugin().getConfig().getString("publickits." + key + ".name");
            Material icon = Material.valueOf(PerPlayerKit.getPlugin().getConfig().getString("publickits." + key + ".icon"));
            PublicKit kit = new PublicKit(key, name, icon);
            publicKitList.add(kit);
        });


        String dbType = this.getConfig().getString("database.type");
        if (dbType == null) {
            this.database = new SQLite();
        } else if (dbType.equalsIgnoreCase("mysql")) {
            this.database = new MySQL();
        } else if (dbType.equalsIgnoreCase("sqlite")) {
            this.database = new SQLite();
        } else {
            this.database = new SQLite();

        }
        sqldata = new SQLGetter(this);


        try {
            database.connect();
        } catch (ClassNotFoundException | SQLException e) {
            //e.printStackTrace();
            Bukkit.getLogger().warning("Database connection failed!");
        }


        if (database.isConnected()) {
            Bukkit.getLogger().info("Database is connected!");
            sqldata.createTable();
            KitRoomDataManager.loadFromSQL();
            loadPublicKits();
            for (Player player : Bukkit.getOnlinePlayers()) {
                KitManager.loadFromSQL(player.getUniqueId());
            }


            new BukkitRunnable() {

                @Override
                public void run() {
                    if (database.isConnected()) {
                        sqldata.keepAlive();
                    } else {
                        Bukkit.getLogger().warning("Keep Alive Failed, attempting to reconnect database");
                        try {
                            database.connect();
                        } catch (SQLException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                        if (database.isConnected()) {
                            Bukkit.getLogger().info("Database is connected!");
                            sqldata.createTable();

                        } else {
                            Bukkit.getLogger().warning("Database connection failed!");
                        }
                    }


                }


            }.runTaskTimerAsynchronously(this, 25 * 20, 25 * 20);


        }

        this.getCommand("kit").setExecutor(new MainMenu());
        this.getCommand("sharekit").setExecutor(new ShareKitCommand());
        this.getCommand("copykit").setExecutor(new CopyKitCommand());
        this.getCommand("kitroom").setExecutor(new KitRoomCommands());
        this.getCommand("swapkit").setExecutor(new SwapKit());
        this.getCommand("deletekit").setExecutor(new DeleteKit());
        this.getCommand("inspectkit").setExecutor(new InspectKitCommand());
        this.getCommand("enderchest").setExecutor(new EnderchestCommand());
        this.getCommand("kitroom").setTabCompleter(new KitRoomTab());
        this.getCommand("savepublickit").setExecutor(new SavePublicKitCommand());
        this.getCommand("publickit").setExecutor(new PublicKitCommand());


        for (int i = 1; i <= 9; i++) {
            this.getCommand("k" + i).setExecutor(new ShortKitCommand());
        }

        for (int i = 1; i <= 9; i++) {
            this.getCommand("ec" + i).setExecutor(new ShortECCommand());
        }


        Bukkit.getPluginManager().registerEvents(new JoinListener(), this);
        Bukkit.getPluginManager().registerEvents(new QuitListener(), this);
        Bukkit.getPluginManager().registerEvents(new MenuFunctionListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitMenuCloseListener(), this);
        Bukkit.getPluginManager().registerEvents(new KitRoomSaveListener(), this);
        Bukkit.getPluginManager().registerEvents(new CommandListener(), this);
        Bukkit.getPluginManager().registerEvents(new RespawnListener(), this);


        startBroadcast();

    }

    @Override
    public void onDisable() {
        database.disconnect();

    }


    private void loadPublicKits() {
        for (PublicKit kit : publicKitList) {
            KitManager.loadSinglePublicKitFromSQL(kit.id);
        }

    }


    private void startBroadcast() {

        List<Component> messages = new ArrayList<>();
        this.getConfig().getStringList("scheduled-broadcast.messages").forEach(message -> {
            messages.add(MiniMessage.miniMessage().deserialize(message));
        });

        if (this.getConfig().getBoolean("scheduled-broadcast.enabled")) {
            Bukkit.getScheduler().runTaskTimer(this, () -> {
                for (Component message : messages) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(message);
                    }
                }
            }, 0, this.getConfig().getInt("scheduled-broadcast.period") * 20L);
        }

    }


}
