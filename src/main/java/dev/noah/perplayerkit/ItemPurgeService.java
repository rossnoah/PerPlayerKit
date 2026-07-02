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

import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.util.KitSlots;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deletes a specific item type from stored kits and ender chests.
 * <p>
 * Kits are stored as serialized blobs in every backend (SQLite, MySQL,
 * PostgreSQL, Redis, YAML), so the purge works the same way everywhere:
 * enumerate the entry IDs, deserialize each entry, strip the item (including
 * inside shulker boxes, other container items, and bundles via
 * {@link ItemPurger}), and write the result back. Entries that end up empty
 * are deleted, and cached copies for online players are refreshed.
 */
public class ItemPurgeService {

    private static final Pattern UUID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    private static final int UUID_LENGTH = 36;

    private final StorageManager storage;
    private final KitManager kitManager;

    public ItemPurgeService(StorageManager storage, KitManager kitManager) {
        this.storage = storage;
        this.kitManager = kitManager;
    }

    /**
     * Purges the material from every player kit and ender chest entry in the
     * database, regardless of whether the owning players are online.
     */
    public PurgeResult purgeAllPlayers(Material target, Consumer<String> progress) {
        List<String> ids = storage.getAllKitIDs().stream()
                .filter(ItemPurgeService::isPlayerDataId)
                .sorted()
                .toList();
        return purgeEntries(ids, target, progress);
    }

    /**
     * Purges the material from all kit and ender chest slots of the given players.
     */
    public PurgeResult purgePlayers(Material target, Collection<UUID> players, Consumer<String> progress) {
        Set<String> uuids = players.stream().map(UUID::toString).collect(Collectors.toSet());
        List<String> ids = storage.getAllKitIDs().stream()
                .filter(ItemPurgeService::isPlayerDataId)
                .filter(id -> uuids.contains(id.substring(0, UUID_LENGTH)))
                .sorted()
                .toList();
        return purgeEntries(ids, target, progress);
    }

    private PurgeResult purgeEntries(List<String> ids, Material target, Consumer<String> progress) {
        int scanned = 0;
        int modified = 0;
        int deleted = 0;
        int itemsRemoved = 0;
        int failed = 0;

        int processed = 0;
        for (String id : ids) {
            processed++;
            try {
                String data = storage.getKitDataByID(id);
                if (data == null || data.equalsIgnoreCase("error")) {
                    continue;
                }
                scanned++;

                ItemStack[] contents = Serializer.itemStackArrayFromBase64(data);
                int removed = ItemPurger.purgeContents(contents, target);
                if (removed == 0) {
                    continue;
                }

                itemsRemoved += removed;
                modified++;
                if (ItemPurger.isEmpty(contents)) {
                    // The plugin never stores fully empty kits, so drop the entry
                    // instead of keeping a blob that would load as an empty inventory.
                    storage.deleteKitByID(id);
                    kitManager.updateCachedKit(id, null);
                    deleted++;
                } else {
                    storage.saveKitDataByID(id, Serializer.itemStackArrayToBase64(contents));
                    kitManager.updateCachedKit(id, contents);
                }
            } catch (Exception e) {
                failed++;
                if (progress != null) {
                    progress.accept("Failed to process entry " + id + ": " + e.getMessage());
                }
            }

            if (progress != null && processed % 100 == 0) {
                progress.accept("Progress: " + processed + "/" + ids.size() + " entries processed...");
            }
        }

        return new PurgeResult(scanned, modified, deleted, itemsRemoved, failed);
    }

    /**
     * Matches per-player entries only: kit IDs ({@code <uuid><slot>}) and ender
     * chest IDs ({@code <uuid>ec<slot>}). Public kits and the kit room are
     * intentionally excluded — those are admin-managed via /savepublickit and
     * /kitroom.
     */
    static boolean isPlayerDataId(String id) {
        if (id == null || id.length() < UUID_LENGTH + 1) {
            return false;
        }
        if (!UUID_PATTERN.matcher(id.substring(0, UUID_LENGTH)).matches()) {
            return false;
        }

        String suffix = id.substring(UUID_LENGTH);
        if (suffix.startsWith("ec")) {
            suffix = suffix.substring(2);
        }
        // Bounded by the absolute KitSlots.MAX_LIMIT, deliberately not the
        // configured max-kits: the database may hold kits above a lowered
        // limit and purging must still reach them.
        return KitSlots.parseSlotSuffix(suffix) != null;
    }

    /**
     * Outcome of a purge run. {@code modified} counts every entry that was
     * changed, including the {@code deleted} entries that became empty and were
     * removed entirely. {@code itemsRemoved} counts individual items (stack
     * amounts included).
     */
    public record PurgeResult(int scanned, int modified, int deleted, int itemsRemoved, int failed) {
    }
}
