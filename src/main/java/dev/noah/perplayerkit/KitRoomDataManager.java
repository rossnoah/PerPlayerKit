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

    public static final int DEFAULT_PAGE_COUNT = 5;
    public static final int MAX_PAGE_COUNT = 99;
    public static final int BUTTONS_PER_GROUP = 5;
    /** Retained for integrations compiled against the original default. */
    @Deprecated public static final int PAGE_COUNT = DEFAULT_PAGE_COUNT;
    private final int pageCount;

    public static int configuredPages(org.bukkit.configuration.ConfigurationSection config) {
        if (!config.contains("kitroom.pages")) return DEFAULT_PAGE_COUNT;
        if (!config.isInt("kitroom.pages") || config.getInt("kitroom.pages") < 1 || config.getInt("kitroom.pages") > MAX_PAGE_COUNT)
            throw new IllegalArgumentException("kitroom.pages must be a whole number from 1 to " + MAX_PAGE_COUNT);
        return config.getInt("kitroom.pages");
    }

    public int getPageCount() { return pageCount; }

    private final ArrayList<ItemStack[]> kitroomData;
    private final Plugin plugin;
    private static KitRoomDataManager instance;
    private boolean loadedAnyPage;
    private final java.util.Set<Integer> storedPages = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public KitRoomDataManager(Plugin plugin) {
        this.plugin = plugin;
        this.pageCount = configuredPages(plugin.getConfig());
        kitroomData = new ArrayList<>();

        for (int i = 0; i < MAX_PAGE_COUNT; i++) {
            ItemStack[] defaultPage = new ItemStack[KitContents.ROOM_SIZE];
            // An unsaved page is empty. Placeholders must never become whitelisted kit content.
            kitroomData.add(defaultPage);
        }

        ItemFilter.get().addToWhitelist(kitroomData.subList(0, pageCount));

        instance = this;
    }

    public static KitRoomDataManager get(){
        if(instance == null){
            throw new IllegalStateException("KitRoomDataManager has not been initialized yet!");
        }
        return instance;
    }

    public void setKitRoom(int page, ItemStack[] data) {
        if (page < 0 || page >= MAX_PAGE_COUNT || !KitContents.hasSize(data, KitContents.ROOM_SIZE))
            throw new IllegalArgumentException("Expected a kit room page from 0 to 98 with 45 slots");
        kitroomData.set(page, KitContents.copy(data));

        ItemFilter.get().clearWhitelist();

        ItemFilter.get().addToWhitelist(kitroomData.subList(0, pageCount));

    }

    public ItemStack[] getKitRoomPage(int page) {
        return KitContents.copy(kitroomData.get(page));
    }

    public void saveToDBAsync() {
        List<Integer> allPages = new ArrayList<>(pageCount);
        for (int i = 0; i < pageCount; i++) {
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
        if (targets.stream().anyMatch(page -> page < 0 || page >= MAX_PAGE_COUNT))
            throw new IllegalArgumentException("Invalid kit room page");
        storedPages.addAll(targets);
        if (targets.stream().anyMatch(page -> page < pageCount)) {
            loadedAnyPage = true;
            // This server has a kit room now, so stop offering to make one.
            StarterSetup.notifyKitRoomSaved();
        }
        for (int page : targets) {
            String output = Serializer.itemStackArrayToBase64(KitContents.copy(kitroomData.get(page)));
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
        for (int i = 0; i < pageCount; i++) {
            String input = PerPlayerKit.storageManager.getKitDataByID(IDUtil.getKitRoomId(i));
            if (input != null && !input.equalsIgnoreCase("error")) {
                loadedAnyPage = true;
                storedPages.add(i);
                try {
                    ItemStack[] pagedata = Serializer.itemStackArrayFromBase64(input);
                    if (!KitContents.hasSize(pagedata, KitContents.ROOM_SIZE)) throw new IOException("Expected " + KitContents.ROOM_SIZE + " kit room slots");
                    kitroomData.set(i, pagedata);

                } catch (IOException e) {
                    plugin.getLogger().warning("Cannot load kit room page " + (i + 1) + ": " + e.getMessage());
                }
            }
        }
        ItemFilter.get().addToWhitelist(kitroomData.subList(0, pageCount));
    }

}







