package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.DisabledCommand;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PublicKitCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command");
            return true;
        }

        if (DisabledCommand.isBlockedInWorld(player)) {
            return true;
        }

        //if args.length<1 open the kit menu
        if (args.length < 1) {
            GUI kitMenu = new GUI();
            kitMenu.OpenPublicKitMenu(player);
            return true;
        }

        //if args.length==1 open the kit menu with the kit
        String kitName = args[0];
        KitManager.get().loadPublicKit(player, kitName);

        return true;


    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (args.length == 1) {

            List<String> list = new ArrayList<>();
            KitManager.get().getPublicKitList().forEach((kit) -> list.add(kit.id));

            return list;

        }

        return null;


    }
}
