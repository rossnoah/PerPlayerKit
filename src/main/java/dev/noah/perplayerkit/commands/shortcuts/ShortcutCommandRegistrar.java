/*
 * Copyright 2022-2025 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.commands.shortcuts;

import dev.noah.perplayerkit.util.KitSlots;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Wires all k&lt;N&gt;/ec&lt;N&gt; shortcut commands: slots 1..9 are declared
 * statically in plugin.yml, while slots above 9 (when max-kits is raised) can
 * only be registered at runtime through the server CommandMap.
 */
public final class ShortcutCommandRegistrar {

    // Number of k<N>/ec<N> commands statically declared in plugin.yml.
    private static final int STATIC_COMMAND_SLOTS = 9;

    private ShortcutCommandRegistrar() {
    }

    public static void registerAll(JavaPlugin plugin) {
        CommandExecutor kitExecutor = new ShortKitCommand();
        CommandExecutor ecExecutor = new ShortECCommand();
        for (int i = 1; i <= STATIC_COMMAND_SLOTS; i++) {
            plugin.getCommand("k" + i).setExecutor(kitExecutor);
            plugin.getCommand("ec" + i).setExecutor(ecExecutor);
        }

        if (KitSlots.maxKits() <= STATIC_COMMAND_SLOTS) {
            return;
        }

        CommandMap commandMap;
        try {
            // getCommandMap() is public on CraftServer but not part of the Spigot
            // API this plugin compiles against, hence reflection.
            commandMap = (CommandMap) plugin.getServer().getClass().getMethod("getCommandMap").invoke(plugin.getServer());
        } catch (ReflectiveOperationException | ClassCastException e) {
            plugin.getLogger().warning("Could not access the server command map; /k10+ and /ec10+ shortcut commands "
                    + "are unavailable. Kit slots above " + STATIC_COMMAND_SLOTS + " are still accessible through the GUI.");
            return;
        }

        // Dynamic commands inherit the permissions plugin.yml gives k1/ec1.
        String kitPermission = plugin.getCommand("k1").getPermission();
        String ecPermission = plugin.getCommand("ec1").getPermission();
        for (int i = STATIC_COMMAND_SLOTS + 1; i <= KitSlots.maxKits(); i++) {
            register(plugin, commandMap, new DynamicSlotCommand(plugin, "k" + i, "kit" + i, kitPermission, kitExecutor));
            register(plugin, commandMap, new DynamicSlotCommand(plugin, "ec" + i, "enderchest" + i, ecPermission, ecExecutor));
        }
    }

    private static void register(Plugin plugin, CommandMap commandMap, DynamicSlotCommand command) {
        if (!commandMap.register("perplayerkit", command)) {
            plugin.getLogger().warning("Command /" + command.getName() + " is already taken by another plugin; "
                    + "use /perplayerkit:" + command.getName() + " instead.");
        }
    }
}
