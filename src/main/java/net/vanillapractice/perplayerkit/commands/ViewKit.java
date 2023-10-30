package net.vanillapractice.perplayerkit.commands;

import net.vanillapractice.perplayerkit.gui.GUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ViewKit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player p = (Player) commandSender;
        String uuid = p.getUniqueId().toString();

        GUI main = new GUI(p);
        main.OpenKitKenu(p,Integer.parseInt(strings[0]));

        return true;
    }
}
