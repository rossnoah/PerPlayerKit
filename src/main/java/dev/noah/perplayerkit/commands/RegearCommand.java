package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.BroadcastManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RegearCommand implements CommandExecutor {


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command!");
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

        return true;
    }
}
