package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.DisabledCommand;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.bukkit.scheduler.BukkitTask;
import dev.noah.perplayerkit.ConfigManager;

import java.util.concurrent.TimeUnit;

public class RegearCommand implements CommandExecutor, Listener {

    public static final ItemStack REGEAR_SHULKER_ITEM = ItemUtil.createItem(Material.WHITE_SHULKER_BOX, 1,
            ChatColor.BLUE + "Regear Shulker", "&7● Restocks Your Kit",
            "&7● Use &9/rg &7to get another regear shulker");
    public static final ItemStack REGEAR_SHELL_ITEM = ItemUtil.createItem(Material.SHULKER_SHELL, 1,
            ChatColor.BLUE + "Regear Shell", "&7● Restocks Your Kit", "&7● Click to use!");
    private final Plugin plugin;
    private CooldownManager commandCooldownManager;
    private CooldownManager damageCooldownManager;
    private boolean allowRegearWhileUsingElytra;
    private boolean preventPuttingItemsInRegearInventory;

    private static RegearCommand instance;

    public RegearCommand(Plugin plugin) {
        this.plugin = plugin;
        reloadConfig();
        instance = this;
    }

    public static RegearCommand getInstance() {
        return instance;
    }

    /**
     * Reloads configuration values for the RegearCommand
     */
    public void reloadConfig() {
        int commandCooldownInSeconds = ConfigManager.get().getRegearCommandCooldown();
        int damageCooldownInSeconds = ConfigManager.get().getRegearDamageTimer();
        this.commandCooldownManager = new CooldownManager(commandCooldownInSeconds);
        this.damageCooldownManager = new CooldownManager(damageCooldownInSeconds);
        this.allowRegearWhileUsingElytra = ConfigManager.get().isRegearAllowWhileUsingElytra();
        this.preventPuttingItemsInRegearInventory = ConfigManager.get().isRegearPreventPuttingItemsInInventory();
    }

    @EventHandler
    public void onPlayerTakesDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        damageCooldownManager.setCooldown(player);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }

        if (ConfigManager.get().getRegearMode().equalsIgnoreCase("shulker")) {
            int slot = player.getInventory().firstEmpty();
            if (slot == -1) {
                BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage()
                        .deserialize("<red>Your inventory is full, can't give you a regear shulker!"));
                return true;
            }

            player.getInventory().setItem(slot, REGEAR_SHULKER_ITEM);
            BroadcastManager.get().sendComponentMessage(player,
                    MiniMessage.miniMessage().deserialize("<green>Regear Shulker given!"));

            return true;
        }

        if (ConfigManager.get().getRegearMode().equalsIgnoreCase("command")) {
            int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());

            if (slot == -1) {
                BroadcastManager.get().sendComponentMessage(player,
                        MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
                return true;
            }

            if (!allowRegearWhileUsingElytra && player.isGliding() && player.getInventory().getChestplate() != null
                    && player.getInventory().getChestplate().getType() == Material.ELYTRA) {
                BroadcastManager.get().sendComponentMessage(player,
                        MiniMessage.miniMessage().deserialize("<red>You cannot regear while using an elytra!"));
                return true;
            }

            if (damageCooldownManager.isOnCooldown(player)) {
                int secondsLeft = damageCooldownManager.getTimeLeft(player);
                BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize(
                        "<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
                return true;
            }

            if (commandCooldownManager.isOnCooldown(player)) {
                int secondsLeft = commandCooldownManager.getTimeLeft(player);
                BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize(
                        "<red>You must wait " + secondsLeft + " seconds before using this command again!"));
                return true;
            }

            KitManager.get().regearKit(player, slot);
            BroadcastManager.get().sendComponentMessage(player,
                    MiniMessage.miniMessage().deserialize("<green>Regeared!"));
            BroadcastManager.get().broadcastPlayerRegeared(player);

            commandCooldownManager.setCooldown(player);

            return true;
        }

        BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage()
                .deserialize("<red>This command is not configured correctly, please contact an administrator."));
        return true;
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (!event.getItemInHand().equals(REGEAR_SHULKER_ITEM)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();

        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());

        if (slot == -1) {
            BroadcastManager.get().sendComponentMessage(player,
                    MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
            return;
        }

        if (damageCooldownManager.isOnCooldown(player)) {
            int secondsLeft = damageCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize(
                    "<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
            return;
        }

        player.getInventory().setItem(event.getHand(), null);

        // custom inv with holder
        RegearInventoryHolder holder = new RegearInventoryHolder(player);
        Inventory inventory = holder.getInventory();
        player.openInventory(inventory);
    }

    @EventHandler
    public void onShulkerShellClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RegearInventoryHolder holder)) {
            return;
        }
        ItemStack currentItem = event.getCurrentItem();

        if (currentItem == null) {
            return;
        }

        if (!currentItem.equals(REGEAR_SHELL_ITEM)) {
            if (preventPuttingItemsInRegearInventory) {
                event.setCancelled(true);
            }
            return;
        }

        Player player = holder.player();

        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());

        if (slot == -1) {
            BroadcastManager.get().sendComponentMessage(player,
                    MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
            return;
        }

        if (damageCooldownManager.isOnCooldown(player)) {
            int secondsLeft = damageCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize(
                    "<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
            return;
        }

        player.closeInventory();

        KitManager.get().regearKit(player, slot);
        player.updateInventory();

        BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize("<green>Regeared!"));
        BroadcastManager.get().broadcastPlayerRegeared(player);
    }

    public record RegearInventoryHolder(
            Player player) implements InventoryHolder {

        @Override
        public @NotNull Inventory getInventory() {
            Inventory inventory = Bukkit.createInventory(this, 27, ChatColor.BLUE + "Regear Shulker");
            inventory.setItem(13, REGEAR_SHELL_ITEM);
            return inventory;
        }
    }
}
