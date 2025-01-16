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

import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;

public class KitRoomDataManager {


    private final ArrayList<ItemStack[]> kitroomData;
    private final Plugin plugin;
    private static KitRoomDataManager instance;

    public KitRoomDataManager(Plugin plugin) {
        this.plugin = plugin;
        kitroomData = new ArrayList<>();


        ItemStack[] defaultPage = new ItemStack[45];
        defaultPage[0] = ItemUtil.createItem(Material.BLUE_STAINED_GLASS_PANE, "&bDefault Kit Room Item");
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);
        kitroomData.add(defaultPage);

        ItemFilter.get().addToWhitelist(kitroomData);

        instance = this;
    }

    public static KitRoomDataManager get(){
        if(instance == null){
            throw new IllegalStateException("KitRoomDataManager has not been initialized yet!");
        }
        return instance;
    }

    public void setKitRoom(int page, ItemStack[] data) {
        kitroomData.set(page, data);

        ItemFilter.get().clearWhitelist();

        ItemFilter.get().addToWhitelist(kitroomData);

    }

    public ItemStack[] getKitRoomPage(int page) {
        return kitroomData.get(page);
    }

    public void saveToDBAsync() {
        new BukkitRunnable() {

            @Override
            public void run() {

                for (int i = 0; i < 5; i++) {
                    ItemStack[] pagedata = kitroomData.get(i);
                    String output = Serializer.itemStackArrayToBase64(pagedata);
                    PerPlayerKit.storageManager.saveKitDataByID(IDUtil.getKitRoomId(i), output);
                }
            }

        }.runTaskAsynchronously(plugin);


    }

    public void loadFromDB() {
        ItemFilter.get().clearWhitelist();
        for (int i = 0; i < 5; i++) {
            String input = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getKitRoomId(i));
            if (!input.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] pagedata = Serializer.itemStackArrayFromBase64(input);
                    kitroomData.set(i, pagedata);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        ItemFilter.get().addToWhitelist(kitroomData);
    }

}







