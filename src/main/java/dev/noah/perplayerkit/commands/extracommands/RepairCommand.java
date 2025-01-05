package dev.noah.perplayerkit.commands.extracommands;

import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.PlayerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RepairCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(!(sender instanceof Player player)){
            sender.sendMessage("Only players can use this command!");
            return true;
        }

        BroadcastManager.get().broadcastPlayerRepaired(player);
        PlayerUtil.repairAll(player);
        return true;
    }
}
