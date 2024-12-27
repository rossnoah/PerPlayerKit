package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.gui.GUI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class InspectKitCommand implements CommandExecutor {
    private Plugin plugin;
    public InspectKitCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length > 1) {
                if (Integer.parseInt(args[1]) > 0 && Integer.parseInt(args[1]) < 10) {
                    int slot = Integer.parseInt(args[1]);

                    UUID target = UUID.fromString(args[0]);
                    if (Bukkit.getPlayer(target) == null) {

                        new BukkitRunnable() {

                            @Override
                            public void run() {
                                KitManager.get().loadPlayerDataFromDB(target);
                            }

                        }.runTaskAsynchronously(plugin);

                    }
                    new BukkitRunnable() {

                        @Override
                        public void run() {
                            if (KitManager.get().hasKit(target, slot)) {

                                GUI main = new GUI(plugin);


                                main.InspectKit(player, target, slot);


                            } else {
                                player.sendMessage("§cPlayer does not have a kit in slot " + slot);
                            }
                        }

                    }.runTaskLater(plugin, 40);


                    return true;


                }

            }
        }


        sender.sendMessage("§çError. You must be a player and use the correct format.");
        sender.sendMessage("§c/inspectkit <uuid> <slot>");


        return true;
    }
}
