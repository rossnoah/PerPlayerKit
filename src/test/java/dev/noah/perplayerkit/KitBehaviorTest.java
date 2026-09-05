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

}
