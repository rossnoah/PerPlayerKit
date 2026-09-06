package ppk.featureprobe;

import dev.noah.perplayerkit.*;
import dev.noah.perplayerkit.gui.GUI;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import java.util.*;

/** Real Bukkit metadata and menu events, driven by a connected protocol client. */
public final class FeatureProbe extends JavaPlugin implements Listener {
    private org.bukkit.plugin.Plugin ppk;
    @Override public void onEnable() {
        ppk = getServer().getPluginManager().getPlugin("PerPlayerKit");
        try { filterChecks(); getServer().getPluginManager().registerEvents(this, this); getLogger().info("PPK_FEATURE_READY"); }
        catch (Throwable error) { fail(error); }
    }
    private ItemFilter rules(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration(); config.loadFromString("item-filter:\n  enabled: true\n" + yaml);
        ppk.getConfig().set("item-filter", config.getConfigurationSection("item-filter"));
        return new ItemFilter(ppk);
    }
    private void filterChecks() throws Exception {
        Enchantment protection = Objects.requireNonNull(Enchantment.getByKey(NamespacedKey.minecraft("protection")));
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET); helmet.addUnsafeEnchantment(protection, 10);
        check(rules("").filterItemStack(new ItemStack[]{helmet})[0] == null, "Protection X was accepted");
        check(helmet.getEnchantmentLevel(protection) == 10, "Source enchantments mutated");
        check(rules("  overrides:\n    DIAMOND_HELMET:\n      enchantments:\n        max-levels: {protection: 10}\n")
                .filterItemStack(new ItemStack[]{helmet})[0] != null, "Material override not applied");
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK); EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        bookMeta.addStoredEnchant(protection, 10, true); book.setItemMeta(bookMeta);
        check(rules("").filterItemStack(new ItemStack[]{book})[0] == null, "Stored book enchantment bypassed limits");
        bookMeta.removeStoredEnchant(protection); bookMeta.addStoredEnchant(protection, 4, true); book.setItemMeta(bookMeta);
        check(rules("").filterItemStack(new ItemStack[]{book})[0] != null, "Valid enchanted book was rejected");

        ItemStack sword = new ItemStack(Material.NETHERITE_SWORD); ItemMeta swordMeta = sword.getItemMeta();
        swordMeta.setUnbreakable(true); sword.setItemMeta(swordMeta);
        check(rules("").filterItemStack(new ItemStack[]{sword})[0] == null, "Unbreakable item accepted");
        check(rules("  allow-unbreakable: true\n").filterItemStack(new ItemStack[]{sword})[0] != null, "Unbreakable toggle ignored");
        ItemStack box = new ItemStack(Material.SHULKER_BOX); BlockStateMeta boxMeta = (BlockStateMeta) box.getItemMeta();
        ShulkerBox contents = (ShulkerBox) boxMeta.getBlockState(); contents.getInventory().setItem(0, sword);
        contents.getInventory().setItem(1, new ItemStack(Material.STONE)); boxMeta.setBlockState(contents); box.setItemMeta(boxMeta);
        ItemStack filteredBox = rules("").filterItemStack(new ItemStack[]{box})[0];
        ShulkerBox filtered = (ShulkerBox) ((BlockStateMeta) filteredBox.getItemMeta()).getBlockState();
        check(filtered.getInventory().getItem(0) == null && filtered.getInventory().getItem(1).getType() == Material.STONE, "Nested filtering failed");
        check(((ShulkerBox) ((BlockStateMeta) box.getItemMeta()).getBlockState()).getInventory().getItem(0).getItemMeta().isUnbreakable(), "Nested source was mutated");
        ItemStack bundle = new ItemStack(Material.BUNDLE);
        if (bundle.getItemMeta() instanceof BundleMeta bundleMeta) {
            bundleMeta.addItem(sword); bundleMeta.addItem(new ItemStack(Material.STONE)); bundle.setItemMeta(bundleMeta);
            var result = rules("").filterItemStack(new ItemStack[]{bundle})[0];
            check(((BundleMeta) result.getItemMeta()).getItems().size() == 1, "Bundle bypassed rules");
            check(((BundleMeta) bundle.getItemMeta()).getItems().size() == 2, "Source bundle mutated");
        }

        ItemStack potion = new ItemStack(Material.POTION); PotionMeta meta = (PotionMeta) potion.getItemMeta();
        setStrongStrength(meta); potion.setItemMeta(meta);
        check(rules("  potions: {max-level: 1}\n").filterItemStack(new ItemStack[]{potion})[0] == null, "Base potion level was not checked");
        check(rules("  potions: {max-level: 2}\n").filterItemStack(new ItemStack[]{potion})[0] != null, "Valid base potion was rejected");
        potion = new ItemStack(Material.POTION); meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(Objects.requireNonNull(PotionEffectType.getByKey(NamespacedKey.minecraft("speed"))), 1201, 1), true);
        potion.setItemMeta(meta);
        check(rules("  potions: {max-duration-seconds: 60}\n").filterItemStack(new ItemStack[]{potion})[0] == null, "Custom potion duration was not checked");
        check(((PotionMeta) potion.getItemMeta()).getCustomEffects().get(0).getDuration() == 1201, "Source potion mutated");
        rules("");
    }
    private void setStrongStrength(PotionMeta meta) throws Exception {
        try {
            PotionMeta.class.getMethod("setBasePotionType", PotionType.class).invoke(meta, PotionType.valueOf("STRONG_STRENGTH"));
        } catch (NoSuchMethodException oldApi) {
            Class<?> data = Class.forName("org.bukkit.potion.PotionData");
            Object value = data.getConstructor(PotionType.class, boolean.class, boolean.class).newInstance(PotionType.valueOf("STRENGTH"), false, true);
            PotionMeta.class.getMethod("setBasePotionData", data).invoke(meta, value);
        }
    }
    @EventHandler public void join(PlayerJoinEvent event) {
        Player player = event.getPlayer(); player.setOp(true);
        getServer().getScheduler().runTaskLater(this, () -> {
            try {
                directPageChecks(player);
                KitRoomDataManager room = KitRoomDataManager.get(); check(room.getPageCount() == 8, "Wrong configured page count");
                for (int page = 0; page < 8; page++) {
                    ItemStack[] items = new ItemStack[45]; items[0] = new ItemStack(Material.STONE, page + 1); room.setKitRoom(page, items);
                }
                GUI gui = new GUI(ppk); gui.OpenKitRoom(player, 0);
                check(player.getOpenInventory().getTopInventory().getItem(52).getType() == Material.ARROW, "Missing next arrow");
                click(player, 52, ClickType.LEFT);
                check(player.getOpenInventory().getTopInventory().getItem(0).getAmount() == 6, "Next group opened wrong page");
                click(player, 49, ClickType.LEFT);
                check(player.getOpenInventory().getTopInventory().getItem(0).getAmount() == 8, "Page 8 button opened wrong page");
                player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.DIRT));
                click(player, 53, ClickType.SHIFT_RIGHT);
                check(room.getKitRoomPage(7)[0].getType() == Material.DIRT, "Saved wrong page");
                click(player, 46, ClickType.LEFT);
                check(player.getOpenInventory().getTopInventory().getItem(0).getAmount() == 5, "Previous group opened wrong page");
                var denied = player.addAttachment(this, "perplayerkit.editkitroom", false);
                InventoryClickEvent blocked = click(player, 0, ClickType.LEFT);
                check(blocked.isCancelled(), "Revoked editor permission left raw items accessible");
                player.closeInventory();
                ItemStack unsafe = new ItemStack(Material.NETHERITE_SWORD); ItemMeta unsafeMeta = unsafe.getItemMeta();
                unsafeMeta.setUnbreakable(true); unsafe.setItemMeta(unsafeMeta);
                ItemStack[] catalog = new ItemStack[45]; catalog[0] = unsafe; catalog[1] = new ItemStack(Material.STONE);
                room.setKitRoom(0, catalog); gui.OpenKitRoom(player, 0);
                check(player.getOpenInventory().getTopInventory().getItem(0) == null, "Normal kit room view exposed a rejected item");
                check(room.getKitRoomPage(0)[0].getItemMeta().isUnbreakable(), "Viewing the kit room changed its definition");
                player.closeInventory(); denied.remove();
                player.getInventory().setContents(new ItemStack[41]); player.getInventory().setItem(0, unsafe);
                gui.OpenKitMenu(player, 1);
                player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.STONE));
                click(player, dev.noah.perplayerkit.gui.GuiLayoutUtils.IMPORT_SLOT, ClickType.LEFT);
                check(player.getOpenInventory().getTopInventory().getItem(0).getType() == Material.STONE, "A rejected import cleared the editor");
                player.closeInventory();
                check(KitManager.get().getPlayerKit(player.getUniqueId(), 1)[0].getType() == Material.STONE, "Valid editor content was not saved");
                KitManager.get().storageWork().run(() -> {}).join(); room.loadFromDB();
                check(room.getKitRoomPage(7)[0].getType() == Material.DIRT, "Extra page did not persist");
                player.sendMessage("PPK_FEATURE_CLIENT_PASS"); getLogger().info("PPK_FEATURE_PASS");
            } catch (Throwable error) { fail(error); }
        }, 20);
    }
    private void directPageChecks(Player player) {
        GUI gui = new GUI(ppk);
        for (int count : new int[]{6, 7}) {
            ppk.getConfig().set("kitroom.pages", count);
            KitRoomDataManager room = new KitRoomDataManager(ppk);
            for (int page = 0; page < count; page++) {
                ItemStack[] items = new ItemStack[45]; items[0] = new ItemStack(Material.STONE, page + 1);
                room.setKitRoom(page, items);
            }
            gui.OpenKitRoom(player, 0);
            for (int page = 0; page < count; page++) {
                click(player, 46 + page, ClickType.LEFT);
                Inventory top = player.getOpenInventory().getTopInventory();
                check(top.getItem(0).getAmount() == page + 1, "Direct button opened wrong page with " + count + " pages");
                check(top.getItem(46 + page).getItemMeta().hasEnchants(), "Wrong direct button highlighted");
                for (int slot = 46; slot <= 52; slot++) {
                    check(top.getItem(slot).getType() != Material.ARROW, "Unnecessary arrow with " + count + " pages");
                }
                click(player, 45, ClickType.LEFT);
                check(player.getOpenInventory().getTopInventory().getItem(0).getAmount() == page + 1, "Refill switched pages");
            }
            player.getOpenInventory().getTopInventory().setItem(0, new ItemStack(Material.DIRT));
            click(player, 53, ClickType.SHIFT_RIGHT);
            check(room.getKitRoomPage(count - 1)[0].getType() == Material.DIRT, "Direct page save used wrong page");
            click(player, 46, ClickType.LEFT);
            check(player.getOpenInventory().getTopInventory().getItem(0).getAmount() == 1, "First direct button did not return to page 1");
            player.closeInventory();
        }
        ppk.getConfig().set("kitroom.pages", 8);
        new KitRoomDataManager(ppk);
    }
    private InventoryClickEvent click(Player player, int slot, ClickType click) {
        InventoryClickEvent event = new InventoryClickEvent(player.getOpenInventory(), InventoryType.SlotType.CONTAINER, slot, click,
                click.isShiftClick() ? InventoryAction.MOVE_TO_OTHER_INVENTORY : InventoryAction.PICKUP_ALL);
        getServer().getPluginManager().callEvent(event); return event;
    }
    private static void check(boolean valid, String message) { if (!valid) throw new AssertionError(message); }
    private void fail(Throwable error) { error.printStackTrace(); getLogger().severe("PPK_FEATURE_FAIL " + error); }
}
