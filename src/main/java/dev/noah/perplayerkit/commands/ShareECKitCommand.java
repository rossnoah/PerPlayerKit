package dev.noah.perplayerkit.commands;

import com.google.common.primitives.Ints;
import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.util.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShareECKitCommand implements CommandExecutor {

    private final CooldownManager shareECCommandCooldown;

    public ShareECKitCommand() {
        this.shareECCommandCooldown = new CooldownManager(5);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Error, you must select a EC slot to share");
            return true;
        }

        if (shareECCommandCooldown.isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Please don't spam the command (5 second cooldown)");
            return true;
        }

        Integer slot = Ints.tryParse(args[0]);

        if (slot == null || slot < 1 || slot > 9) {
            player.sendMessage(ChatColor.RED + "Select a valid kit slot");
            return true;
        }

        KitShareManager.get().shareEC(player, slot);
        shareECCommandCooldown.setCooldown(player);

        return true;
    }
}
