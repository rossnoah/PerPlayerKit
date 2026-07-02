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
package dev.noah.perplayerkit.gui;

import com.google.common.primitives.Ints;
import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.util.*;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.Slot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.noah.perplayerkit.gui.ItemUtil.addHideFlags;
import static dev.noah.perplayerkit.gui.ItemUtil.createItem;
import static dev.noah.perplayerkit.gui.ItemUtil.createGlassPane;
import static dev.noah.perplayerkit.gui.GuiLayoutUtils.*;
import static dev.noah.perplayerkit.util.PlayerUtil.getPlayerName;

public class GUI {
    private final Plugin plugin;
    private final boolean filterItemsOnImport;
    private static final Set<UUID> kitDeletionFlag = new HashSet<>();
    private static final Map<UUID, EditorContext> editorContexts = new HashMap<>();
    // Last main-menu page each player viewed, so back buttons from submenus
    // (kit room, public kits) return to it instead of resetting to page 1.
    private static final Map<UUID, Integer> lastMainMenuPage = new HashMap<>();

    public enum EditorType {
        KIT,
        PUBLIC_KIT,
        ENDERCHEST,
        INSPECT_KIT,
        INSPECT_ENDERCHEST
    }

    public record EditorContext(EditorType type, int slot, String id, UUID target, String playerName) {
    }

    public static EditorContext getAndRemoveEditorContext(UUID viewer) {
        return editorContexts.remove(viewer);
    }

    public static void forgetMainMenuPage(UUID player) {
        lastMainMenuPage.remove(player);
    }

    private static void setEditorContext(Player viewer, EditorContext context) {
        editorContexts.put(viewer.getUniqueId(), context);
    }

    public GUI(Plugin plugin) {
        this.plugin = plugin;
        this.filterItemsOnImport = plugin.getConfig().getBoolean("anti-exploit.import-filter", false);
    }

    public static void addLoadPublicKit(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            KitManager.get().loadPublicKit(player, id);
            info.getClickedMenu().close();
        });
    }

    public static boolean removeKitDeletionFlag(Player player) {
        return kitDeletionFlag.remove(player.getUniqueId());
    }

    private static String lang(String key) {
        return Lang.get().raw(key);
    }

    private static String lang(String key, String... pairs) {
        return Lang.get().raw(key, pairs);
    }

    public void OpenKitMenu(Player p, int slot) {
        Menu menu = GuiMenuFactory.createKitMenu(slot);

        if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slot);
            for (int i = 0; i < KIT_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        allowModificationRange(menu, 0, KIT_CONTENT_END);
        setGlassPaneRange(menu, KIT_CONTENT_END, MENU_SIZE);
        setArmorAndOffhandIndicators(menu);

        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.CHEST, 1, lang("gui.import-button"), lang("gui.lore-import-inventory")));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, lang("gui.clear-kit-button"), lang("gui.lore-shift-clear")));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
        addMainButton(menu.getSlot(BACK_SLOT), KitSlots.pageOf(slot));
        addClear(menu.getSlot(CLEAR_SLOT));
        addImport(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);

        menu.open(p);
        setEditorContext(p, new EditorContext(EditorType.KIT, slot, null, null, null));
    }

    public void OpenPublicKitEditor(Player p, String kitId) {
        Menu menu = GuiMenuFactory.createPublicKitMenu(kitId);

        if (KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId)) != null) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(kitId));
            for (int i = 0; i < KIT_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        allowModificationRange(menu, 0, KIT_CONTENT_END);
        setGlassPaneRange(menu, KIT_CONTENT_END, MENU_SIZE);
        setArmorAndOffhandIndicators(menu);

        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.CHEST, 1, lang("gui.import-button"), lang("gui.lore-import-inventory")));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, lang("gui.clear-kit-button"), lang("gui.lore-shift-clear")));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
        addMainButton(menu.getSlot(BACK_SLOT));
        addClear(menu.getSlot(CLEAR_SLOT));
        addImport(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);

        menu.open(p);
        setEditorContext(p, new EditorContext(EditorType.PUBLIC_KIT, 0, kitId, null, null));
    }

    public void OpenECKitKenu(Player p, int slot) {
        Menu menu = GuiMenuFactory.createECMenu(slot);

        setGlassPaneRange(menu, 0, EC_CONTENT_START);
        setGlassPaneRange(menu, EC_CONTENT_END, MENU_SIZE);
        if (KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + slot) != null) {

            ItemStack[] kit = KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + slot);
            for (int i = EC_CONTENT_START; i < EC_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i - EC_CONTENT_START]);
            }
        }
        allowModificationRange(menu, EC_CONTENT_START, EC_CONTENT_END);
        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.ENDER_CHEST, 1, lang("gui.import-button"), lang("gui.lore-import-ec")));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, lang("gui.clear-kit-button"), lang("gui.lore-shift-clear")));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
        addMainButton(menu.getSlot(BACK_SLOT), KitSlots.pageOf(slot));
        addClear(menu.getSlot(CLEAR_SLOT), EC_CONTENT_START, EC_CONTENT_END);
        addImportEC(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        setEditorContext(p, new EditorContext(EditorType.ENDERCHEST, slot, null, null, null));
    }

    public void InspectKit(Player p, UUID target, int slot) {
        String playerName = getPlayerName(target);
        if (playerName == null) {
            playerName = target.toString();
        }
        Menu menu = GuiMenuFactory.createInspectMenu(slot, playerName);

        if (KitManager.get().hasKit(target, slot)) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(target.toString() + slot);
            for (int i = 0; i < KIT_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        setGlassPaneRange(menu, KIT_CONTENT_END, MENU_SIZE);
        setArmorAndOffhandIndicators(menu);

        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.close-button")));
        menu.getSlot(BACK_SLOT).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });

        if (p.hasPermission("perplayerkit.admin")) {
            allowModificationRange(menu, 0, KIT_CONTENT_END);
            menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, lang("gui.clear-kit-button"), lang("gui.lore-shift-delete-kit")));
            addClearKit(menu.getSlot(CLEAR_SLOT), target, slot);
        }

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        setEditorContext(p, new EditorContext(EditorType.INSPECT_KIT, slot, null, target, playerName));
        SoundManager.playOpenGui(p);
    }

    public void InspectEc(Player p, UUID target, int slot) {
        String playerName = getPlayerName(target);
        if (playerName == null) {
            playerName = target.toString();
        }
        Menu menu = GuiMenuFactory.createInspectEcMenu(slot, playerName);

        setGlassPaneRange(menu, 0, EC_CONTENT_START);
        setGlassPaneRange(menu, EC_CONTENT_END, MENU_SIZE);
        if (KitManager.get().getItemStackArrayById(target + "ec" + slot) != null) {

            ItemStack[] kit = KitManager.get().getItemStackArrayById(target + "ec" + slot);
            for (int i = EC_CONTENT_START; i < EC_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i - EC_CONTENT_START]);
            }
        }

        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.close-button")));
        menu.getSlot(BACK_SLOT).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });

        if (p.hasPermission("perplayerkit.admin")) {
            allowModificationRange(menu, EC_CONTENT_START, EC_CONTENT_END);
            menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, lang("gui.clear-ec-button"), lang("gui.lore-shift-delete-ec")));
            addClearEnderchest(menu.getSlot(CLEAR_SLOT), target, slot);
        }

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        setEditorContext(p, new EditorContext(EditorType.INSPECT_ENDERCHEST, slot, null, target, playerName));
        SoundManager.playOpenGui(p);
    }

    public void OpenMainMenu(Player p) {
        OpenMainMenu(p, 0);
    }

    public void OpenMainMenu(Player p, int page) {
        int maxKits = KitSlots.maxKits();
        int pages = KitSlots.pageCount();
        page = Ints.constrainToRange(page, 0, pages - 1);
        int base = page * KitSlots.SLOTS_PER_PAGE;
        if (pages > 1) {
            lastMainMenuPage.put(p.getUniqueId(), page);
        }

        Menu menu = GuiMenuFactory.createMainMenu(p, page, pages);
        for (int i = 0; i < MENU_SIZE; i++) {
            menu.getSlot(i).setItem(createGlassPane());
        }
        // Three rows per kit slot: load/edit chest (row 1), enderchest (row 2),
        // status book (row 3). Columns past maxKits stay glass.
        for (int col = 0; col < KitSlots.SLOTS_PER_PAGE; col++) {
            int slotNum = base + col + 1;
            if (slotNum > maxKits) {
                break;
            }

            menu.getSlot(9 + col).setItem(createItem(Material.CHEST, 1,
                    lang("gui.kit-slot-name", "slot", String.valueOf(slotNum)),
                    lang("gui.lore-left-load"), lang("gui.lore-right-edit")));
            addEditLoad(menu.getSlot(9 + col), slotNum);

            if (KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + slotNum) != null) {
                menu.getSlot(18 + col).setItem(createItem(Material.ENDER_CHEST, 1,
                        lang("gui.enderchest-slot-name", "slot", String.valueOf(slotNum)),
                        lang("gui.lore-left-load"), lang("gui.lore-right-edit")));
                addEditLoadEC(menu.getSlot(18 + col), slotNum);
            } else {
                menu.getSlot(18 + col).setItem(createItem(Material.ENDER_EYE, 1,
                        lang("gui.enderchest-slot-name", "slot", String.valueOf(slotNum)),
                        lang("gui.lore-click-create")));
                addEditEC(menu.getSlot(18 + col), slotNum);
            }

            if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + slotNum) != null) {
                menu.getSlot(27 + col).setItem(createItem(Material.KNOWLEDGE_BOOK, 1, lang("gui.kit-exists"), lang("gui.lore-click-edit")));
            } else {
                menu.getSlot(27 + col).setItem(createItem(Material.BOOK, 1, lang("gui.kit-not-found"), lang("gui.lore-click-create")));
            }
            addEdit(menu.getSlot(27 + col), slotNum);
        }

        for (int i = 37; i < 44; i++) {
            menu.getSlot(i).setItem(createGlassPane());
        }

        menu.getSlot(37).setItem(createItem(Material.NETHER_STAR, 1, lang("gui.kit-room-button")));
        menu.getSlot(38).setItem(createItem(Material.BOOKSHELF, 1, lang("gui.premade-kits-button")));
        menu.getSlot(39).setItem(createItem(Material.OAK_SIGN, 1, lang("gui.info-button"),
                lang("gui.lore-info-load"), lang("gui.lore-info-edit"), lang("gui.lore-info-share")));
        menu.getSlot(41).setItem(createItem(Material.REDSTONE_BLOCK, 1, lang("gui.clear-inventory-button"), lang("gui.lore-shift-click")));
        menu.getSlot(42).setItem(createItem(Material.COMPASS, 1, lang("gui.share-kits-button"), lang("gui.lore-share-kits")));
        menu.getSlot(43).setItem(createItem(Material.EXPERIENCE_BOTTLE, 1, lang("gui.repair-items-button")));
        addRepairButton(menu.getSlot(43));
        addKitRoom(menu.getSlot(37));
        addPublicKitMenu(menu.getSlot(38));
        addClearButton(menu.getSlot(41));

        if (page > 0) {
            addPageArrow(menu, MAIN_PREV_PAGE_SLOT, "gui.previous-page-button", page - 1, pages);
        }
        if (page < pages - 1) {
            addPageArrow(menu, MAIN_NEXT_PAGE_SLOT, "gui.next-page-button", page + 1, pages);
        }

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void OpenKitRoom(Player p) {
        OpenKitRoom(p, 0);
    }

    public void OpenKitRoom(Player p, int page) {
        Menu menu = GuiMenuFactory.createKitRoomMenu();
        allowModificationRange(menu, 0, FOOTER_START);
        setGlassPaneRange(menu, FOOTER_START, MENU_SIZE);
        if (KitRoomDataManager.get().getKitRoomPage(page) != null) {
            for (int i = 0; i < FOOTER_START; i++) {
                menu.getSlot(i).setItem(KitRoomDataManager.get().getKitRoomPage(page)[i]);
            }
        }

        menu.getSlot(45).setItem(createItem(Material.BEACON, 1, lang("gui.refill-button")));
        addKitRoom(menu.getSlot(45), page);

        if (!p.hasPermission("perplayerkit.editkitroom")) {
            menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
            addMainButton(menu.getSlot(53));
        } else {
            menu.getSlot(53).setItem(createItem(Material.BARRIER, page + 1, lang("gui.edit-menu-button"), lang("gui.edit-menu-lore")));
        }
        addKitRoom(menu.getSlot(47), 0);
        addKitRoom(menu.getSlot(48), 1);
        addKitRoom(menu.getSlot(49), 2);
        addKitRoom(menu.getSlot(50), 3);
        addKitRoom(menu.getSlot(51), 4);

        for (int i = 1; i < 6; i++) {
            menu.getSlot(46 + i).setItem(addHideFlags(createItem(Material.valueOf(plugin.getConfig().getString("kitroom.items." + i + ".material")), "<reset>" + plugin.getConfig().getString("kitroom.items." + i + ".name"))));
        }

        menu.getSlot(page + 47).setItem(ItemUtil.addEnchantLook(menu.getSlot(page + 47).getItem(p)));

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public Menu ViewPublicKitMenu(Player p, String id) {
        ItemStack[] kit = KitManager.get().getPublicKit(id);

        if (kit == null) {
            Lang.get().send(p, "error.kit-not-found-display");
            if (p.hasPermission("perplayerkit.admin")) {
                Lang.get().send(p, "info.assign-publickit-instruction");
            }
            return null;
        }
        Menu menu = GuiMenuFactory.createViewPublicKitMenu(id);

        for (int i = 0; i < MENU_SIZE; i++) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }

        for (int i = 9; i < 36; i++) {
            menu.getSlot(i).setItem(kit[i]);
        }
        for (int i = 0; i < 9; i++) {
            menu.getSlot(i + 36).setItem(kit[i]);
        }
        for (int i = 36; i < 41; i++) {
            menu.getSlot(i + 9).setItem(kit[i]);
        }

        setArmorAndOffhandIndicators(menu);
        menu.getSlot(LOAD_PUBLIC_KIT_SLOT).setItem(createItem(Material.APPLE, 1, lang("gui.load-kit-button")));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
        addPublicKitMenu(menu.getSlot(BACK_SLOT));
        addLoadPublicKit(menu.getSlot(LOAD_PUBLIC_KIT_SLOT), id);

        menu.open(p);

        return menu;
    }

    public void OpenPublicKitMenu(Player player) {
        Menu menu = GuiMenuFactory.createPublicKitRoomMenu();
        for (int i = 0; i < MENU_SIZE; i++) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }

        for (int i = 18; i < 36; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BOOK, 1, lang("gui.more-kits-coming")));
        }

        List<PublicKit> publicKitList = KitManager.get().getPublicKitList();

        for (int i = 0; i < publicKitList.size(); i++) {
            PublicKit kit = publicKitList.get(i);
            if (KitManager.get().hasPublicKit(kit.id)) {
                if (player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i + 18).setItem(createItem(kit.icon, 1, ChatColor.RESET + kit.name, lang("gui.lore-admin-shift-edit")));
                } else {
                    menu.getSlot(i + 18).setItem(createItem(kit.icon, 1, ChatColor.RESET + kit.name));
                }
                addPublicKitButton(menu.getSlot(i + 18), kit.id);
            } else {
                String unassignedName = ChatColor.RESET + kit.name + " " + lang("gui.unassigned-tag");
                if (player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i + 18).setItem(createItem(kit.icon, 1, unassignedName,
                            lang("gui.lore-unassigned-info"), lang("gui.lore-admin-shift-edit")));
                } else {
                    menu.getSlot(i + 18).setItem(createItem(kit.icon, 1, unassignedName, lang("gui.lore-unassigned-info")));
                }
            }

            if (player.hasPermission("perplayerkit.admin")) {
                addAdminPublicKitButton(menu.getSlot(i + 18), kit.id);
            }
        }

        addMainButton(menu.getSlot(BACK_SLOT));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, lang("gui.back-button")));
        menu.open(player);
    }

    public void addClear(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = 0; i < 41; i++) {
                    m.getSlot(i).setItem((org.bukkit.inventory.ItemStack) null);
                }
            }
        });
    }

    public void addClear(Slot slot, int start, int end) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                Menu m = info.getClickedMenu();
                for (int i = start; i < end; i++) {
                    m.getSlot(i).setItem((org.bukkit.inventory.ItemStack) null);
                }
            }
        });
    }

    public void addClearKit(Slot slot, UUID target, int slotNum) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                KitManager.get().deleteKit(target, slotNum);
                Lang.get().send(player, "success.admin-kit-deleted", "slot", String.valueOf(slotNum));
                SoundManager.playSuccess(player);
                kitDeletionFlag.add(player.getUniqueId());
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addClearEnderchest(Slot slot, UUID target, int slotNum) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                KitManager.get().deleteEnderchest(target, slotNum);
                Lang.get().send(player, "success.admin-ec-deleted", "slot", String.valueOf(slotNum));
                SoundManager.playSuccess(player);
                kitDeletionFlag.add(player.getUniqueId());
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT) {
                Menu m = ViewPublicKitMenu(player, id);
                if (m != null) {
                    m.open(player);
                }
            }
        });
    }

    public void addAdminPublicKitButton(Slot slot, String id) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                OpenPublicKitEditor(player, id);
                return;
            }
            if (info.getClickType() == ClickType.LEFT) {
                KitManager.get().loadPublicKit(player, id);
            } else if (info.getClickType() == ClickType.RIGHT) {
                Menu m = ViewPublicKitMenu(player, id);
                if (m != null) {
                    m.open(player);
                }
            }
        });
    }

    public void addMainButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            OpenMainMenu(player, lastMainMenuPage.getOrDefault(player.getUniqueId(), 0));
        });
    }

    public void addMainButton(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            OpenMainMenu(player, page);
        });
    }

    private void addPageArrow(Menu menu, int guiSlot, String nameKey, int targetPage, int pages) {
        menu.getSlot(guiSlot).setItem(createItem(Material.ARROW, 1, lang(nameKey),
                lang("gui.lore-page-indicator", "page", String.valueOf(targetPage + 1), "pages", String.valueOf(pages))));
        addMainButton(menu.getSlot(guiSlot), targetPage);
    }

    public void addKitRoom(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            OpenKitRoom(player);
            BroadcastManager.get().broadcastPlayerOpenedKitRoom(player);
        });
    }

    public void addKitRoom(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            OpenKitRoom(player, page);
        });
    }

    public void addPublicKitMenu(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            OpenPublicKitMenu(player);
        });
    }

    public void addKitRoomSaveButton(Slot slot, int page) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isRightClick() && info.getClickType().isShiftClick()) {
                ItemStack[] data = new ItemStack[45];
                for (int i = 0; i < 41; i++) {
                    data[i] = player.getInventory().getContents()[i];
                }
                KitRoomDataManager.get().setKitRoom(page, data);
                Lang.get().send(player, "success.kitroom-menu-saved");
                SoundManager.playSuccess(player);
            }
        });
    }

    public void addRepairButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            BroadcastManager.get().broadcastPlayerRepaired(player);
            PlayerUtil.repairAll(player);
            player.updateInventory();
            SoundManager.playSuccess(player);
        });
    }

    public void addClearButton(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isShiftClick()) {
                player.getInventory().clear();
                Lang.get().send(player, "success.inventory-cleared");
                SoundManager.playSuccess(player);
            }
        });
    }

    public void addImport(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            Menu m = info.getClickedMenu();
            ItemStack[] inv;
            if (filterItemsOnImport) {
                inv = ItemFilter.get().filterItemStack(player.getInventory().getContents());
            } else {
                inv = player.getInventory().getContents();
            }
            for (int i = 0; i < 41; i++) {
                m.getSlot(i).setItem(inv[i]);
            }
        });
    }

    public void addImportEC(Slot slot) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            Menu m = info.getClickedMenu();
            ItemStack[] inv;
            if (filterItemsOnImport) {
                inv = ItemFilter.get().filterItemStack(player.getEnderChest().getContents());
            } else {
                inv = player.getEnderChest().getContents();
            }
            for (int i = 0; i < 27; i++) {
                m.getSlot(i + 9).setItem(inv[i]);
            }
        });
    }

    public void addEdit(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                OpenKitMenu(player, i);
            }
        });
    }

    public void addEditEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType().isLeftClick() || info.getClickType().isRightClick()) {
                OpenECKitKenu(player, i);
            }
        });
    }

    public void addLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();
                SoundManager.playCloseGui(player);
            }
        });
    }

    public void addEditLoad(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadKit(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                OpenKitMenu(player, i);
            }
        });
    }

    public void addEditLoadEC(Slot slot, int i) {
        slot.setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            if (info.getClickType() == ClickType.LEFT || info.getClickType() == ClickType.SHIFT_LEFT) {
                KitManager.get().loadEnderchest(player, i);
                info.getClickedMenu().close();
            } else if (info.getClickType() == ClickType.RIGHT || info.getClickType() == ClickType.SHIFT_RIGHT) {
                OpenECKitKenu(player, i);
            }
        });
    }
}
