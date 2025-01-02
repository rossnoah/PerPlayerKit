package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.CooldownManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;


public class RegearCommand implements CommandExecutor, Listener {

    private final Plugin plugin;
    private final int commandCooldownInSeconds;
    private final int damageCooldownInSeconds;
    private final CooldownManager commandCooldownManager;
    private final CooldownManager damageCooldownManager;

    public RegearCommand(Plugin plugin) {
        this.plugin = plugin;
        this.commandCooldownInSeconds = plugin.getConfig().getInt("regear.command-cooldown", 5);
        this.damageCooldownInSeconds = plugin.getConfig().getInt("regear.damage-timer", 5);
        this.commandCooldownManager = new CooldownManager(commandCooldownInSeconds);
        this.damageCooldownManager = new CooldownManager(damageCooldownInSeconds);
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
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        if (damageCooldownManager.isOnCooldown(player)) {
            int secondsLeft = damageCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize("<red>You must be out of combat for " + secondsLeft + " more seconds before regearing!"));
            return true;
        }

        if (commandCooldownManager.isOnCooldown(player)) {
            int secondsLeft = commandCooldownManager.getTimeLeft(player);
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize("<red>You must wait " + secondsLeft + " seconds before using this command again!"));
            return true;
        }

        int slot = KitManager.get().getLastKitLoaded(player.getUniqueId());

        if (slot == -1) {
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize("<red>You have not loaded a kit yet!"));
            return true;
        }

        KitManager.get().regearKit(player, slot);
        BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize("<green>Regeared!"));
        BroadcastManager.get().broadcastPlayerRegeared(player);

        commandCooldownManager.setCooldown(player);

        return true;
    }
}
