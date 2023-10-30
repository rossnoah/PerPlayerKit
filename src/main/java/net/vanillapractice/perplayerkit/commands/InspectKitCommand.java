package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.KitManager;
import net.vanillapractice.perplayerkit.PerPlayerKit;
import net.vanillapractice.perplayerkit.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InspectKitCommand implements CommandExecutor {
    private final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player){
            Player p = (Player) sender;
            if(args.length>1){
                if(Integer.parseInt(args[1])>0&&Integer.parseInt(args[1])<10) {
            int slot = Integer.parseInt(args[1]);

                UUID target = UUID.fromString(args[0]);
                    if(Bukkit.getPlayer(target)==null){

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                KitManager.loadFromSQL(target);
                            }

                        }.runTaskAsynchronously(plugin);

                    }
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            if(KitManager.hasKit(target,slot)){

                                GUI main = new GUI(p);



                                main.InspectKit(p,target,slot);


                            }else{
                                p.sendMessage("§cPlayer does not have a kit in slot "+slot);
                            }
                        }

                    }.runTaskLater(plugin,40);


                    return true;



                }

            }
        }


        sender.sendMessage("§çError. You must be a player and use the correct format.");
        sender.sendMessage("§c/inspectkit <uuid> <slot>");


        return true;
    }
}
