package dev.noah.perplayerkit;

import dev.noah.perplayerkit.gui.GUI;
import dev.noah.perplayerkit.storage.StorageManager;
import dev.noah.perplayerkit.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.scheduler.BukkitScheduler;
import org.ipvp.canvas.slot.Slot;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KitBehaviorTest {
    @TempDir Path directory;
    PerPlayerKit plugin;
    YamlConfiguration config;
    KitManager kits;
    Player player;
    UUID uuid;
    StorageManager storage;
    BukkitScheduler scheduler;
    final Map<String, String> database = new ConcurrentHashMap<>();
    final Map<String, ItemStack[]> encoded = new ConcurrentHashMap<>();
    final List<Runnable> mainTasks = Collections.synchronizedList(new ArrayList<>());
    MockedStatic<Bukkit> bukkit;
    MockedStatic<Lang> language;
    MockedStatic<SoundManager> sounds;
    MockedStatic<BroadcastManager> broadcasts;
    MockedStatic<Serializer> serializer;

    @BeforeEach void setup() throws Exception {
        plugin = mock(PerPlayerKit.class);
        config = new YamlConfiguration();
        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("kit-test"));
        when(plugin.getDataFolder()).thenReturn(directory.toFile());
        when(plugin.isEnabled()).thenReturn(true);
        player = mock(Player.class);
        uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getInventory()).thenReturn(mock(PlayerInventory.class));
        when(player.getEnderChest()).thenReturn(mock(Inventory.class));
        when(player.getInventory().getContents()).thenReturn(new ItemStack[41]);
        scheduler = mock(BukkitScheduler.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        doAnswer(i -> { mainTasks.add(i.getArgument(1)); return null; })
                .when(scheduler).runTask(eq(plugin), any(Runnable.class));
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.equals(any(), any())).thenAnswer(i -> Objects.equals(i.getArgument(0), i.getArgument(1)));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(player);
        language = mockStatic(Lang.class);
        language.when(Lang::get).thenReturn(mock(Lang.class));
        sounds = mockStatic(SoundManager.class);
        broadcasts = mockStatic(BroadcastManager.class);
        broadcasts.when(BroadcastManager::get).thenReturn(mock(BroadcastManager.class));
        serializer = mockStatic(Serializer.class);
        serializer.when(() -> Serializer.itemStackArrayToBase64(any())).thenAnswer(i -> {
            String id = UUID.randomUUID().toString(); encoded.put(id, ItemFilter.copy(i.getArgument(0))); return id;
        });
        serializer.when(() -> Serializer.itemStackArrayFromBase64(anyString()))
                .thenAnswer(i -> ItemFilter.copy(encoded.get(i.getArgument(0))));
        storage = mock(StorageManager.class);
        doAnswer(i -> { database.put(i.getArgument(0), i.getArgument(1)); return null; })
                .when(storage).saveKitDataByID(anyString(), anyString());
        doAnswer(i -> { database.remove(i.getArgument(0)); return null; }).when(storage).deleteKitByID(anyString());
        when(storage.getKitDataByID(anyString())).thenAnswer(i -> database.getOrDefault(i.getArgument(0), "error"));
        PerPlayerKit.storageManager = storage;
        new ItemFilter(plugin);
        kits = new KitManager(plugin);
    }

    @AfterEach void close() {
        kits.shutdown();
        serializer.close(); broadcasts.close(); sounds.close(); language.close(); bukkit.close();
    }

    ItemStack[] kit(Material material) { ItemStack[] items = new ItemStack[41]; items[0] = new ItemStack(material); return items; }
    void settle() { kits.storageWork().run(() -> {}).join(); }

    @Test void filterNeverMutatesInputOrStoredDefinition() {
        config.set("anti-exploit.only-allow-kitroom-items", true);
        ItemFilter filter = new ItemFilter(plugin);
        ItemStack[] original = kit(Material.STONE);
        assertNull(filter.filterItemStack(original)[0]);
        assertEquals(Material.STONE, original[0].getType());
        kits.savekit(uuid, 1, original, true);
        settle();
        assertEquals(Material.STONE, encoded.get(database.get(IDUtil.getPlayerKitId(uuid, 1)))[0].getType());
        assertEquals(Material.STONE, kits.getPlayerKit(uuid, 1)[0].getType());
    }

    @Test void emptyWhitelistDoesNotReplaceInventory() {
        config.set("anti-exploit.only-allow-kitroom-items", true);
        new ItemFilter(plugin);
        kits.savePublicKit("crystal", kit(Material.STONE));
        assertFalse(kits.loadPublicKit(player, "crystal"));
        verify(player.getInventory(), never()).setContents(any());
    }

    @Test void publicKitBecomesLastKitForRespawnAndRegear() {
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        kits.savePublicKit("crystal", kit(Material.DIRT));
        kits.loadKit(player, 1);
        kits.loadPublicKit(player, "crystal");
        clearInvocations(player.getInventory());
        assertTrue(kits.loadLastKit(player));
        ArgumentCaptor<ItemStack[]> restored = ArgumentCaptor.forClass(ItemStack[].class);
        verify(player.getInventory()).setContents(restored.capture());
        assertEquals(Material.DIRT, restored.getValue()[0].getType());
        config.set("regear.whitelist", List.of("DIRT"));
        clearInvocations(player.getInventory());
        assertTrue(kits.regearLastKit(player));
        verify(player.getInventory()).setContents(restored.capture());
        assertEquals(Material.DIRT, restored.getValue()[0].getType());
    }

    @Test void saveThenDeleteIsOrderedAndQuitDoesNotResurrectIt() {
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        kits.deleteKit(uuid, 1);
        kits.unloadPlayer(uuid);
        settle();
        assertFalse(database.containsKey(IDUtil.getPlayerKitId(uuid, 1)));
        assertFalse(kits.hasKit(uuid, 1));
    }

    @Test void savedSnapshotAndGetterDoNotAliasCallerItems() {
        ItemStack[] original = kit(Material.STONE);
        kits.savekit(uuid, 1, original, true);
        original[0].setType(Material.DIRT);
        kits.getPlayerKit(uuid, 1)[0].setType(Material.DIRT);
        settle();
        assertEquals(Material.STONE, encoded.get(database.get(IDUtil.getPlayerKitId(uuid, 1)))[0].getType());
        assertEquals(Material.STONE, kits.getPlayerKit(uuid, 1)[0].getType());
    }

    @Test void savingOneRoomPageLeavesOtherPagesUnconfigured() {
        KitRoomDataManager room = new KitRoomDataManager(plugin);
        room.setKitRoom(0, new ItemStack[45]);
        room.savePagesToDBAsync(List.of(0));
        assertTrue(KitRoomDataManager.hasStoredPage(0));
        assertFalse(KitRoomDataManager.hasStoredPage(1));
        settle();
        assertEquals(Set.of("kitroom0"), database.keySet());
    }

    @Test void guiRepairCannotBypassPermission() {
        Slot slot = mock(Slot.class);
        new GUI(plugin).addRepairButton(slot);
        ArgumentCaptor<Slot.ClickHandler> click = ArgumentCaptor.forClass(Slot.ClickHandler.class);
        verify(slot).setClickHandler(click.capture());
        try (MockedStatic<PlayerUtil> players = mockStatic(PlayerUtil.class)) {
            click.getValue().click(player, null);
            players.verifyNoInteractions();
        }
    }

    @Test void enderchestRestoreDoesNotChangeRememberedInventoryKit() {
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        kits.loadKit(player, 1);
        ItemStack[] ec = new ItemStack[27]; ec[0] = new ItemStack(Material.DIRT);
        kits.saveECSilent(uuid, 2, ec);
        kits.loadEnderchest(player, 2);
        assertEquals(1, kits.getLastKitLoaded(uuid));
        clearInvocations(player.getEnderChest());
        kits.restoreLastEnderchest(player);
        verify(player.getEnderChest()).setContents(any());
    }

    @Test void staleJoinCallbackCannotOverwriteARejoinedPlayersData() throws Exception {
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        settle();
        kits.unloadPlayer(uuid);
        kits.loadPlayerDataAsync(uuid);
        settle();
        assertTrue(kits.isLoading(uuid));
        assertFalse(kits.savekit(uuid, 1, kit(Material.GRAVEL), true));
        kits.unloadPlayer(uuid);
        String fresh = Serializer.itemStackArrayToBase64(kit(Material.DIRT));
        database.put(IDUtil.getPlayerKitId(uuid, 1), fresh);
        kits.loadPlayerDataAsync(uuid);
        settle();
        new ArrayList<>(mainTasks).forEach(Runnable::run);
        assertFalse(kits.isLoading(uuid));
        assertEquals(Material.DIRT, kits.getPlayerKit(uuid, 1)[0].getType());
    }

    @Test void retryOfFailedSaveCannotOverwriteNewerSuccessfulSave() {
        doThrow(new IllegalStateException("test storage outage")).when(storage).saveKitDataByID(anyString(), anyString());
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        settle();
        doAnswer(i -> { database.put(i.getArgument(0), i.getArgument(1)); return null; })
                .when(storage).saveKitDataByID(anyString(), anyString());
        kits.savekit(uuid, 1, kit(Material.DIRT), true);
        kits.retryFailedWrites();
        settle();
        assertEquals(Material.DIRT, encoded.get(database.get(IDUtil.getPlayerKitId(uuid, 1)))[0].getType());
    }

    @Test void repairSkipsItemsWithoutDurability() {
        ItemStack food = mock(ItemStack.class);
        when(food.getItemMeta()).thenReturn(mock(org.bukkit.inventory.meta.ItemMeta.class));
        assertDoesNotThrow(() -> PlayerUtil.repairItem(food));
        verify(food, never()).setItemMeta(any());
    }

    @Test void publicMenuPaginatesMoreThanThirtySixKits() throws Exception {
        for (int i = 0; i < 40; i++) kits.getPublicKitList().add(new PublicKit("kit" + i, "Kit " + i, Material.STONE));
        when(player.hasPermission("perplayerkit.publickit")).thenReturn(true);
        org.ipvp.canvas.Menu menu = mock(org.ipvp.canvas.Menu.class);
        Map<Integer, Slot> slots = new HashMap<>();
        when(menu.getSlot(anyInt())).thenAnswer(i -> {
            int index = i.getArgument(0);
            assertTrue(index >= 0 && index < 54, "menu slot outside inventory: " + index);
            return slots.computeIfAbsent(index, key -> mock(Slot.class));
        });
        try (MockedStatic<dev.noah.perplayerkit.gui.GuiMenuFactory> factory = mockStatic(dev.noah.perplayerkit.gui.GuiMenuFactory.class);
             MockedStatic<dev.noah.perplayerkit.gui.ItemUtil> items = mockStatic(dev.noah.perplayerkit.gui.ItemUtil.class);
             MockedStatic<?> compat = mockStatic(Class.forName("dev.noah.perplayerkit.gui.GuiCompat"));
             MockedStatic<DisabledCommand> disabled = mockStatic(DisabledCommand.class)) {
            factory.when(dev.noah.perplayerkit.gui.GuiMenuFactory::createPublicKitRoomMenu)
                    .thenReturn(new dev.noah.perplayerkit.gui.GuiMenuFactory.TitledMenu(menu, "Public kits"));
            new GUI(plugin).OpenPublicKitMenu(player);
            ArgumentCaptor<Slot.ClickHandler> next = ArgumentCaptor.forClass(Slot.ClickHandler.class);
            verify(slots.get(52)).setClickHandler(next.capture());
            next.getValue().click(player, null);
            verify(menu, times(2)).open(player);
        }
    }

    enum SaveRoute {
        PRIVATE, PRIVATE_SILENT, PRIVATE_FLAG_FALSE, PUBLIC, PUBLIC_WITH_PLAYER, ENDERCHEST, ENDERCHEST_SILENT;
        int size() { return name().startsWith("ENDERCHEST") ? 27 : 41; }
    }

    private boolean save(SaveRoute route, ItemStack[] items) {
        return switch (route) {
            case PRIVATE -> kits.savekit(uuid, 1, items);
            case PRIVATE_SILENT -> kits.savekit(uuid, 1, items, true);
            case PRIVATE_FLAG_FALSE -> kits.savekit(uuid, 1, items, false);
            case PUBLIC -> kits.savePublicKit("custom", items);
            case PUBLIC_WITH_PLAYER -> kits.savePublicKit(player, "custom", items);
            case ENDERCHEST -> kits.saveEC(uuid, 1, items);
            case ENDERCHEST_SILENT -> kits.saveECSilent(uuid, 1, items);
        };
    }

    private ItemStack[] saved(SaveRoute route) {
        return switch (route) {
            case PUBLIC, PUBLIC_WITH_PLAYER -> kits.getPublicKit("custom");
            case ENDERCHEST, ENDERCHEST_SILENT -> kits.getPlayerEC(uuid, 1);
            default -> kits.getPlayerKit(uuid, 1);
        };
    }

    @ParameterizedTest @EnumSource(SaveRoute.class)
    void malformedSaveLeavesTheExistingKitIntact(SaveRoute route) {
        ItemStack[] original = new ItemStack[route.size()]; original[0] = new ItemStack(Material.STONE);
        assertTrue(save(route, original));
        for (ItemStack[] invalid : new ItemStack[][] {null, new ItemStack[1], new ItemStack[route.size() + 1]}) {
            if (invalid != null) invalid[0] = new ItemStack(Material.DIRT);
            assertFalse(assertDoesNotThrow(() -> save(route, invalid)));
            assertEquals(Material.STONE, saved(route)[0].getType());
        }
    }

    @ParameterizedTest @EnumSource(SaveRoute.class)
    void airOnlySaveCannotEraseAnExistingKit(SaveRoute route) {
        ItemStack[] original = new ItemStack[route.size()]; original[0] = new ItemStack(Material.STONE);
        assertTrue(save(route, original));
        ItemStack[] empty = new ItemStack[route.size()]; empty[0] = new ItemStack(Material.AIR);
        assertFalse(save(route, empty));
        assertEquals(Material.STONE, saved(route)[0].getType());
    }

    @ParameterizedTest @EnumSource(value=SaveRoute.class, names={"PRIVATE", "PRIVATE_SILENT", "PRIVATE_FLAG_FALSE", "PUBLIC", "PUBLIC_WITH_PLAYER"})
    void invalidArmorIsRemovedFromTheSaveWithoutMutatingTheCaller(SaveRoute route) {
        ItemStack[] items = kit(Material.STONE);
        items[36] = new ItemStack(Material.DIRT);
        items[37] = new ItemStack(Material.DIAMOND_LEGGINGS);
        items[38] = new ItemStack(Material.ELYTRA);
        items[39] = new ItemStack(Material.STONE);
        assertTrue(save(route, items));
        assertEquals(Material.DIRT, items[36].getType());
        assertEquals(Material.STONE, items[39].getType());
        ItemStack[] saved = saved(route);
        assertNull(saved[36]);
        assertNull(saved[39]);
        assertEquals(Material.DIAMOND_LEGGINGS, saved[37].getType());
        assertEquals(Material.ELYTRA, saved[38].getType());
    }

    @Test void malformedStoredLayoutKeepsEditingBlocked() throws Exception {
        database.put(IDUtil.getPlayerKitId(uuid, 1), Serializer.itemStackArrayToBase64(new ItemStack[1]));
        kits.loadPlayerDataAsync(uuid);
        settle();
        new ArrayList<>(mainTasks).forEach(Runnable::run);
        assertTrue(kits.isLoading(uuid));
        assertFalse(kits.hasKit(uuid, 1));
        assertFalse(kits.savekit(uuid, 1, kit(Material.STONE), true));
    }

    @Test void storageReadFailureKeepsEditingBlocked() {
        when(storage.getKitDataByID(anyString())).thenThrow(new IllegalStateException("test read failure"));
        kits.loadPlayerDataAsync(uuid);
        settle();
        new ArrayList<>(mainTasks).forEach(Runnable::run);
        assertTrue(kits.isLoading(uuid));
        assertFalse(kits.saveECSilent(uuid, 1, new ItemStack[27]));
    }

    @Test void invalidRoomLayoutCannotReplaceTheCurrentPage() {
        KitRoomDataManager room = new KitRoomDataManager(plugin);
        ItemStack[] page = new ItemStack[45]; page[0] = new ItemStack(Material.STONE);
        room.setKitRoom(0, page);
        database.put("kitroom0", Serializer.itemStackArrayToBase64(new ItemStack[1]));
        room.loadFromDB();
        assertEquals(45, room.getKitRoomPage(0).length);
        assertEquals(Material.STONE, room.getKitRoomPage(0)[0].getType());
        assertTrue(KitRoomDataManager.hasStoredPage(0), "autosetup must not overwrite unreadable stored data");
    }

    @ParameterizedTest @EnumSource(value=SaveRoute.class, names={"PRIVATE", "PRIVATE_SILENT", "PRIVATE_FLAG_FALSE", "PUBLIC", "PUBLIC_WITH_PLAYER"})
    void aSaveWithOnlyInvalidArmorIsEmptyAfterValidation(SaveRoute route) {
        ItemStack[] items = new ItemStack[41];
        items[36] = new ItemStack(Material.STONE);
        assertFalse(save(route, items));
        assertNull(saved(route));
        assertEquals(Material.STONE, items[36].getType());
    }

    @ParameterizedTest @ValueSource(booleans={false, true})
    void loadEffectsOnlyApplyToTheirConfiguredKitType(boolean enderchest) {
        String prefix = enderchest ? "enderchests.load." : "kits.load.";
        for (String effect : List.of("heal", "feed", "saturate", "clear-effects")) config.set(prefix + effect, true);
        when(player.getMaxHealth()).thenReturn(40.0);
        org.bukkit.potion.PotionEffect potion = mock(org.bukkit.potion.PotionEffect.class);
        org.bukkit.potion.PotionEffectType type = mock(org.bukkit.potion.PotionEffectType.class);
        when(potion.getType()).thenReturn(type);
        when(player.getActivePotionEffects()).thenReturn(List.of(potion));
        kits.savekit(uuid, 1, kit(Material.STONE), true);
        ItemStack[] ec = new ItemStack[27]; ec[0] = new ItemStack(Material.DIRT);
        kits.saveECSilent(uuid, 1, ec);
        if (enderchest) kits.loadKitSilent(player, 1);
        else kits.loadEnderchestSilent(player, 1);
        verify(player, never()).setHealth(anyDouble());
        verify(player, never()).setFoodLevel(anyInt());
        verify(player, never()).setSaturation(anyFloat());
        verify(player, never()).removePotionEffect(any());
        if (enderchest) kits.loadEnderchestSilent(player, 1);
        else kits.loadKitSilent(player, 1);
        verify(player).setHealth(40.0);
        verify(player).setFoodLevel(20);
        verify(player).setSaturation(20);
        verify(player).removePotionEffect(type);
    }

    @Test void offlineUpdatesKeepSupportedHiddenSlotsButRejectInvalidIdentities() {
        bukkit.when(() -> Bukkit.getPlayer(uuid)).thenReturn(null);
        assertFalse(kits.savekit(uuid, 1, kit(Material.STONE)));
        assertTrue(kits.savekit(uuid, KitSlots.MAX_LIMIT, kit(Material.STONE), true));
        assertNotNull(kits.getPlayerKit(uuid, KitSlots.MAX_LIMIT));
        assertFalse(kits.savekit(null, 1, kit(Material.STONE), true));
        assertFalse(kits.savekit(uuid, 0, kit(Material.STONE), true));
        assertFalse(kits.savekit(uuid, KitSlots.MAX_LIMIT + 1, kit(Material.STONE), true));
        assertFalse(kits.savePublicKit(null, kit(Material.STONE)));
        assertFalse(kits.savePublicKit("", kit(Material.STONE)));
        assertFalse(kits.savePublicKit(null, "custom", kit(Material.STONE)));
        assertFalse(kits.hasPublicKit("custom"));
    }
    @Test void shareCodeFiltersAtDeliveryAndRetainsTheStoredSnapshot() {
        config.set("anti-exploit.only-allow-kitroom-items", true);
        ItemFilter filter = new ItemFilter(plugin);
        ItemStack[] allowed = kit(Material.STONE);
        filter.addToWhitelist(java.util.Collections.singletonList(allowed));
        KitShareManager shares = new KitShareManager(plugin);
        ItemStack[] original = kit(Material.DIRT); original[1] = new ItemStack(Material.STONE);
        KitShareManager.kitShareMap.put("ABC123", original);
        shares.copyKit(player, "abc123");
        var applied = ArgumentCaptor.forClass(ItemStack[].class);
        verify(player.getInventory()).setContents(applied.capture());
        assertNull(applied.getValue()[0]);
        assertEquals(Material.STONE, applied.getValue()[1].getType());
        assertEquals(Material.DIRT, original[0].getType());
        applied.getValue()[1].setType(Material.GRAVEL);
        assertEquals(Material.STONE, original[1].getType());
    }
    @Test void shareCodeDoesNotEmptyAnInventoryWhileKitRoomWhitelistIsUnavailable() {
        config.set("anti-exploit.only-allow-kitroom-items", true);
        new ItemFilter(plugin);
        KitShareManager shares = new KitShareManager(plugin);
        KitShareManager.kitShareMap.put("ABC123", kit(Material.DIRT));
        shares.copyKit(player, "ABC123");
        verify(player.getInventory(), never()).setContents(any());
        assertNotNull(KitShareManager.kitShareMap.get("ABC123"));
    }
    @Test void directShareIsFilteredOnAcceptanceAndUnavailableFilterKeepsRequestPending() {
        ItemStack[] original = kit(Material.DIRT); original[1] = new ItemStack(Material.STONE);
        kits.savekit(uuid, 1, original, true);
        KitShareManager shares = new KitShareManager(plugin);
        Player target = mock(Player.class);
        when(target.getUniqueId()).thenReturn(UUID.randomUUID());
        PlayerInventory inventory = mock(PlayerInventory.class); when(target.getInventory()).thenReturn(inventory);
        shares.sendKitShareRequest(player, 1, target);
        String id = shares.getPendingRequestIds(target).get(0);
        config.set("anti-exploit.only-allow-kitroom-items", true);
        ItemFilter filter = new ItemFilter(plugin);
        shares.acceptRequest(target, id);
        assertEquals(List.of(id), shares.getPendingRequestIds(target));
        verify(inventory, never()).setContents(any());
        filter.addToWhitelist(java.util.Collections.singletonList(kit(Material.STONE)));
        shares.acceptRequest(target, id);
        var applied = ArgumentCaptor.forClass(ItemStack[].class);
        verify(inventory).setContents(applied.capture());
        assertNull(applied.getValue()[0]);
        assertEquals(Material.STONE, applied.getValue()[1].getType());
        assertTrue(shares.getPendingRequestIds(target).isEmpty());
        assertEquals(Material.DIRT, kits.getPlayerKit(uuid, 1)[0].getType());
    }
}
