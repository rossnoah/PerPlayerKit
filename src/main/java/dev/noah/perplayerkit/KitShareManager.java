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

import dev.noah.perplayerkit.util.BroadcastManager;
import org.apache.commons.lang3.RandomStringUtils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class KitShareManager {


    public static HashMap<String, ItemStack[]> kitShareMap;
    private static KitShareManager instance;
    private final Plugin plugin;

    public KitShareManager(Plugin plugin) {
        this.plugin = plugin;
        kitShareMap = new HashMap<>();
        instance = this;
    }

    public static KitShareManager get() {
        if (instance == null) {
            throw new IllegalStateException("KitShareManager has not been initialized");
        }
        return instance;
    }

    public List<String> getKitSlots(Player p) {
        ArrayList<String> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (KitManager.get().hasKit(p.getUniqueId(), i)) {
                slots.add(String.valueOf(i));
            }
        }
        return slots;
    }

    public List<String> getECSlots(Player p) {
        ArrayList<String> slots = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if (KitManager.get().hasEC(p.getUniqueId(), i)) {
                slots.add(String.valueOf(i));
            }
        }
        return slots;
    }

    public void shareKit(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasKit(uuid, slot)) {
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if (kitShareMap.putIfAbsent(id, kitManager.getPlayerKit(uuid, slot).clone()) == null) {
                p.sendMessage(ChatColor.GREEN + "Use /copykit " + id + " to copy this kit");
                p.sendMessage(ChatColor.GREEN + "Code expires in 15 minutes");
                SoundManager.playSuccess(p);


                new BukkitRunnable() {

                    @Override
                    public void run() {
                        kitShareMap.remove(id);
                    }

                }.runTaskLater(plugin, 15 * 60 * 20);


            } else {
                p.sendMessage(ChatColor.RED + "Unexpected error occurred, please try again.");
                SoundManager.playFailure(p);
            }

        } else {
            p.sendMessage(ChatColor.RED + "Error, that kit does not exist");
            SoundManager.playFailure(p);
        }

    }


    public void shareEC(Player p, int slot) {
        UUID uuid = p.getUniqueId();
        KitManager kitManager = KitManager.get();
        if (kitManager.hasEC(uuid, slot)) {
            String id = RandomStringUtils.randomAlphanumeric(6).toUpperCase();

            if (kitShareMap.putIfAbsent(id, kitManager.getPlayerEC(uuid, slot).clone()) == null) {
                p.sendMessage(ChatColor.GREEN + "Use /copyEC " + id + " to copy this enderchest");
                p.sendMessage(ChatColor.GREEN + "Code expires in 15 minutes");
                SoundManager.playSuccess(p);


                new BukkitRunnable() {

                    @Override
                    public void run() {
                        kitShareMap.remove(id);
                    }

                }.runTaskLater(plugin, 15 * 60 * 20);


            } else {
                p.sendMessage(ChatColor.RED + "Unexpected error occurred, please try again.");
                SoundManager.playFailure(p);
            }

        } else {
            p.sendMessage(ChatColor.RED + "Error, that EC does not exist");
            SoundManager.playFailure(p);
        }

    }


    public void copyKit(Player p, String str) {

        String id = str.toUpperCase();
        if (!kitShareMap.containsKey(id)) {
            p.sendMessage(ChatColor.RED + "Error, kit does not exist or has expired");
            SoundManager.playFailure(p);
            return;
        }

            ItemStack[] data = kitShareMap.get(id);

            if (data.length == 27) {
            // enderchest
                p.getEnderChest().setContents(kitShareMap.get(id));
                BroadcastManager.get().broadcastPlayerCopiedEC(p);
                SoundManager.playSuccess(p);

            } else if (data.length == 41) {
                // inventory

                p.getInventory().setContents(kitShareMap.get(id));
                BroadcastManager.get().broadcastPlayerCopiedKit(p);
                SoundManager.playSuccess(p);
            } else {
                p.sendMessage(ChatColor.RED + "Unexpected error occurred, please try again.");
                SoundManager.playFailure(p);
            }


    }


}
