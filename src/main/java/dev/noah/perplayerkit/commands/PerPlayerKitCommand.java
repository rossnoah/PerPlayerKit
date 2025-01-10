package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.util.importutil.KitsXImporter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PerPlayerKitCommand implements CommandExecutor, TabCompleter {


    private Plugin plugin;
    public PerPlayerKitCommand(Plugin plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Missing arguments!");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "about":
                sender.sendMessage(ChatColor.GREEN + "PerPlayerKit is a plugin that allows players to have their own kits.");
                return true;
            case "import":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Missing import type!");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "kitsx":
                        sender.sendMessage(ChatColor.GREEN + "Starting import...");
                        KitsXImporter importer = new KitsXImporter(plugin,sender);
                        if(!importer.checkForFiles()){
                            sender.sendMessage(ChatColor.RED+"Missing files to import");
                            sender.sendMessage(ChatColor.RED+"Copy data folder from KitsX into the PerPlayerKit folder");
                        }
                        importer.importFiles();
                        sender.sendMessage(ChatColor.GREEN + "Attempted import of KitsX data!");

                        break;
                    default:
                        sender.sendMessage(ChatColor.RED + "Invalid import type!");
                        break;
                }
                return true;
            default:
                sender.sendMessage(ChatColor.RED + "Invalid subcommand!");
                return true;

        }
    }


    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if(args.length == 1) {
            return List.of("about", "import");
        }

        if(args.length == 2 && args[0].equalsIgnoreCase("import")) {
            return List.of("kitsx");
        }

        return null;
    }
}