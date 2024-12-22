package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Repair implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player p = (Player) commandSender;
        String uuid = p.getUniqueId().toString();
        PlayerUtils.repairAll(p);
        p.sendMessage("repair");
        return true;
    }
}
