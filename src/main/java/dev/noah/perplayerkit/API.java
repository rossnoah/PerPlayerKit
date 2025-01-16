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
package dev.noah.perplayerkit;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class API {

    private static API instance;

    private API() {
        instance = this;
    }

    public static API getInstance() {
        if (instance == null) {
            instance = new API();
        }
        return instance;
    }


    public List<PublicKit> getPublicKits() {
        List<PublicKit> originalList = KitManager.get().getPublicKitList();
        return new ArrayList<>(originalList);
    }

    public void loadPublicKit(Player player, PublicKit kit) {
        KitManager.get().loadPublicKitSilent(player, kit.id);
    }


}
