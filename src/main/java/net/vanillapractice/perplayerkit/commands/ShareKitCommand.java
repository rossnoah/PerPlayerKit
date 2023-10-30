package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.Cooldown;
import net.vanillapractice.perplayerkit.kitsharing.KitShareManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShareKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(sender instanceof Player){
            Player p = (Player)sender;

            if(args.length>0) {
                if(!Cooldown.isOnShareCooldown(p.getUniqueId())) {
                    KitShareManager.Sharekit(p, Integer.parseInt(args[0]));
                    Cooldown.updateKitroomCooldown(p.getUniqueId());
                }else{
                    p.sendMessage(ChatColor.RED+"Please dont spam the command (30 second cooldown)");
                }
            }else{
                p.sendMessage(ChatColor.RED+"Error, you must select a kit slot to share");
            }

        }else{
            sender.sendMessage("Only players can use this command");
        }



        return true;
    }
}
