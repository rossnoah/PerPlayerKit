package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.util.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShareKitCommand implements CommandExecutor {

    CooldownManager shareKitCommandCooldown;

    public ShareKitCommand() {
        this.shareKitCommandCooldown = new CooldownManager(30);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        Player p = (Player) sender;

        if (args.length < 0) {
            p.sendMessage(ChatColor.RED + "Error, you must select a kit slot to share");
            return true;
        }

        if (shareKitCommandCooldown.isOnCooldown(p)) {
            p.sendMessage(ChatColor.RED + "Please don't spam the command (30 second cooldown)");
            return true;
        }

        KitShareManager.get().sharekit(p, Integer.parseInt(args[0]));
        shareKitCommandCooldown.setCooldown(p);

        return true;
    }
}
