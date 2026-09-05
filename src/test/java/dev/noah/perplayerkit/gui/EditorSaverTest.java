package dev.noah.perplayerkit.gui;

import dev.noah.perplayerkit.ItemFilter;
import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.LocationAccess;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.*;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EditorSaverTest {
    private final UUID uuid = UUID.randomUUID();
    private Player player;
    private KitManager kits;
    private Lang messages;
    private Inventory inventory;
    private ItemStack[] view;
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<KitManager> kitManager;
    private MockedStatic<Lang> language;
    private MockedStatic<GUI> gui;
    private MockedStatic<SoundManager> sounds;

    @BeforeEach void setup() {
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.hasPermission(anyString())).thenReturn(true);
        kits = mock(KitManager.class);
        messages = mock(Lang.class);
        inventory = mock(Inventory.class);
        view = new ItemStack[54];
        when(inventory.getContents()).thenReturn(view);
        ItemFactory factory = mock(ItemFactory.class);
        when(factory.equals(any(), any())).thenAnswer(i -> Objects.equals(i.getArgument(0), i.getArgument(1)));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
        kitManager = mockStatic(KitManager.class);
        kitManager.when(KitManager::get).thenReturn(kits);
        language = mockStatic(Lang.class);
        language.when(Lang::get).thenReturn(messages);
        gui = mockStatic(GUI.class);
        sounds = mockStatic(SoundManager.class);
        Plugin plugin = mock(Plugin.class);
        when(plugin.getConfig()).thenReturn(new YamlConfiguration());
        new ItemFilter(plugin);
        new LocationAccess(plugin);
    }

    @AfterEach void close() {
        sounds.close(); gui.close(); language.close(); kitManager.close(); bukkit.close();
    }

    private GUI.EditorContext context(GUI.EditorType type) {
        return new GUI.EditorContext(type, 3, null, UUID.randomUUID(), "Target");
    }

    private void existingPersonalKit(GUI.EditorType type) {
        ItemStack[] existing = new ItemStack[type == GUI.EditorType.KIT ? 41 : 27];
        existing[0] = new ItemStack(Material.STONE);
        when(kits.getPlayerKit(uuid, 3)).thenReturn(existing);
        when(kits.getPlayerEC(uuid, 3)).thenReturn(existing);
        view[type == GUI.EditorType.KIT ? 0 : 9] = existing[0].clone();
    }

    @ParameterizedTest @EnumSource(value=GUI.EditorType.class, names={"INSPECT_KIT", "INSPECT_ENDERCHEST"})
    void closingReadOnlyInspectionDoesNotAttemptASaveOrSendPermissionErrors(GUI.EditorType type) {
        when(player.hasPermission("perplayerkit.admin")).thenReturn(false);
        EditorSaver.save(player, context(type), inventory);
        verifyNoInteractions(kits, messages);
        sounds.verifyNoInteractions();
    }

    @ParameterizedTest @EnumSource(value=GUI.EditorType.class, names={"KIT", "ENDERCHEST"})
    void unchangedEditorDoesNotWriteOrDelete(GUI.EditorType type) {
        existingPersonalKit(type);
        EditorSaver.save(player, context(type), inventory);
        verify(kits, never()).savekit(any(), anyInt(), any());
        verify(kits, never()).saveEC(any(), anyInt(), any());
        verify(kits, never()).deleteKit(any(), anyInt());
        verify(kits, never()).deleteEnderchest(any(), anyInt());
    }

    @ParameterizedTest @EnumSource(value=GUI.EditorType.class, names={"KIT", "ENDERCHEST"})
    void clearingAnExistingPersonalEditorDeletesTheCorrectSlot(GUI.EditorType type) {
        existingPersonalKit(type);
        view[type == GUI.EditorType.KIT ? 0 : 9] = null;
        gui.when(() -> GUI.takeClearFlag(player)).thenReturn(true);
        EditorSaver.save(player, context(type), inventory);
        if (type == GUI.EditorType.KIT) verify(kits).deleteKit(uuid, 3);
        else verify(kits).deleteEnderchest(uuid, 3);
        verify(kits, never()).savekit(any(), anyInt(), any());
        verify(kits, never()).saveEC(any(), anyInt(), any());
    }

    @ParameterizedTest @EnumSource(value=GUI.EditorType.class, names={"KIT", "ENDERCHEST"})
    void changedEditorSavesOnlyOwnedCopiesOfEditableSlots(GUI.EditorType type) {
        existingPersonalKit(type);
        int first = type == GUI.EditorType.KIT ? 0 : 9;
        view[first] = new ItemStack(Material.DIRT);
        view[53] = new ItemStack(Material.BARRIER);
        EditorSaver.save(player, context(type), inventory);
        ArgumentCaptor<ItemStack[]> contents = ArgumentCaptor.forClass(ItemStack[].class);
        if (type == GUI.EditorType.KIT) verify(kits).savekit(eq(uuid), eq(3), contents.capture());
        else verify(kits).saveEC(eq(uuid), eq(3), contents.capture());
        assertEquals(type == GUI.EditorType.KIT ? 41 : 27, contents.getValue().length);
        assertEquals(Material.DIRT, contents.getValue()[0].getType());
        assertNotSame(view[first], contents.getValue()[0]);
    }

    @Test void revokedPermissionPreventsSavingAnAlreadyOpenEditor() {
        when(player.hasPermission("perplayerkit.kit")).thenReturn(false);
        view[0] = new ItemStack(Material.STONE);
        EditorSaver.save(player, context(GUI.EditorType.KIT), inventory);
        verifyNoInteractions(kits);
        gui.verify(() -> GUI.takeClearFlag(player));
    }

    @Test void invalidPublicEditorCannotLeakItsClearFlagIntoTheNextEditor() {
        EditorSaver.save(player, context(GUI.EditorType.PUBLIC_KIT), inventory);
        gui.verify(() -> GUI.takeClearFlag(player));
        verifyNoInteractions(kits);
    }
}
