package dev.noah.perplayerkit.commands.features;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.commands.core.CommandGuards;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.CooldownManager;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.StyleManager;
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

    private final Plugin plugin;
    private final CooldownManager commandCooldownManager;
    private final CooldownManager damageCooldownManager;
    private final boolean allowRegearWhileUsingElytra;
    private final boolean preventPuttingItemsInRegearInventory;
    private final ItemStack regearShulkerItem;
    private final ItemStack regearShellItem;

    public RegearCommand(Plugin plugin) {
        this.plugin = plugin;
        int commandCooldownInSeconds = plugin.getConfig().getInt("regear.command-cooldown", 5);
        int damageCooldownInSeconds = plugin.getConfig().getInt("regear.damage-timer", 5);
        this.commandCooldownManager = new CooldownManager(commandCooldownInSeconds);
        this.damageCooldownManager = new CooldownManager(damageCooldownInSeconds);
        this.allowRegearWhileUsingElytra = plugin.getConfig().getBoolean("regear.allow-while-using-elytra", true);
        this.preventPuttingItemsInRegearInventory = plugin.getConfig().getBoolean("regear.prevent-putting-items-in-regear-inventory", false);

        String primaryTag = StyleManager.get().getPrimaryColorTag();
        this.regearShulkerItem = ItemUtil.createItem(
                Material.WHITE_SHULKER_BOX,
                1,
                primaryTag + Lang.get().raw("gui.regear-shulker-name"),
                Lang.get().raw("gui.lore-regear-restocks"),
                Lang.get().raw("gui.lore-regear-shulker-use", "primary", primaryTag)
        );
        this.regearShellItem = ItemUtil.createItem(
                Material.SHULKER_SHELL,
                1,
                primaryTag + Lang.get().raw("gui.regear-shell-name"),
                Lang.get().raw("gui.lore-regear-restocks"),
                Lang.get().raw("gui.lore-regear-shell-click")
        );
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
        Player player = CommandGuards.requirePlayerInEnabledWorld(sender);
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

        Lang.get().send(player, "error.regear-misconfigured");
        return true;
    }

    @EventHandler
    public void onShulkerPlace(BlockPlaceEvent event) {
        if (!event.getItemInHand().equals(regearShulkerItem)) {
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
        Inventory inventory = createRegearInventory(holder);
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


        if (!currentItem.equals(regearShellItem)) {
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
            Lang.get().send(player, "error.inventory-full");
            return;
        }

        player.getInventory().setItem(slot, regearShulkerItem);
        Lang.get().send(player, "success.shulker-given");
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

        Lang.get().send(player, "error.no-kit-loaded");
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

        Lang.get().send(player, "error.regear-elytra-blocked");
        return true;
    }

    private boolean isDamageCooldownBlocked(Player player) {
        if (!damageCooldownManager.isOnCooldown(player)) {
            return false;
        }

        int secondsLeft = damageCooldownManager.getTimeLeft(player);
        Lang.get().send(player, "error.regear-combat-cooldown", "seconds", String.valueOf(secondsLeft));
        return true;
    }

    private boolean isCommandCooldownBlocked(Player player) {
        if (!commandCooldownManager.isOnCooldown(player)) {
            return false;
        }

        int secondsLeft = commandCooldownManager.getTimeLeft(player);
        Lang.get().send(player, "error.regear-command-cooldown", "seconds", String.valueOf(secondsLeft));
        return true;
    }

    private void announceRegearSuccess(Player player) {
        Lang.get().send(player, "success.regeared");
        BroadcastManager.get().broadcastPlayerRegeared(player);
    }


    private Inventory createRegearInventory(RegearInventoryHolder holder) {
        Inventory inventory = Bukkit.createInventory(holder, 27,
                StyleManager.get().getPrimaryColor() + Lang.get().legacy("gui.regear-shulker-title"));
        inventory.setItem(13, regearShellItem);
        holder.setInventory(inventory);
        return inventory;
    }


    public static class RegearInventoryHolder implements InventoryHolder {
        private final Player player;
        private Inventory inventory;

        public RegearInventoryHolder(Player player) {
            this.player = player;
        }

        public Player player() {
            return player;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
