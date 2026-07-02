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

import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/**
 * A pending player-to-player share, snapshotted at request time so it stays
 * valid even if the sender edits their kits or logs off before the target
 * responds.
 */
public final class ShareRequest {

    public enum Type {
        KIT,
        ENDERCHEST,
        TRANSFER
    }

    private final String id;
    private final Type type;
    private final UUID senderId;
    private final String senderName;
    private final UUID targetId;
    private final ItemStack[] contents;
    private final Map<Integer, ItemStack[]> kits;
    private final Map<Integer, ItemStack[]> enderchests;
    private BukkitTask expiryTask;

    private ShareRequest(String id, Type type, UUID senderId, String senderName, UUID targetId,
                         ItemStack[] contents, Map<Integer, ItemStack[]> kits, Map<Integer, ItemStack[]> enderchests) {
        this.id = id;
        this.type = type;
        this.senderId = senderId;
        this.senderName = senderName;
        this.targetId = targetId;
        this.contents = contents;
        this.kits = kits;
        this.enderchests = enderchests;
    }

    public static ShareRequest kit(String id, UUID senderId, String senderName, UUID targetId, ItemStack[] contents) {
        return new ShareRequest(id, Type.KIT, senderId, senderName, targetId, contents, null, null);
    }

    public static ShareRequest enderchest(String id, UUID senderId, String senderName, UUID targetId, ItemStack[] contents) {
        return new ShareRequest(id, Type.ENDERCHEST, senderId, senderName, targetId, contents, null, null);
    }

    public static ShareRequest transfer(String id, UUID senderId, String senderName, UUID targetId,
                                        Map<Integer, ItemStack[]> kits, Map<Integer, ItemStack[]> enderchests) {
        return new ShareRequest(id, Type.TRANSFER, senderId, senderName, targetId, null, kits, enderchests);
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public ItemStack[] getContents() {
        return contents;
    }

    public Map<Integer, ItemStack[]> getKits() {
        return kits;
    }

    public Map<Integer, ItemStack[]> getEnderchests() {
        return enderchests;
    }

    public void setExpiryTask(@Nullable BukkitTask expiryTask) {
        this.expiryTask = expiryTask;
    }

    public void cancelExpiryTask() {
        if (expiryTask != null) {
            expiryTask.cancel();
            expiryTask = null;
        }
    }
}
