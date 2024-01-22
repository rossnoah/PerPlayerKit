package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.DisabledCommand;
import net.vanillapractice.perplayerkit.KitManager;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class KitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        if(commandSender instanceof Player){
            Player player = (Player) commandSender;

            if(DisabledCommand.isBlockedInWorld(player)){
                return true;
            }

            UUID playeruuid = player.getUniqueId();

            if (strings.length>=2){
                if(strings[0].equalsIgnoreCase("load")){

                    if(NumberUtils.isNumber(strings[1])){
                        int slot = Integer.parseInt(strings[1]);

                        if(slot>0&&slot<=9){


                            if (KitManager.loadkit(playeruuid,slot)){

                            }else{
                                player.sendMessage(ChatColor.RED+"Kit loading failed!");
                            }

                        }else{
                            player.sendMessage(ChatColor.RED+"Select slot 1-9");

                        }

                    }else{
                        player.sendMessage(ChatColor.RED+"Enter a valid number");
                    }

                }
                if(strings[0].equalsIgnoreCase("save")){

                    if(NumberUtils.isNumber(strings[1])){
                        int slot = Integer.parseInt(strings[1]);

                        if(slot>0&&slot<=9){


                            if (KitManager.savekit(playeruuid,slot)){
                                player.sendMessage(ChatColor.GREEN+"Kit saved in slot "+slot+"!");

                            }else{
                                player.sendMessage(ChatColor.RED+"Kit loading failed!");
                            }

                        }else{
                            player.sendMessage(ChatColor.RED+"Select slot 1-9");

                        }

                    }else{
                        player.sendMessage(ChatColor.RED+"Enter a valid number");
                    }

                }

                if(!(strings[0].equalsIgnoreCase("load")||strings[0].equalsIgnoreCase("save"))){
                    player.sendMessage(ChatColor.RED+"Usage: /kit <load/save> <slot>");
                }


            }else {
                player.sendMessage(ChatColor.RED + "Usage: /kit <load/save> <slot>");
            }


        }else{
            commandSender.sendMessage(ChatColor.RED+"Only Players can use this!");

        }


        return true;
    }
}
