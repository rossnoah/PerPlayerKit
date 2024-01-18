package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.DisabledCommand;
import net.vanillapractice.perplayerkit.KitManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.type.ChestMenu;
import org.jetbrains.annotations.NotNull;

public class EnderchestCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player){
            Player p = (Player) sender;

            if(DisabledCommand.isBlockedInWorld(p)){
                return true;
            }

//            if(args.length==1){
//                if(args[0].equalsIgnoreCase("save")){
//                    EnderChestUtil.saveEnderChest(p);
//                    return true;
//                }
//                if(args[0].equalsIgnoreCase("load")){
//                    EnderChestUtil.loadEnderChest(p.getUniqueId());
//                    return true;
//                }
//            }
//            p.sendMessage(ChatColor.RED+"Error. Incorrect Usage");
//            p.sendMessage("/"+label+" <save> to save | "+label+" <load> to load");
//
//            ensure there is a number 1-9 as the argument
//            if(args.length==1){
//                try{
//                    int slot = Integer.parseInt(args[0]);
//                    if(slot>0&&slot<10){
//                        KitManager.loadEC(p.getUniqueId(),slot);
//                        return true;
//                    }
//                }catch (NumberFormatException e){
//                    p.sendMessage(ChatColor.RED+"Error. Incorrect Usage");
//                    p.sendMessage("/"+label+" <1-9>");
//                    return true;
//                }
//            }
           viewOnlyEC(p);
            return true;
        }

        sender.sendMessage("Only players can use this command");
        return true;
    }
    public void viewOnlyEC(Player p){
        Inventory inv = Bukkit.createInventory(null,45, ChatColor.BLUE+"View Only Enderchest");
//        fill first 9 and last 9 with purple glass
        ItemStack fill = new ItemStack(Material.BLUE_STAINED_GLASS_PANE);
//        set name to nothing
        fill.getItemMeta().setDisplayName("");

        Menu menu = ChestMenu.builder(5).title(ChatColor.BLUE+"View Only Enderchest").build();


        for(int i = 0;i<9;i++){
            menu.getSlot(i).setItem(fill);
        }
        for(int i = 36;i<45;i++){
            menu.getSlot(i).setItem(fill);
        }
//        set the items in the inventory to the items in the enderchest
        ItemStack[] items = p.getEnderChest().getContents();
        for(int i = 0;i<27;i++){
            menu.getSlot(i+9).setItem(items[i]);
        }
        menu.open(p);
    }
}


