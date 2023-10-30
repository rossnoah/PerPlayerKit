package net.vanillapractice.perplayerkit.commands;

import com.google.common.primitives.Ints;
import net.vanillapractice.perplayerkit.KitManager;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class SwapKit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            UUID uuid = player.getUniqueId();

            if (args.length==2){
                Integer slot1 = Ints.tryParse(args[0]);
                Integer slot2 = Ints.tryParse(args[1]);

                if(slot1!=null&&slot2!=null) {


                    if (KitManager.hasKit(uuid, slot1)) {
                        if (KitManager.hasKit(uuid, slot2)) {
                            ItemStack[] tempkit = KitManager.getKit(uuid, slot1).clone();
                            KitManager.savekit(uuid, slot1, KitManager.getKit(uuid, slot2), true);
                            KitManager.savekit(uuid, slot2, tempkit.clone(), true);

                            player.sendMessage(ChatColor.GREEN + "Kits " + slot1 + " and " + slot2 + " have been swapped!");


                        } else {
                            player.sendMessage(ChatColor.RED + "Kit " + slot2 + " doesnt exist!");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Kit " + slot1 + " doesnt exist!");
                    }
                }else {
                    player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
                    player.sendMessage(ChatColor.RED + "Select real numbers");
                }


            }else {
                player.sendMessage(ChatColor.RED + "Usage: /swapkit <slot1> <slot2>");
            }
        }else{
            sender.sendMessage(ChatColor.RED+"Only Players can use this!");

        }


        return true;
    }
}