package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.StyleManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
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

public class RegearCommand implements CommandExecutor, Listener {

    public static final ItemStack REGEAR_SHULKER_ITEM = ItemUtil.createItem(Material.WHITE_SHULKER_BOX, 1, StyleManager.get().getPrimaryColor() + "Regear Shulker", "<gray>● Restocks Your Kit</gray>", "<gray>● Use </gray>" + StyleManager.get().getPrimaryColor() + "<gray>/rg to get another regear shulker</gray>");
    public static final ItemStack REGEAR_SHELL_ITEM = ItemUtil.createItem(Material.SHULKER_SHELL, 1, StyleManager.get().getPrimaryColor() + "Regear Shell", "<gray>● Restocks Your Kit</gray>", "<gray>● Click to use!</gray>");
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final Plugin plugin;
    private final CooldownManager commandCooldownManager;
    private final CooldownManager damageCooldownManager;
    private final boolean allowRegearWhileUsingElytra;
    private final boolean preventPuttingItemsInRegearInventory;

    public RegearCommand(Plugin plugin) {
        this.plugin = plugin;
        int commandCooldownInSeconds = plugin.getConfig().getInt("regear.command-cooldown", 5);
        int damageCooldownInSeconds = plugin.getConfig().getInt("regear.damage-timer", 5);
        this.commandCooldownManager = new CooldownManager(commandCooldownInSeconds);
        this.damageCooldownManager = new CooldownManager(damageCooldownInSeconds);
        this.allowRegearWhileUsingElytra = plugin.getConfig().getBoolean("regear.allow-while-using-elytra", true);
        this.preventPuttingItemsInRegearInventory = plugin.getConfig().getBoolean("regear.prevent-putting-items-in-regear-inventory", false);
    }

    @EventHandler
    public void onPlayerTakesDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        damageCooldownManager.setCooldown(player);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Player player = CommandGuards.requirePlayerInEnabledWorld(sender, "Only players can use this command!");
        if (player == null) {
            return true;
        }

        String effectiveMode = getEffectiveMode(label);
        if (effectiveMode.equalsIgnoreCase("shulker")) {
            handleShulkerMode(player);
            return true;
        }

        if (effectiveMode.equalsIgnoreCase("command")) {
            handleCommandMode(player);
            return true;
        }

        sendMessage(player, "<red>This command is not configured correctly, please contact an administrator.");
        return true;
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (!event.getItemInHand().equals(REGEAR_SHULKER_ITEM)) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();

        Integer slot = getLastLoadedKitSlot(player);
        if (slot == null) {
            return;
        }

        if (isDamageCooldownBlocked(player)) {
            return;
        }

        player.getInventory().setItem(event.getHand(), null);

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

        Integer slot = getLastLoadedKitSlot(player);
        if (slot == null) {
            return;
        }

        if (isDamageCooldownBlocked(player)) {
            return;
        }

        player.closeInventory();

        KitManager.get().regearKit(player, slot);
        player.updateInventory();

        announceRegearSuccess(player);
    }

    private String getEffectiveMode(String label) {
        if (label.equalsIgnoreCase("rg")) {
            return plugin.getConfig().getString("regear.rg-mode", "command");
        }
        if (label.equalsIgnoreCase("regear")) {
            return plugin.getConfig().getString("regear.regear-mode", "command");
        }
        return plugin.getConfig().getString("regear.rg-mode", "command");
    }

    private void handleShulkerMode(Player player) {
        int slot = player.getInventory().firstEmpty();
        if (slot == -1) {
            sendMessage(player, "<red>Your inventory is full, can't give you a regear shulker!");
            return;
        }

        player.getInventory().setItem(slot, REGEAR_SHULKER_ITEM);
        sendMessage(player, "<green>Regear Shulker given!");
    }

    private void handleCommandMode(Player player) {
        Integer slot = getLastLoadedKitSlot(player);
        if (slot == null) {
            return;
        }
        if (isElytraBlocked(player)) {
            return;
        }
        if (isDamageCooldownBlocked(player)) {
            return;
        }
        if (isCommandCooldownBlocked(player)) {
            return;
        }

        KitManager.get().regearKit(player, slot);
        announceRegearSuccess(player);
        commandCooldownManager.setCooldown(player);
    }

    private Integer getLastLoadedKitSlot(Player player) {
        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());
        if (slot != -1) {
            return slot;
        }

        sendMessage(player, "<red>You have not loaded a kit yet!");
        return null;
    }

    private boolean isElytraBlocked(Player player) {
        if (allowRegearWhileUsingElytra) {
            return false;
        }
        if (!player.isGliding()) {
            return false;
        }
        if (player.getInventory().getChestplate() == null || player.getInventory().getChestplate().getType() != Material.ELYTRA) {
            return false;
        }

        sendMessage(player, "<red>You cannot regear while using an elytra!");
        return true;
    }

    private boolean isDamageCooldownBlocked(Player player) {
        if (!damageCooldownManager.isOnCooldown(player)) {
            return false;
        }

        int secondsLeft = damageCooldownManager.getTimeLeft(player);
        sendMessage(player, "<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!");
        return true;
    }

    private boolean isCommandCooldownBlocked(Player player) {
        if (!commandCooldownManager.isOnCooldown(player)) {
            return false;
        }

        int secondsLeft = commandCooldownManager.getTimeLeft(player);
        sendMessage(player, "<red>You must wait " + secondsLeft + " seconds before using this command again!");
        return true;
    }

    private void announceRegearSuccess(Player player) {
        sendMessage(player, "<green>Regeared!");
        BroadcastManager.get().broadcastPlayerRegeared(player);
    }

    private void sendMessage(Player player, String message) {
        BroadcastManager.get().sendComponentMessage(player, MM.deserialize(message));
    }


    public record RegearInventoryHolder(
            Player player) implements InventoryHolder {

        @Override
        public @NotNull Inventory getInventory() {
            Inventory inventory = Bukkit.createInventory(this, 27, StyleManager.get().getPrimaryColor() + "Regear Shulker");
            inventory.setItem(13, REGEAR_SHELL_ITEM);
            return inventory;
        }
    }
}
