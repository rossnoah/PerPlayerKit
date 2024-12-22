package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.Serializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SaveKit implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        Player p = (Player) commandSender;
        String uuid = p.getUniqueId().toString();
        int i = Integer.parseInt(strings[0]);
        PerPlayerKit.sqldata.saveMySQLKit(uuid + i, Serializer.itemStackArrayToBase64(PerPlayerKit.data.get(uuid + i)));
        p.sendMessage("saved to sql");
        p.sendMessage(Serializer.itemStackArrayToBase64(PerPlayerKit.data.get(uuid + i)));
        return true;
    }
}
