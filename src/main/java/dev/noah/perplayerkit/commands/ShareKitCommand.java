package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitShareManager;
import dev.noah.perplayerkit.util.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ShareKitCommand implements CommandExecutor, TabCompleter {

    CooldownManager shareKitCommandCooldown;

    public ShareKitCommand() {
        this.shareKitCommandCooldown = new CooldownManager(5);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.RED + "Error, you must select a kit slot to share");
            return true;
        }

        if (shareKitCommandCooldown.isOnCooldown(player)) {
            player.sendMessage(ChatColor.RED + "Please don't spam the command (5 second cooldown)");
            return true;
        }

        try{
            int slot = Integer.parseInt(args[0]);

            if (slot < 1 || slot > 9) {
                player.sendMessage(ChatColor.RED + "Select a valid kit slot");
                return true;
            }


        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Select a valid kit slot tht");
            return true;

        }

        KitShareManager.get().sharekit(player, Integer.parseInt(args[0]));
        shareKitCommandCooldown.setCooldown(player);

        return true;
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player player)) {
            return List.of();
        }
        return KitShareManager.get().getKitSlots(player);
    }
}
