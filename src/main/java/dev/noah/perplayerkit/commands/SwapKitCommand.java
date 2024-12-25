package dev.noah.perplayerkit.commands;

import com.google.common.primitives.Ints;
import dev.noah.perplayerkit.KitManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            UUID uuid = player.getUniqueId();

            if (args.length == 2) {
                Integer slot1 = Ints.tryParse(args[0]);
                Integer slot2 = Ints.tryParse(args[1]);

                if (slot1 == null || slot2 == null) {
                    player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
                    player.sendMessage(ChatColor.RED + "Select real numbers");
                    return true;
                }
                KitManager kitManager = KitManager.get();

                if (kitManager.hasKit(uuid, slot1)) {
                    if (kitManager.hasKit(uuid, slot2)) {
                        ItemStack[] tempkit = kitManager.getPlayerKit(uuid, slot1).clone();
                        kitManager.savekit(uuid, slot1, kitManager.getPlayerKit(uuid, slot2), true);
                        kitManager.savekit(uuid, slot2, tempkit.clone(), true);
                        kitManager.saveEnderchestToDB(uuid, slot1);
                        kitManager.saveEnderchestToDB(uuid, slot2);

                        player.sendMessage(ChatColor.GREEN + "Kits " + slot1 + " and " + slot2 + " have been swapped!");


                    } else {
                        player.sendMessage(ChatColor.RED + "Kit " + slot2 + " doesn't exist!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Kit " + slot1 + " doesn't exist!");
                }


            } else {
                player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Only Players can use this!");

        }
        return true;
    }
}