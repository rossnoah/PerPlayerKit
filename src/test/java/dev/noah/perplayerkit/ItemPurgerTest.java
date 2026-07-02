package dev.noah.perplayerkit;

import org.bukkit.Material;
import org.bukkit.block.Container;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ItemPurgerTest {

    private ItemStack mockItem(Material type, int amount) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(type);
        when(item.getAmount()).thenReturn(amount);
        return item;
    }

    @Test
    void removesMatchingTopLevelItemsAndCountsAmounts() {
        ItemStack[] contents = {
                mockItem(Material.TNT, 64),
                mockItem(Material.DIAMOND_SWORD, 1),
                null,
                mockItem(Material.TNT, 3),
        };

        int removed = ItemPurger.purgeContents(contents, Material.TNT);

        assertEquals(67, removed);
        assertNull(contents[0]);
        assertNotNull(contents[1]);
        assertNull(contents[3]);
    }

    @Test
    void leavesNonMatchingContentsUntouched() {
        ItemStack keeper = mockItem(Material.DIAMOND_SWORD, 1);
        ItemStack[] contents = {keeper, null};

        int removed = ItemPurger.purgeContents(contents, Material.TNT);

        assertEquals(0, removed);
        assertEquals(keeper, contents[0]);
    }

    @Test
    void purgesInsideShulkerBoxContents() {
        ItemStack[] inner = {mockItem(Material.TNT, 10), mockItem(Material.APPLE, 5)};

        Inventory innerInventory = mock(Inventory.class);
        when(innerInventory.getContents()).thenReturn(inner);
        Container container = mock(Container.class);
        when(container.getInventory()).thenReturn(innerInventory);
        BlockStateMeta meta = mock(BlockStateMeta.class);
        when(meta.getBlockState()).thenReturn(container);

        ItemStack shulker = mockItem(Material.RED_SHULKER_BOX, 1);
        when(shulker.getItemMeta()).thenReturn(meta);

        ItemStack[] contents = {shulker};
        int removed = ItemPurger.purgeContents(contents, Material.TNT);

        assertEquals(10, removed);
        assertNotNull(contents[0]);
        assertNull(inner[0]);
        verify(innerInventory).setContents(inner);
        verify(meta).setBlockState(container);
        verify(shulker).setItemMeta(meta);
    }

    @Test
    void doesNotRewriteContainerWithoutMatches() {
        ItemStack[] inner = {mockItem(Material.APPLE, 5)};

        Inventory innerInventory = mock(Inventory.class);
        when(innerInventory.getContents()).thenReturn(inner);
        Container container = mock(Container.class);
        when(container.getInventory()).thenReturn(innerInventory);
        BlockStateMeta meta = mock(BlockStateMeta.class);
        when(meta.getBlockState()).thenReturn(container);

        ItemStack shulker = mockItem(Material.SHULKER_BOX, 1);
        when(shulker.getItemMeta()).thenReturn(meta);

        int removed = ItemPurger.purgeContents(new ItemStack[]{shulker}, Material.TNT);

        assertEquals(0, removed);
        verify(innerInventory, never()).setContents(inner);
        verify(shulker, never()).setItemMeta(meta);
    }

    @Test
    void removesMatchingShulkerBoxItself() {
        ItemStack shulker = mockItem(Material.RED_SHULKER_BOX, 1);
        ItemStack[] contents = {shulker};

        int removed = ItemPurger.purgeContents(contents, Material.RED_SHULKER_BOX);

        assertEquals(1, removed);
        assertNull(contents[0]);
    }

    @Test
    void purgesInsideBundles() {
        ItemStack bundledTnt = mockItem(Material.TNT, 7);
        ItemStack bundledApple = mockItem(Material.APPLE, 2);

        BundleMeta meta = mock(BundleMeta.class);
        List<ItemStack> bundleItems = new ArrayList<>(List.of(bundledTnt, bundledApple));
        when(meta.getItems()).thenReturn(bundleItems);

        ItemStack bundle = mockItem(Material.BUNDLE, 1);
        when(bundle.getItemMeta()).thenReturn(meta);

        int removed = ItemPurger.purgeContents(new ItemStack[]{bundle}, Material.TNT);

        assertEquals(7, removed);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ItemStack>> captor = ArgumentCaptor.forClass(List.class);
        verify(meta).setItems(captor.capture());
        assertEquals(List.of(bundledApple), captor.getValue());
        verify(bundle).setItemMeta(meta);
    }

    @Test
    void purgesContainersNestedInsideBundles() {
        ItemStack[] inner = {mockItem(Material.TNT, 4)};
        Inventory innerInventory = mock(Inventory.class);
        when(innerInventory.getContents()).thenReturn(inner);
        Container container = mock(Container.class);
        when(container.getInventory()).thenReturn(innerInventory);
        BlockStateMeta shulkerMeta = mock(BlockStateMeta.class);
        when(shulkerMeta.getBlockState()).thenReturn(container);

        ItemStack nestedShulker = mockItem(Material.BLUE_SHULKER_BOX, 1);
        when(nestedShulker.getItemMeta()).thenReturn(shulkerMeta);

        BundleMeta bundleMeta = mock(BundleMeta.class);
        when(bundleMeta.getItems()).thenReturn(List.of(nestedShulker));

        ItemStack bundle = mockItem(Material.BUNDLE, 1);
        when(bundle.getItemMeta()).thenReturn(bundleMeta);

        int removed = ItemPurger.purgeContents(new ItemStack[]{bundle}, Material.TNT);

        assertEquals(4, removed);
        assertNull(inner[0]);
        verify(innerInventory).setContents(inner);
    }

    @Test
    void isEmptyDetectsEmptyAndNonEmptyArrays() {
        assertTrue(ItemPurger.isEmpty(null));
        assertTrue(ItemPurger.isEmpty(new ItemStack[]{null, null}));
        assertTrue(ItemPurger.isEmpty(new ItemStack[]{mockItem(Material.AIR, 1)}));
        assertFalse(ItemPurger.isEmpty(new ItemStack[]{null, mockItem(Material.STONE, 1)}));
    }
}
