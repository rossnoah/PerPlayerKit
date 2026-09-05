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
import dev.noah.perplayerkit.starter.StarterSetup;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KitRoomDataManager {

    /** Pages in the kit room. Fixed by the five category buttons in the menu. */
    public static final int PAGE_COUNT = 5;

    private final ArrayList<ItemStack[]> kitroomData;
    private final Plugin plugin;
    private static KitRoomDataManager instance;
    private boolean loadedAnyPage;
    private final java.util.Set<Integer> storedPages = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public KitRoomDataManager(Plugin plugin) {
        this.plugin = plugin;
        kitroomData = new ArrayList<>();

        for (int i = 0; i < PAGE_COUNT; i++) {
            ItemStack[] defaultPage = new ItemStack[45];
            // An unsaved page is empty. Placeholders must never become whitelisted kit content.
            kitroomData.add(defaultPage);
        }

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
        kitroomData.set(page, ItemFilter.copy(data));

        ItemFilter.get().clearWhitelist();

        ItemFilter.get().addToWhitelist(kitroomData);

    }

    public ItemStack[] getKitRoomPage(int page) {
        return ItemFilter.copy(kitroomData.get(page));
    }

    public void saveToDBAsync() {
        List<Integer> allPages = new ArrayList<>(PAGE_COUNT);
        for (int i = 0; i < PAGE_COUNT; i++) {
            allPages.add(i);
        }
        savePagesToDBAsync(allPages);
    }

    /**
     * Writes only the given pages. Used when seeding starter content so pages
     * that have never been configured are not overwritten with placeholders.
     */
    public void savePagesToDBAsync(List<Integer> pages) {
        List<Integer> targets = List.copyOf(pages);
        storedPages.addAll(targets);
        if (!targets.isEmpty()) {
            // This server has a kit room now, so stop offering to make one.
            StarterSetup.notifyKitRoomSaved();
        }
        for (int page : targets) {
            String output = Serializer.itemStackArrayToBase64(ItemFilter.copy(kitroomData.get(page)));
            KitManager.get().queueWrite(IDUtil.getKitRoomId(page), output);
        }
    }

    /** Whether this page has ever been saved on this server. */
    public static boolean hasStoredPage(int page) {
        return get().storedPages.contains(page);
    }

    /**
     * Whether the last {@link #loadFromDB()} found any saved page. Lets callers
     * tell a never-configured kit room from a real one without re-reading
     * storage.
     */
    public boolean loadedAnyPage() {
        return loadedAnyPage;
    }

    public void loadFromDB() {
        ItemFilter.get().clearWhitelist();
        loadedAnyPage = false;
        storedPages.clear();
        for (int i = 0; i < PAGE_COUNT; i++) {
            String input = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getKitRoomId(i));
            if (!input.equalsIgnoreCase("error")) {
                loadedAnyPage = true;
                storedPages.add(i);
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







