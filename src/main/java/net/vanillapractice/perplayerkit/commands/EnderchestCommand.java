package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.EnderChestUtil;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EnderchestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player){
            Player p = (Player) sender;

            if(args.length==1){
                if(args[0].equalsIgnoreCase("save")){
                    EnderChestUtil.saveEnderChest(p);
                    return true;
                }
                if(args[0].equalsIgnoreCase("load")){
                    EnderChestUtil.loadEnderChest(p.getUniqueId());
                    return true;
                }
            }
            p.sendMessage(ChatColor.RED+"Error. Incorrect Usage");
            p.sendMessage("/"+label+" <save> to save | "+label+" <load> to load");


        }

        sender.sendMessage("Only players can use this command");
        return true;
    }
}
