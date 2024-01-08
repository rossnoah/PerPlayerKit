package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.Filter;
import net.vanillapractice.perplayerkit.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class SavePublicKitCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        //if not player
        if(!(sender instanceof Player)){
            sender.sendMessage("Only players can use this command");
            return true;
        }

        Player p = (Player) sender;

        //if not enough arguments

        if(args.length<1){
            p.sendMessage("Not enough arguments");
            p.sendMessage("/"+label+" <id>");
            return true;
        }

        Inventory inv = p.getInventory();

        ItemStack[] data = new ItemStack[41];
//        copy inventory into data
        for(int i=0;i<41;i++){
            data[i] = inv.getItem(i);
        }


        data= Filter.filterItemStack(data);

        //save kit
       boolean success = KitManager.savePublicKit(args[0],data);
         if(success){
             KitManager.saveSinglePublicKitToSQL(args[0]);
              p.sendMessage("Saved kit "+args[0]);
            }else{
                p.sendMessage("Error saving kit "+args[0]);

         }

         return true;



    }
}
