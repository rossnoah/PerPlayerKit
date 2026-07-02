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

import org.bukkit.command.Command;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A slot shortcut command (e.g. /k10, /ec42) registered at runtime for kit
 * slots above 9, which cannot be declared statically in plugin.yml because the
 * count depends on the max-kits config option.
 */
public class DynamicSlotCommand extends Command implements PluginIdentifiableCommand {

    private final Plugin plugin;
    private final CommandExecutor delegate;

    public DynamicSlotCommand(Plugin plugin, String name, String alias, String permission, CommandExecutor delegate) {
        super(name);
        this.plugin = plugin;
        this.delegate = delegate;
        setAliases(List.of(alias));
        setPermission(permission);
        setUsage("/" + name);
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!plugin.isEnabled()) {
            // Match PluginCommand: statically declared commands refuse to run
            // once the owning plugin is disabled.
            throw new CommandException("Cannot execute command '" + label + "' in plugin "
                    + plugin.getDescription().getFullName() + " - plugin is disabled.");
        }
        if (!testPermission(sender)) {
            return true;
        }
        return delegate.onCommand(sender, this, label, args);
    }

    @Override
    public @NotNull Plugin getPlugin() {
        return plugin;
    }
}
