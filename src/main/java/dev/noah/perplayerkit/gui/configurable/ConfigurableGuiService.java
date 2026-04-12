package dev.noah.perplayerkit.gui.configurable;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.gui.ItemUtil;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.IDUtil;
import dev.noah.perplayerkit.util.PlayerUtil;
import dev.noah.perplayerkit.util.SoundManager;
import dev.noah.perplayerkit.util.StyleManager;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.ipvp.canvas.Menu;
import org.ipvp.canvas.slot.ClickOptions;
import org.ipvp.canvas.slot.Slot;
import org.ipvp.canvas.type.ChestMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigurableGuiService {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%([a-zA-Z0-9_]+)%");

    private static ConfigurableGuiService instance;

    private final Plugin plugin;
    private final GuiConfigManager guiConfigManager;

    public ConfigurableGuiService(Plugin plugin) {
        this.plugin = plugin;
        this.guiConfigManager = new GuiConfigManager(plugin);
        instance = this;
    }

    public static ConfigurableGuiService get() {
        if (instance == null) {
            throw new IllegalStateException("ConfigurableGuiService has not been initialized");
        }
        return instance;
    }

    public void openMainMenu(Player player) {
        openConfiguredGui("main-menu", player, GuiContext.empty());
    }

    public void openPlayerKitEditor(Player player, int slot) {
        openConfiguredGui("player-kit-editor", player, GuiContext.empty()
                .with("slot", slot)
                .with("slot_display", slot));
    }

    public void openPublicKitEditor(Player player, String publicKitId) {
        openConfiguredGui("public-kit-editor", player, enrichPublicKitContext(GuiContext.empty(), publicKitId));
    }

    public void openEnderchestEditor(Player player, int slot) {
        openConfiguredGui("enderchest-editor", player, GuiContext.empty()
                .with("slot", slot)
                .with("slot_display", slot));
    }

    public void openInspectKit(Player player, UUID targetUuid, int slot) {
        openConfiguredGui("inspect-kit", player, GuiContext.empty()
                .with("slot", slot)
                .with("slot_display", slot)
                .with("target_uuid", targetUuid)
                .with("target_name", getPlayerName(targetUuid)));
    }

    public void openInspectEnderchest(Player player, UUID targetUuid, int slot) {
        openConfiguredGui("inspect-enderchest", player, GuiContext.empty()
                .with("slot", slot)
                .with("slot_display", slot)
                .with("target_uuid", targetUuid)
                .with("target_name", getPlayerName(targetUuid)));
    }

    public void openPublicKitMenu(Player player) {
        openConfiguredGui("public-kit-menu", player, GuiContext.empty());
    }

    public void openKitRoom(Player player) {
        openKitRoom(player, 0);
    }

    public void openKitRoom(Player player, int page) {
        openConfiguredGui("kit-room", player, GuiContext.empty()
                .with("page", page)
                .with("page_display", page + 1));
    }

    public Menu createPublicKitViewer(Player player, String publicKitId) {
        if (KitManager.get().getPublicKit(publicKitId) == null) {
            player.sendMessage(ChatColor.RED + "Kit not found");
            if (player.hasPermission("perplayerkit.admin")) {
                player.sendMessage(ChatColor.RED + "To assign a kit to this publickit use /savepublickit <id>");
            }
            return null;
        }
        return createMenu("public-kit-viewer", player, enrichPublicKitContext(GuiContext.empty(), publicKitId));
    }

    public void openViewOnlyEnderchest(Player player) {
        openConfiguredGui("view-only-enderchest", player, GuiContext.empty());
    }

    public Menu createMenu(String guiId, Player viewer, GuiContext context) {
        ConfigurationSection guiSection = guiConfigManager.getGuiSection(guiId);
        if (guiSection == null) {
            plugin.getLogger().warning("Missing GUI definition: " + guiId);
            return null;
        }

        int rows = Math.max(1, Math.min(6, guiSection.getInt("rows", 6)));
        String title = resolveText(guiSection.getString("title", "Menu"), viewer, context);

        Menu menu = ChestMenu.builder(rows)
                .title(title)
                .redraw(guiSection.getBoolean("redraw", false))
                .build();

        menu.setCursorDropHandler(Menu.ALLOW_CURSOR_DROPPING);

        AtomicBoolean skipCloseSave = new AtomicBoolean(false);
        applyCloseHandler(guiId, menu, context, skipCloseSave);

        for (ConfigurationSection elementSection : getOrderedElementSections(guiSection)) {
            renderElement(menu, viewer, guiId, context, elementSection, skipCloseSave);
        }

        return menu;
    }

    private Menu openConfiguredGui(String guiId, Player viewer, GuiContext context) {
        Menu menu = createMenu(guiId, viewer, context);
        if (menu == null) {
            return null;
        }

        menu.open(viewer);
        SoundManager.playOpenGui(viewer);
        return menu;
    }

    private void renderElement(Menu menu, Player viewer, String guiId, GuiContext context, ConfigurationSection elementSection, AtomicBoolean skipCloseSave) {
        if (!isVisibleToViewer(elementSection, viewer)) {
            return;
        }

        String type = elementSection.getString("type", "static").toLowerCase(Locale.ROOT);
        switch (type) {
            case "fill":
            case "static":
            case "button":
                renderStaticElement(menu, viewer, context, elementSection, skipCloseSave);
                return;
            case "component":
                renderComponent(menu, viewer, guiId, context, elementSection, skipCloseSave);
                return;
            default:
                plugin.getLogger().warning("Unknown GUI element type '" + type + "'");
        }
    }

    private void renderStaticElement(Menu menu, Player viewer, GuiContext context, ConfigurationSection elementSection, AtomicBoolean skipCloseSave) {
        List<Integer> slots = parseSlots(elementSection.get("slots"));
        ConfigurationSection itemSection = elementSection.getConfigurationSection("item");
        ConfigurationSection actionsSection = elementSection.getConfigurationSection("actions");
        boolean editable = elementSection.getBoolean("editable", false);

        for (int slotIndex : slots) {
            Slot slot = menu.getSlot(slotIndex);
            ItemStack item = buildItem(itemSection, viewer, context);
            if (item != null) {
                slot.setItem(item);
            }
            if (editable) {
                allowModification(slot);
            }
            bindActions(slot, actionsSection, context, skipCloseSave);
        }
    }

    private void renderComponent(Menu menu, Player viewer, String guiId, GuiContext context, ConfigurationSection elementSection, AtomicBoolean skipCloseSave) {
        String component = elementSection.getString("component", "").toLowerCase(Locale.ROOT);
        List<Integer> slots = parseSlots(elementSection.get("slots"));

        switch (component) {
            case "player-kit-selector":
                renderIndexedVariantComponent(menu, viewer, slots, elementSection, context, skipCloseSave, "default", false, false);
                return;
            case "enderchest-selector":
                renderIndexedVariantComponent(menu, viewer, slots, elementSection, context, skipCloseSave, null, false, true);
                return;
            case "kit-status-selector":
                renderIndexedVariantComponent(menu, viewer, slots, elementSection, context, skipCloseSave, null, true, false);
                return;
            case "public-kit-list":
                renderPublicKitList(menu, viewer, elementSection, context, slots, skipCloseSave);
                return;
            case "kit-data":
                renderItemData(menu, viewer, slots, resolveKitData(viewer, context), elementSection.getBoolean("editable", false));
                return;
            case "enderchest-data":
                renderItemData(menu, viewer, slots, resolveEnderchestData(viewer, context), elementSection.getBoolean("editable", false));
                return;
            case "player-enderchest-data":
                renderItemData(menu, viewer, slots, viewer.getEnderChest().getContents(), elementSection.getBoolean("editable", false));
                return;
            case "kit-room-data":
                renderItemData(menu, viewer, slots, resolveKitRoomData(context), elementSection.getBoolean("editable", false));
                return;
            case "kit-room-category-buttons":
                renderKitRoomCategoryButtons(menu, viewer, slots, elementSection, context, skipCloseSave);
                return;
            case "kit-room-control":
                renderKitRoomControl(menu, viewer, elementSection, context, skipCloseSave);
                return;
            default:
                plugin.getLogger().warning("Unknown GUI component '" + component + "'");
        }
    }

    private void renderIndexedVariantComponent(Menu menu, Player viewer, List<Integer> slots, ConfigurationSection elementSection, GuiContext baseContext, AtomicBoolean skipCloseSave, String fallbackVariant, boolean playerKitExists, boolean enderchestExists) {
        int startIndex = elementSection.getInt("start-index", 1);

        for (int i = 0; i < slots.size(); i++) {
            int slotNumber = startIndex + i;
            GuiContext slotContext = baseContext
                    .with("slot", slotNumber)
                    .with("slot_display", slotNumber);

            String variant = fallbackVariant;
            if (playerKitExists) {
                variant = KitManager.get().hasKit(viewer.getUniqueId(), slotNumber) ? "exists" : "missing";
            } else if (enderchestExists) {
                variant = KitManager.get().hasEC(viewer.getUniqueId(), slotNumber) ? "exists" : "missing";
            }

            applyVariant(menu.getSlot(slots.get(i)), viewer, elementSection, variant, slotContext, skipCloseSave);
        }
    }

    private void renderPublicKitList(Menu menu, Player viewer, ConfigurationSection elementSection, GuiContext baseContext, List<Integer> slots, AtomicBoolean skipCloseSave) {
        List<PublicKit> publicKits = KitManager.get().getPublicKitList();
        boolean admin = viewer.hasPermission("perplayerkit.admin");

        for (int i = 0; i < Math.min(slots.size(), publicKits.size()); i++) {
            PublicKit publicKit = publicKits.get(i);
            boolean assigned = KitManager.get().hasPublicKit(publicKit.id);
            String variant;

            if (admin) {
                variant = assigned ? "admin_assigned" : "admin_unassigned";
            } else {
                variant = assigned ? "assigned" : "unassigned";
            }

            GuiContext slotContext = baseContext
                    .with("public_kit_id", publicKit.id)
                    .with("public_kit_name", publicKit.name)
                    .with("public_kit_icon", publicKit.icon);

            applyVariant(menu.getSlot(slots.get(i)), viewer, elementSection, variant, slotContext, skipCloseSave);
        }
    }

    private void renderKitRoomCategoryButtons(Menu menu, Player viewer, List<Integer> slots, ConfigurationSection elementSection, GuiContext baseContext, AtomicBoolean skipCloseSave) {
        int currentPage = getRequiredInt(baseContext, "page", 0);
        int startPage = elementSection.getInt("start-page", 0);

        for (int i = 0; i < slots.size(); i++) {
            int page = startPage + i;
            String basePath = "kitroom.items." + (page + 1);
            String name = plugin.getConfig().getString(basePath + ".name", "Page " + (page + 1));
            Material material = resolveMaterialName(plugin.getConfig().getString(basePath + ".material", "BOOK"), Material.BOOK);

            GuiContext slotContext = baseContext
                    .with("page", page)
                    .with("page_display", page + 1)
                    .with("kitroom_name", name)
                    .with("kitroom_material", material);

            String variant = page == currentPage ? "active" : "default";
            applyVariant(menu.getSlot(slots.get(i)), viewer, elementSection, variant, slotContext, skipCloseSave);
        }
    }

    private void renderKitRoomControl(Menu menu, Player viewer, ConfigurationSection elementSection, GuiContext context, AtomicBoolean skipCloseSave) {
        List<Integer> slots = parseSlots(elementSection.get("slots"));
        if (slots.isEmpty()) {
            return;
        }

        boolean editor = viewer.hasPermission("perplayerkit.editkitroom") || viewer.isOp();
        String variant = editor ? "editor" : "viewer";
        applyVariant(menu.getSlot(slots.get(0)), viewer, elementSection, variant, context, skipCloseSave);
    }

    private void renderItemData(Menu menu, Player viewer, List<Integer> slots, ItemStack[] data, boolean editable) {
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = menu.getSlot(slots.get(i));
            ItemStack item = data != null && i < data.length ? cloneItem(data[i]) : null;
            slot.setItem(item);
            if (editable) {
                allowModification(slot);
            }
        }
    }

    private void applyVariant(Slot slot, Player viewer, ConfigurationSection elementSection, String variantName, GuiContext context, AtomicBoolean skipCloseSave) {
        ConfigurationSection variantSection = getVariantSection(elementSection, variantName);
        if (variantSection == null) {
            return;
        }

        ConfigurationSection itemSection = variantSection.getConfigurationSection("item");
        if (itemSection != null) {
            ItemStack item = buildItem(itemSection, viewer, context);
            if (item != null) {
                slot.setItem(item);
            }
        }

        boolean editable = variantSection.getBoolean("editable", elementSection.getBoolean("editable", false));
        if (editable) {
            allowModification(slot);
        }

        bindActions(slot, getActionSection(elementSection, variantSection), context, skipCloseSave);
    }

    private ConfigurationSection getActionSection(ConfigurationSection elementSection, ConfigurationSection variantSection) {
        ConfigurationSection actionsSection = variantSection.getConfigurationSection("actions");
        if (actionsSection != null) {
            return actionsSection;
        }
        return elementSection.getConfigurationSection("actions");
    }

    private ConfigurationSection getVariantSection(ConfigurationSection elementSection, String variantName) {
        ConfigurationSection variantsSection = elementSection.getConfigurationSection("variants");
        if (variantsSection == null) {
            return elementSection;
        }

        if (variantName != null) {
            ConfigurationSection variantSection = variantsSection.getConfigurationSection(variantName);
            if (variantSection != null) {
                return variantSection;
            }
        }

        return variantsSection.getConfigurationSection("default");
    }

    private void bindActions(Slot slot, ConfigurationSection actionsSection, GuiContext context, AtomicBoolean skipCloseSave) {
        if (actionsSection == null) {
            return;
        }

        slot.setClickHandler((player, info) -> {
            List<Map<?, ?>> actions = resolveActions(actionsSection, info.getClickType());
            if (actions.isEmpty()) {
                return;
            }

            SoundManager.playClick(player);
            for (Map<?, ?> action : actions) {
                if (executeAction(player, info.getClickedMenu(), context, action, skipCloseSave)) {
                    if (readBoolean(action, "stop", false)) {
                        break;
                    }
                }
            }
        });
    }

    private boolean executeAction(Player player, Menu menu, GuiContext context, Map<?, ?> action, AtomicBoolean skipCloseSave) {
        String type = readString(action, "type");
        if (type == null || type.isEmpty()) {
            return false;
        }

        String permission = readString(action, "permission");
        if (permission != null && !permission.isEmpty() && !player.hasPermission(permission)) {
            return false;
        }

        boolean success;
        switch (type.toLowerCase(Locale.ROOT)) {
            case "open-gui":
                success = executeOpenGuiAction(player, action, context);
                return success;
            case "load-player-kit":
                success = KitManager.get().loadKit(player, resolveInt(action, "slot", context, "slot", 1));
                handleConfiguredMessages(player, action, context, success);
                maybeCloseMenu(player, menu, action, success, skipCloseSave);
                return success;
            case "load-enderchest":
                success = KitManager.get().loadEnderchest(player, resolveInt(action, "slot", context, "slot", 1));
                handleConfiguredMessages(player, action, context, success);
                maybeCloseMenu(player, menu, action, success, skipCloseSave);
                return success;
            case "load-public-kit":
                success = KitManager.get().loadPublicKit(player, resolveString(action, "public-kit-id", context, "public_kit_id"));
                handleConfiguredMessages(player, action, context, success);
                maybeCloseMenu(player, menu, action, success, skipCloseSave);
                return success;
            case "close":
                menu.close(player);
                SoundManager.playCloseGui(player);
                return true;
            case "clear-editor-range":
                clearMenuSlots(menu, player, parseSlots(action.get("slots")));
                return true;
            case "import-player-inventory":
                importContentsIntoMenu(menu, player, getPlayerInventoryContents(player), parseSlots(action.get("slots")));
                return true;
            case "import-player-enderchest":
                importContentsIntoMenu(menu, player, getPlayerEnderchestContents(player), parseSlots(action.get("slots")));
                return true;
            case "clear-player-inventory":
                player.getInventory().clear();
                player.sendMessage(ChatColor.GREEN + "Inventory cleared");
                SoundManager.playSuccess(player);
                return true;
            case "repair-player-items":
                BroadcastManager.get().broadcastPlayerRepaired(player);
                PlayerUtil.repairAll(player);
                player.updateInventory();
                SoundManager.playSuccess(player);
                return true;
            case "delete-player-kit":
                success = deletePlayerKit(player, action, context);
                maybeCloseMenu(player, menu, action, success, skipCloseSave);
                return success;
            case "delete-player-enderchest":
                success = deletePlayerEnderchest(player, action, context);
                maybeCloseMenu(player, menu, action, success, skipCloseSave);
                return success;
            case "save-kit-room-page":
                success = saveKitRoomPage(player, menu, context);
                handleConfiguredMessages(player, action, context, success);
                return success;
            case "broadcast-kit-room-opened":
                BroadcastManager.get().broadcastPlayerOpenedKitRoom(player);
                return true;
            default:
                plugin.getLogger().warning("Unknown GUI action type '" + type + "'");
                return false;
        }
    }

    private boolean executeOpenGuiAction(Player player, Map<?, ?> action, GuiContext context) {
        String guiId = readString(action, "gui");
        if (guiId == null || guiId.isEmpty()) {
            return false;
        }

        GuiContext nextContext = buildActionContext(player, context, action.get("context"));
        Menu nextMenu = openConfiguredGui(guiId, player, nextContext);
        return nextMenu != null;
    }

    private boolean deletePlayerKit(Player player, Map<?, ?> action, GuiContext context) {
        UUID targetUuid = resolveTargetUuid(player, context);
        int slot = resolveInt(action, "slot", context, "slot", 1);
        boolean success = KitManager.get().deleteKit(targetUuid, slot);

        if (success) {
            SoundManager.playSuccess(player);
        } else {
            SoundManager.playFailure(player);
        }

        handleConfiguredMessages(player, action, context, success);
        return success;
    }

    private boolean deletePlayerEnderchest(Player player, Map<?, ?> action, GuiContext context) {
        UUID targetUuid = resolveTargetUuid(player, context);
        int slot = resolveInt(action, "slot", context, "slot", 1);
        boolean success = KitManager.get().deleteEnderchest(targetUuid, slot);

        if (success) {
            SoundManager.playSuccess(player);
        } else {
            SoundManager.playFailure(player);
        }

        handleConfiguredMessages(player, action, context, success);
        return success;
    }

    private boolean saveKitRoomPage(Player player, Menu menu, GuiContext context) {
        int page = getRequiredInt(context, "page", 0);
        List<Integer> dataSlots = getComponentSlots("kit-room", "kit-room-data");
        if (dataSlots.isEmpty()) {
            return false;
        }

        ItemStack[] kitRoomItems = readMenuItems(menu, player, dataSlots);
        KitRoomDataManager.get().setKitRoom(page, kitRoomItems);
        KitRoomDataManager.get().saveToDBAsync();
        SoundManager.playSuccess(player);
        return true;
    }

    private void maybeCloseMenu(Player player, Menu menu, Map<?, ?> action, boolean success, AtomicBoolean skipCloseSave) {
        if (!success) {
            return;
        }

        if (readBoolean(action, "skip-close-save", false)) {
            skipCloseSave.set(true);
        }

        if (readBoolean(action, "close", false)) {
            menu.close(player);
            SoundManager.playCloseGui(player);
        }
    }

    private void handleConfiguredMessages(Player player, Map<?, ?> action, GuiContext context, boolean success) {
        String message = readString(action, success ? "success-message" : "failure-message");
        if (message == null || message.isEmpty()) {
            return;
        }
        player.sendMessage(resolveText(message, player, context));
    }

    private void clearMenuSlots(Menu menu, Player player, List<Integer> slots) {
        for (int slotIndex : slots) {
            menu.getSlot(slotIndex).setRawItem(player, null);
        }
    }

    private void importContentsIntoMenu(Menu menu, Player player, ItemStack[] source, List<Integer> slots) {
        for (int i = 0; i < slots.size(); i++) {
            ItemStack item = i < source.length ? source[i] : null;
            menu.getSlot(slots.get(i)).setRawItem(player, cloneItem(item));
        }
    }

    private ItemStack[] getPlayerInventoryContents(Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        if (!plugin.getConfig().getBoolean("anti-exploit.import-filter", false)) {
            return contents;
        }
        return ItemFilter.get().filterItemStack(contents);
    }

    private ItemStack[] getPlayerEnderchestContents(Player player) {
        ItemStack[] contents = player.getEnderChest().getContents();
        if (!plugin.getConfig().getBoolean("anti-exploit.import-filter", false)) {
            return contents;
        }
        return ItemFilter.get().filterItemStack(contents);
    }

    private void applyCloseHandler(String guiId, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        switch (guiId) {
            case "player-kit-editor":
                menu.setCloseHandler((player, closedMenu) -> savePlayerKitEditor(player, closedMenu, context, skipCloseSave));
                return;
            case "public-kit-editor":
                menu.setCloseHandler((player, closedMenu) -> savePublicKitEditor(player, closedMenu, context, skipCloseSave));
                return;
            case "enderchest-editor":
                menu.setCloseHandler((player, closedMenu) -> saveEnderchestEditor(player, closedMenu, context, skipCloseSave));
                return;
            case "inspect-kit":
                menu.setCloseHandler((player, closedMenu) -> saveInspectKitEditor(player, closedMenu, context, skipCloseSave));
                return;
            case "inspect-enderchest":
                menu.setCloseHandler((player, closedMenu) -> saveInspectEnderchestEditor(player, closedMenu, context, skipCloseSave));
                return;
            default:
        }
    }

    private void savePlayerKitEditor(Player player, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        if (skipCloseSave.get()) {
            return;
        }

        int slot = getRequiredInt(context, "slot", 1);
        ItemStack[] kit = readMenuItems(menu, player, getComponentSlots("player-kit-editor", "kit-data"));
        KitManager.get().savekit(player.getUniqueId(), slot, kit);
    }

    private void savePublicKitEditor(Player player, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        if (skipCloseSave.get()) {
            return;
        }

        String publicKitId = context.getString("public_kit_id");
        if (publicKitId == null || publicKitId.isEmpty()) {
            return;
        }

        ItemStack[] kit = readMenuItems(menu, player, getComponentSlots("public-kit-editor", "kit-data"));
        KitManager.get().savePublicKit(player, publicKitId, kit);
    }

    private void saveEnderchestEditor(Player player, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        if (skipCloseSave.get()) {
            return;
        }

        int slot = getRequiredInt(context, "slot", 1);
        ItemStack[] kit = readMenuItems(menu, player, getComponentSlots("enderchest-editor", "enderchest-data"));
        KitManager.get().saveEC(player.getUniqueId(), slot, kit);
    }

    private void saveInspectKitEditor(Player player, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        if (skipCloseSave.get() || !player.hasPermission("perplayerkit.admin")) {
            return;
        }

        UUID targetUuid = context.getUuid("target_uuid");
        if (targetUuid == null) {
            return;
        }

        int slot = getRequiredInt(context, "slot", 1);
        String targetName = context.getString("target_name");
        ItemStack[] kit = readMenuItems(menu, player, getComponentSlots("inspect-kit", "kit-data"));

        if (KitManager.get().savekit(targetUuid, slot, kit, true)) {
            player.sendMessage(ChatColor.GREEN + "Kit " + slot + " updated for player " + targetName + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to update kit for player " + targetName + "!");
        }
    }

    private void saveInspectEnderchestEditor(Player player, Menu menu, GuiContext context, AtomicBoolean skipCloseSave) {
        if (skipCloseSave.get() || !player.hasPermission("perplayerkit.admin")) {
            return;
        }

        UUID targetUuid = context.getUuid("target_uuid");
        if (targetUuid == null) {
            return;
        }

        int slot = getRequiredInt(context, "slot", 1);
        String targetName = context.getString("target_name");
        ItemStack[] kit = readMenuItems(menu, player, getComponentSlots("inspect-enderchest", "enderchest-data"));

        if (KitManager.get().saveECSilent(targetUuid, slot, kit)) {
            player.sendMessage(ChatColor.GREEN + "Enderchest " + slot + " updated for player " + targetName + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Failed to update enderchest for player " + targetName + "!");
        }
    }

    private ItemStack[] readMenuItems(Menu menu, Player player, List<Integer> slots) {
        ItemStack[] contents = new ItemStack[slots.size()];
        for (int i = 0; i < slots.size(); i++) {
            contents[i] = cloneItem(menu.getSlot(slots.get(i)).getRawItem(player));
        }
        return contents;
    }

    private List<Integer> getComponentSlots(String guiId, String componentName) {
        ConfigurationSection guiSection = guiConfigManager.getGuiSection(guiId);
        if (guiSection == null) {
            return Collections.emptyList();
        }

        for (ConfigurationSection elementSection : getOrderedElementSections(guiSection)) {
            if (!"component".equalsIgnoreCase(elementSection.getString("type", ""))
                    || !componentName.equalsIgnoreCase(elementSection.getString("component", ""))) {
                continue;
            }
            return parseSlots(elementSection.get("slots"));
        }

        return Collections.emptyList();
    }

    private List<ConfigurationSection> getOrderedElementSections(ConfigurationSection guiSection) {
        ConfigurationSection elementsSection = guiSection.getConfigurationSection("elements");
        if (elementsSection == null) {
            return Collections.emptyList();
        }

        List<ConfigurationSection> sections = new ArrayList<>();
        for (String key : elementsSection.getKeys(false)) {
            ConfigurationSection section = elementsSection.getConfigurationSection(key);
            if (section != null) {
                sections.add(section);
            }
        }
        sections.sort((left, right) -> Integer.compare(left.getInt("order", 0), right.getInt("order", 0)));
        return sections;
    }

    private boolean isVisibleToViewer(ConfigurationSection section, Player viewer) {
        String permission = section.getString("permission");
        if (permission != null && !permission.isEmpty() && !viewer.hasPermission(permission)) {
            return false;
        }

        String excludedPermission = section.getString("exclude-permission");
        return excludedPermission == null || excludedPermission.isEmpty() || !viewer.hasPermission(excludedPermission);
    }

    private void allowModification(Slot slot) {
        slot.setClickOptions(ClickOptions.ALLOW_ALL);
    }

    private ItemStack[] resolveKitData(Player viewer, GuiContext context) {
        String publicKitId = context.getString("public_kit_id");
        if (publicKitId != null && !publicKitId.isEmpty()) {
            return cloneItemArray(KitManager.get().getItemStackArrayById(IDUtil.getPublicKitId(publicKitId)));
        }

        UUID ownerUuid = resolveTargetUuid(viewer, context);
        int slot = getRequiredInt(context, "slot", 1);
        return cloneItemArray(KitManager.get().getItemStackArrayById(IDUtil.getPlayerKitId(ownerUuid, slot)));
    }

    private ItemStack[] resolveEnderchestData(Player viewer, GuiContext context) {
        UUID ownerUuid = resolveTargetUuid(viewer, context);
        int slot = getRequiredInt(context, "slot", 1);
        return cloneItemArray(KitManager.get().getItemStackArrayById(IDUtil.getECId(ownerUuid, slot)));
    }

    private ItemStack[] resolveKitRoomData(GuiContext context) {
        int page = getRequiredInt(context, "page", 0);
        return cloneItemArray(KitRoomDataManager.get().getKitRoomPage(page));
    }

    private UUID resolveTargetUuid(Player viewer, GuiContext context) {
        UUID targetUuid = context.getUuid("target_uuid");
        return targetUuid != null ? targetUuid : viewer.getUniqueId();
    }

    private GuiContext enrichPublicKitContext(GuiContext context, String publicKitId) {
        PublicKit publicKit = KitManager.get().getPublicKitList().stream()
                .filter(entry -> entry.id.equals(publicKitId))
                .findFirst()
                .orElse(null);

        GuiContext enriched = context.with("public_kit_id", publicKitId);
        if (publicKit != null) {
            enriched = enriched
                    .with("public_kit_name", publicKit.name)
                    .with("public_kit_icon", publicKit.icon);
        }
        return enriched;
    }

    private List<Integer> parseSlots(Object rawValue) {
        if (rawValue == null) {
            return Collections.emptyList();
        }

        LinkedHashSet<Integer> slots = new LinkedHashSet<>();
        if (rawValue instanceof Number number) {
            slots.add(number.intValue());
        } else if (rawValue instanceof String stringValue) {
            parseSlotString(stringValue, slots);
        } else if (rawValue instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof Number number) {
                    slots.add(number.intValue());
                } else if (entry instanceof String stringValue) {
                    parseSlotString(stringValue, slots);
                }
            }
        }
        return new ArrayList<>(slots);
    }

    private void parseSlotString(String slotString, LinkedHashSet<Integer> slots) {
        for (String part : slotString.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            if (trimmed.contains("-")) {
                String[] bounds = trimmed.split("-", 2);
                try {
                    int start = Integer.parseInt(bounds[0].trim());
                    int end = Integer.parseInt(bounds[1].trim());
                    if (start <= end) {
                        for (int value = start; value <= end; value++) {
                            slots.add(value);
                        }
                    } else {
                        for (int value = start; value >= end; value--) {
                            slots.add(value);
                        }
                    }
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Invalid slot range '" + trimmed + "' in guis.yml");
                }
                continue;
            }

            try {
                slots.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException ignored) {
                plugin.getLogger().warning("Invalid slot '" + trimmed + "' in guis.yml");
            }
        }
    }

    private ItemStack buildItem(ConfigurationSection itemSection, Player viewer, GuiContext context) {
        if (itemSection == null) {
            return null;
        }

        Material material = resolveMaterial(itemSection.getString("material"), viewer, context);
        if (material == null) {
            return null;
        }

        int amount = Math.max(1, itemSection.getInt("amount", 1));
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = itemSection.getString("name");
            if (name != null) {
                meta.setDisplayName(resolveText(name, viewer, context));
            }

            List<String> loreLines = itemSection.getStringList("lore");
            if (!loreLines.isEmpty()) {
                List<String> lore = new ArrayList<>(loreLines.size());
                for (String loreLine : loreLines) {
                    lore.add(resolveText(loreLine, viewer, context));
                }
                meta.setLore(lore);
            }

            if (itemSection.getBoolean("glow", false)) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            if (itemSection.getBoolean("hide-flags", false)) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_ATTRIBUTES,
                        ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DYE);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private Material resolveMaterial(String materialName, Player viewer, GuiContext context) {
        if (materialName == null || materialName.isEmpty()) {
            return null;
        }

        String resolvedName = resolvePlainValue(materialName, viewer, context);
        if ("@glass".equalsIgnoreCase(resolvedName)) {
            return StyleManager.get().getGlassMaterial();
        }

        if (resolvedName.startsWith("@")) {
            Object dynamicValue = context.get(resolvedName.substring(1));
            if (dynamicValue instanceof Material material) {
                return material;
            }
            if (dynamicValue != null) {
                resolvedName = String.valueOf(dynamicValue);
            }
        }

        return resolveMaterialName(resolvedName, null);
    }

    private Material resolveMaterialName(String materialName, Material fallback) {
        try {
            return Material.valueOf(materialName.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private String resolveText(String value, Player viewer, GuiContext context) {
        if (value == null) {
            return null;
        }

        String resolved = resolvePlainValue(value, viewer, context);
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            resolved = PlaceholderAPI.setPlaceholders(viewer, resolved);
        }
        return StyleManager.convertMiniMessage(resolved);
    }

    private String resolvePlainValue(String value, Player viewer, GuiContext context) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String replacement = getPlaceholderValue(matcher.group(1), viewer, context);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private String getPlaceholderValue(String placeholder, Player viewer, GuiContext context) {
        Object contextValue = context.get(placeholder);
        if (contextValue != null) {
            return String.valueOf(contextValue);
        }

        switch (placeholder) {
            case "viewer_name":
                return viewer.getName();
            case "viewer_display_name":
                return viewer.getDisplayName();
            case "viewer_uuid":
                return viewer.getUniqueId().toString();
            case "primary_color":
                return StyleManager.get().getPrimaryColor();
            case "glass_material":
                return StyleManager.get().getGlassMaterial().name();
            default:
                return "%" + placeholder + "%";
        }
    }

    private GuiContext buildActionContext(Player player, GuiContext baseContext, Object rawContext) {
        if (!(rawContext instanceof Map<?, ?> rawContextMap)) {
            return baseContext;
        }

        GuiContext nextContext = baseContext;
        for (Map.Entry<?, ?> entry : rawContextMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }

            String key = String.valueOf(entry.getKey());
            Object value = resolveContextValue(player, baseContext, entry.getValue());
            if (value != null) {
                nextContext = nextContext.with(key, value);
            }
        }
        return nextContext;
    }

    private Object resolveContextValue(Player player, GuiContext context, Object rawValue) {
        if (!(rawValue instanceof String stringValue)) {
            return rawValue;
        }

        if (stringValue.startsWith("%") && stringValue.endsWith("%") && stringValue.indexOf('%', 1) == stringValue.length() - 1) {
            String placeholder = stringValue.substring(1, stringValue.length() - 1);
            Object existing = context.get(placeholder);
            if (existing != null) {
                return existing;
            }
        }

        return resolvePlainValue(stringValue, player, context);
    }

    private List<Map<?, ?>> resolveActions(ConfigurationSection actionsSection, ClickType clickType) {
        List<String> candidates = new ArrayList<>();
        candidates.add(clickType.name().toLowerCase(Locale.ROOT));
        if (clickType.isShiftClick()) {
            candidates.add("shift");
        }
        if (clickType.isLeftClick()) {
            candidates.add("left");
        } else if (clickType.isRightClick()) {
            candidates.add("right");
        } else if (clickType == ClickType.MIDDLE) {
            candidates.add("middle");
        }
        candidates.add("any");

        for (String candidate : candidates) {
            List<Map<?, ?>> actions = readActionList(actionsSection, candidate);
            if (!actions.isEmpty()) {
                return actions;
            }
        }

        return Collections.emptyList();
    }

    private List<Map<?, ?>> readActionList(ConfigurationSection actionsSection, String path) {
        List<?> rawActions = actionsSection.getList(path);
        if (rawActions == null || rawActions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<?, ?>> actions = new ArrayList<>();
        for (Object rawAction : rawActions) {
            if (rawAction instanceof Map<?, ?> map) {
                actions.add(map);
            }
        }
        return actions;
    }

    private String readString(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private boolean readBoolean(Map<?, ?> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return defaultValue;
    }

    private String resolveString(Map<?, ?> action, String key, GuiContext context, String fallbackContextKey) {
        String rawValue = readString(action, key);
        if (rawValue != null) {
            if (rawValue.startsWith("%") && rawValue.endsWith("%") && rawValue.indexOf('%', 1) == rawValue.length() - 1) {
                String placeholder = rawValue.substring(1, rawValue.length() - 1);
                Object contextValue = context.get(placeholder);
                if (contextValue != null) {
                    return String.valueOf(contextValue);
                }
            }
            return rawValue;
        }
        return context.getString(fallbackContextKey);
    }

    private int resolveInt(Map<?, ?> action, String key, GuiContext context, String fallbackContextKey, int defaultValue) {
        Object rawValue = action.get(key);
        if (rawValue instanceof Number number) {
            return number.intValue();
        }
        if (rawValue instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue);
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }

        Integer contextValue = context.getInt(fallbackContextKey);
        return contextValue != null ? contextValue : defaultValue;
    }

    private int getRequiredInt(GuiContext context, String key, int defaultValue) {
        Integer value = context.getInt(key);
        return value != null ? value : defaultValue;
    }

    private ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }

    private ItemStack[] cloneItemArray(ItemStack[] items) {
        if (items == null) {
            return null;
        }

        ItemStack[] clone = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            clone[i] = cloneItem(items[i]);
        }
        return clone;
    }

    private String getPlayerName(UUID uuid) {
        Player onlinePlayer = Bukkit.getPlayer(uuid);
        if (onlinePlayer != null) {
            return onlinePlayer.getName();
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
        return offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
    }
}
