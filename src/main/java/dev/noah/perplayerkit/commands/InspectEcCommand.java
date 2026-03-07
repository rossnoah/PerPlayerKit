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
package dev.noah.perplayerkit.commands;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.gui.GUI;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.UUID;
public class InspectEcCommand extends AbstractInspectCommand {

    public InspectEcCommand(Plugin plugin) {
        super(plugin);
    }

    @Override
    protected String usageCommand() {
        return "inspectec";
    }

    @Override
    protected boolean hasData(UUID targetUuid, int slot) {
        return KitManager.get().hasEC(targetUuid, slot);
    }

    @Override
    protected void openInspectGui(Player inspector, UUID targetUuid, int slot) {
        GUI gui = new GUI(plugin);
        gui.InspectEc(inspector, targetUuid, slot);
    }

    @Override
    protected String missingDataMessage(String targetName, int slot) {
        return "<red>" + targetName + " does not have an enderchest in slot " + slot + "</red>";
    }

    @Override
    protected String loadErrorLogMessage() {
        return "Error loading enderchest data";
    }

    @Override
    protected String loadErrorUserMessage() {
        return "<red>An error occurred while loading enderchest data. See console for details.</red>";
    }
}
