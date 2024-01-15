package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import net.vanillapractice.perplayerkit.gui.GUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.ipvp.canvas.Menu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PublicKitCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player)){
            sender.sendMessage("Only players can use this command");
            return true;
        }

        Player p = (Player) sender;

        //if args.length<1 open the kit menu
        if(args.length<1){
            GUI.OpenPublicKitMenu(p);
            return true;
        }

        //if args.length==1 open the kit menu with the kit
        String kitName = args[0];
        KitManager.loadPublicKit(p,kitName);

        return true;


    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if(args.length==1){

            List<String> list = new ArrayList<>();
            PerPlayerKit.publicKitList.forEach((kit) -> list.add(kit.id));

            return list;

        }

        return null;


    }
}
