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
    private static final Map<UUID, UUID> inspectTargets = new HashMap<>();

    public static void setInspectTarget(UUID inspector, UUID target) {
        inspectTargets.put(inspector, target);
    }

    public static UUID getAndRemoveInspectTarget(UUID inspector) {
        return inspectTargets.remove(inspector);
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

        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>● Import from inventory</gray>"));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>● Shift click to clear</gray>"));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        addMainButton(menu.getSlot(BACK_SLOT));
        addClear(menu.getSlot(CLEAR_SLOT));
        addImport(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);

        menu.open(p);
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

        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>● Import from inventory</gray>"));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>● Shift click to clear</gray>"));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        addMainButton(menu.getSlot(BACK_SLOT));
        addClear(menu.getSlot(CLEAR_SLOT));
        addImport(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);

        menu.open(p);
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
        menu.getSlot(IMPORT_SLOT).setItem(createItem(Material.ENDER_CHEST, 1, "<green><b>IMPORT</b></green>", "<gray>● Import from enderchest</gray>"));
        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>● Shift click to clear</gray>"));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        addMainButton(menu.getSlot(BACK_SLOT));
        addClear(menu.getSlot(CLEAR_SLOT), EC_CONTENT_START, EC_CONTENT_END);
        addImportEC(menu.getSlot(IMPORT_SLOT));
        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
    }

    public void InspectKit(Player p, UUID target, int slot) {
        setInspectTarget(p.getUniqueId(), target);
        String playerName = getPlayerName(target);
        Menu menu = GuiMenuFactory.createInspectMenu(slot, playerName);

        if (KitManager.get().hasKit(target, slot)) {
            ItemStack[] kit = KitManager.get().getItemStackArrayById(target.toString() + slot);
            for (int i = 0; i < KIT_CONTENT_END; i++) {
                menu.getSlot(i).setItem(kit[i]);
            }
        }
        setGlassPaneRange(menu, KIT_CONTENT_END, MENU_SIZE);
        setArmorAndOffhandIndicators(menu);

        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>CLOSE</b></red>"));
        menu.getSlot(BACK_SLOT).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });

        if (p.hasPermission("perplayerkit.admin")) {
            allowModificationRange(menu, 0, KIT_CONTENT_END);
            menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, "<red><b>CLEAR KIT</b></red>", "<gray>● Shift click to delete kit</gray>"));
            addClearKit(menu.getSlot(CLEAR_SLOT), target, slot);
        }

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        SoundManager.playOpenGui(p);
    }

    public void InspectEc(Player p, UUID target, int slot) {
        setInspectTarget(p.getUniqueId(), target);
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

        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>CLOSE</b></red>"));
        menu.getSlot(BACK_SLOT).setClickHandler((player, info) -> {
            SoundManager.playClick(player);
            info.getClickedMenu().close();
            SoundManager.playCloseGui(player);
        });

        if (p.hasPermission("perplayerkit.admin")) {
            allowModificationRange(menu, EC_CONTENT_START, EC_CONTENT_END);
            menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.BARRIER, 1, "<red><b>CLEAR ENDERCHEST</b></red>", "<gray>● Shift click to delete enderchest</gray>"));
            addClearEnderchest(menu.getSlot(CLEAR_SLOT), target, slot);
        }

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);
        menu.open(p);
        SoundManager.playOpenGui(p);
    }

    public void OpenMainMenu(Player p) {
        Menu menu = GuiMenuFactory.createMainMenu(p);
        for (int i = 0; i < MENU_SIZE; i++) {
            menu.getSlot(i).setItem(createGlassPane());
        }
        for (int i = 9; i < 18; i++) {
            menu.getSlot(i).setItem(createItem(Material.CHEST, 1, "<dark_aqua><b>Kit " + (i - 8) + "</b></dark_aqua>", "<gray>● Left click to load kit</gray>", "<gray>● Right click to edit kit</gray>"));
            addEditLoad(menu.getSlot(i), i - 8);
        }
        for (int i = 18; i < 27; i++) {
            if (KitManager.get().getItemStackArrayById(p.getUniqueId() + "ec" + (i - 17)) != null) {
                menu.getSlot(i).setItem(createItem(Material.ENDER_CHEST, 1, "<dark_aqua><b>Enderchest " + (i - 17) + "</b></dark_aqua>", "<gray>● Left click to load kit</gray>", "<gray>● Right click to edit kit</gray>"));
                addEditLoadEC(menu.getSlot(i), i - 17);
            } else {
                menu.getSlot(i).setItem(createItem(Material.ENDER_EYE, 1, "<dark_aqua><b>Enderchest " + (i - 17) + "</b></dark_aqua>", "<gray>● Click to create</gray>"));
                addEditEC(menu.getSlot(i), i - 17);
            }
        }
        for (int i = 27; i < 36; i++) {
            if (KitManager.get().getItemStackArrayById(p.getUniqueId().toString() + (i - 26)) != null) {
                menu.getSlot(i).setItem(createItem(Material.KNOWLEDGE_BOOK, 1, "<green><b>KIT EXISTS</b></green>", "<gray>● Click to edit</gray>"));
            } else {
                menu.getSlot(i).setItem(createItem(Material.BOOK, 1, "<red><b>KIT NOT FOUND</b></red>", "<gray>● Click to create</gray>"));
            }
            addEdit(menu.getSlot(i), i - 26);
        }

        for (int i = 37; i < 44; i++) {
            menu.getSlot(i).setItem(createGlassPane());
        }

        menu.getSlot(37).setItem(createItem(Material.NETHER_STAR, 1, "<green><b>KIT ROOM</b></green>"));
        menu.getSlot(38).setItem(createItem(Material.BOOKSHELF, 1, "<yellow><b>PREMADE KITS</b></yellow>"));
        menu.getSlot(39).setItem(createItem(Material.OAK_SIGN, 1, "<green><b>INFO</b></green>", "<gray>● Click a kit slot to load your kit</gray>", "<gray>● Right click or click the book to edit</gray>", "<gray>● Share kits with /sharekit <slot></gray>"));
        menu.getSlot(41).setItem(createItem(Material.REDSTONE_BLOCK, 1, "<red><b>CLEAR INVENTORY</b></red>", "<gray>● Shift click</gray>"));
        menu.getSlot(42).setItem(createItem(Material.COMPASS, 1, "<green><b>SHARE KITS</b></green>", "<gray>● /sharekit <slot></gray>"));
        menu.getSlot(43).setItem(createItem(Material.EXPERIENCE_BOTTLE, 1, "<green><b>REPAIR ITEMS</b></green>"));
        addRepairButton(menu.getSlot(43));
        addKitRoom(menu.getSlot(37));
        addPublicKitMenu(menu.getSlot(38));
        addClearButton(menu.getSlot(41));

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

        menu.getSlot(45).setItem(createItem(Material.BEACON, 1, "<dark_aqua><b>REFILL</b></dark_aqua>"));
        addKitRoom(menu.getSlot(45), page);

        if (!p.hasPermission("perplayerkit.editkitroom")) {
            menu.getSlot(53).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
            addMainButton(menu.getSlot(53));
        } else {
            menu.getSlot(53).setItem(createItem(Material.BARRIER, page + 1, "<red><b>EDIT MENU</b></red>", "<red>SHIFT RIGHT CLICK TO SAVE</red>"));
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
            p.sendMessage(ChatColor.RED + "Kit not found");
            if (p.hasPermission("perplayerkit.admin")) {
                p.sendMessage(ChatColor.RED + "To assign a kit to this publickit use /savepublickit <id>");
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

        menu.getSlot(CLEAR_SLOT).setItem(createItem(Material.APPLE, 1, "<green><b>LOAD KIT</b></green>"));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
        addPublicKitMenu(menu.getSlot(BACK_SLOT));
        addLoadPublicKit(menu.getSlot(CLEAR_SLOT), id);

        menu.open(p);

        return menu;
    }

    public void OpenPublicKitMenu(Player player) {
        Menu menu = GuiMenuFactory.createPublicKitRoomMenu();
        for (int i = 0; i < MENU_SIZE; i++) {
            menu.getSlot(i).setItem(ItemUtil.createGlassPane());
        }

        for (int i = 18; i < 36; i++) {
            menu.getSlot(i).setItem(ItemUtil.createItem(Material.BOOK, 1, "<gray><b>MORE KITS COMING SOON</b></gray>"));
        }

        List<PublicKit> publicKitList = KitManager.get().getPublicKitList();

        for (int i = 0; i < publicKitList.size(); i++) {
            if (KitManager.get().hasPublicKit(publicKitList.get(i).id)) {
                if (player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET + publicKitList.get(i).name, "<gray>● [ADMIN] Shift click to edit</gray>"));
                } else {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET + publicKitList.get(i).name));
                }
                addPublicKitButton(menu.getSlot(i + 18), publicKitList.get(i).id);
            } else {
                if (player.hasPermission("perplayerkit.admin")) {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET + publicKitList.get(i).name + " <red><b>[UNASSIGNED]</b></red>", "<gray>● Admins have not yet setup this kit yet</gray>", "<gray>● [ADMIN] Shift click to edit</gray>"));
                } else {
                    menu.getSlot(i + 18).setItem(createItem(publicKitList.get(i).icon, 1, ChatColor.RESET + publicKitList.get(i).name + " <red><b>[UNASSIGNED]</b></red>", "<gray>● Admins have not yet setup this kit yet</gray>"));
                }
            }

            if (player.hasPermission("perplayerkit.admin")) {
                addAdminPublicKitButton(menu.getSlot(i + 18), publicKitList.get(i).id);
            }
        }

        addMainButton(menu.getSlot(BACK_SLOT));
        menu.getSlot(BACK_SLOT).setItem(createItem(Material.OAK_DOOR, 1, "<red><b>BACK</b></red>"));
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
                player.sendMessage(ChatColor.GREEN + "Kit " + slotNum + " deleted for player!");
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
                player.sendMessage(ChatColor.GREEN + "Enderchest " + slotNum + " deleted for player!");
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
            OpenMainMenu(player);
        });
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
                player.sendMessage("saved menu");
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
                player.sendMessage(ChatColor.GREEN + "Inventory cleared");
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
