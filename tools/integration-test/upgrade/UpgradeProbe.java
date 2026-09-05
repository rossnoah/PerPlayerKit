package ppk.upgradeprobe;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.*;
import java.util.*;

/** Test-only companion plugin, compiled against the stable API shared by the historical builds. */
public final class UpgradeProbe extends JavaPlugin {
    private static final UUID PLAYER = UUID.fromString("74e6c4e6-f888-4a78-b411-a5a98763902c");
    private static final String PERSONAL = PLAYER + "1";
    private final Path fixtureFile = Path.of("upgrade-fixture.yml");

    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                String phase = Files.readString(Path.of("upgrade-phase.txt")).trim();
                if (phase.equals("seed")) seed();
                else {
                    verify();
                    if (phase.equals("write")) saveWithNewApi();
                }
                getLogger().info("PPK_UPGRADE_PASS " + phase);
            } catch (Throwable error) {
                getLogger().severe("PPK_UPGRADE_FAIL " + error);
                error.printStackTrace();
            }
        });
    }

    private ItemStack sword(String name) {
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
        sword.addEnchantment(Objects.requireNonNull(Enchantment.getByKey(NamespacedKey.minecraft("sharpness"))), 3);
        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(List.of("Owner fixture", "Keep this text through upgrade and rollback"));
        sword.setItemMeta(meta);
        return sword;
    }

    private void seed() throws Exception {
        check(PerPlayerKit.storageManager.getAllKitIDs().isEmpty(), "fixture needs an empty, dedicated test database");
        Map<String, ItemStack[]> kits = new LinkedHashMap<>();
        ItemStack[] personal = new ItemStack[41];
        personal[0] = sword("Original player kit");
        personal[9] = new ItemStack(Material.STONE, 32);
        personal[36] = new ItemStack(Material.DIAMOND_BOOTS);
        personal[40] = new ItemStack(Material.TOTEM_OF_UNDYING);
        kits.put(PERSONAL, personal);
        ItemStack[] highSlot = new ItemStack[41];
        highSlot[0] = new ItemStack(Material.ARROW, 48);
        kits.put(PLAYER + "13", highSlot);
        ItemStack[] ec = new ItemStack[27];
        ItemStack box = new ItemStack(Material.WHITE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) box.getItemMeta();
        ShulkerBox state = (ShulkerBox) meta.getBlockState();
        state.getInventory().setItem(0, sword("Nested sword"));
        meta.setBlockState(state);
        box.setItemMeta(meta);
        ec[0] = box;
        kits.put(PLAYER + "ec1", ec);
        ItemStack[] publicKit = new ItemStack[41];
        publicKit[0] = sword("Custom public kit");
        kits.put("publiccustom", publicKit);
        ItemStack[] room = new ItemStack[45];
        room[0] = sword("Kit room sword");
        room[1] = new ItemStack(Material.STONE, 64);
        kits.put("kitroom0", room);
        kits.put("kitroom1", new ItemStack[45]); // Deliberately saved empty, not unconfigured.

        YamlConfiguration fixture = new YamlConfiguration();
        for (Map.Entry<String, ItemStack[]> entry : kits.entrySet()) {
            String encoded = Serializer.itemStackArrayToBase64(entry.getValue());
            PerPlayerKit.storageManager.saveKitDataByID(entry.getKey(), encoded);
            fixture.set("kits." + entry.getKey(), encoded);
        }
        notifications().forEach((key, value) -> fixture.set("notifications." + key, value));
        fixture.save(fixtureFile.toFile());
        verify();
    }

    private void verify() throws Exception {
        YamlConfiguration fixture = YamlConfiguration.loadConfiguration(fixtureFile.toFile());
        Set<String> expected = fixture.getConfigurationSection("kits").getKeys(false);
        check(PerPlayerKit.storageManager.getAllKitIDs().equals(expected), "stored kit IDs changed");
        for (String id : expected) {
            String actual = PerPlayerKit.storageManager.getKitDataByID(id);
            check(Objects.equals(fixture.getString("kits." + id), actual), "stored contents changed: " + id);
            ItemStack[] items = Serializer.itemStackArrayFromBase64(actual);
            check(items.length == (id.contains("ec") ? 27 : id.startsWith("kitroom") ? 45 : 41), "slot layout changed: " + id);
        }
        ItemStack[] contents = Serializer.itemStackArrayFromBase64(PerPlayerKit.storageManager.getKitDataByID(PERSONAL));
        check(contents[0].getEnchantmentLevel(Enchantment.getByKey(NamespacedKey.minecraft("sharpness"))) == 3, "enchantment lost");
        ItemStack[] ec = Serializer.itemStackArrayFromBase64(PerPlayerKit.storageManager.getKitDataByID(PLAYER + "ec1"));
        ShulkerBox box = (ShulkerBox) ((BlockStateMeta) ec[0].getItemMeta()).getBlockState();
        check(box.getInventory().getItem(0).getItemMeta().getDisplayName().equals("Nested sword"), "nested metadata lost");
        notifications().forEach((key, value) -> check(value == fixture.getBoolean("notifications." + key), "notification audience changed: " + key));
        KitManager.get().loadPlayerDataFromDB(PLAYER);
        check(KitManager.get().getPlayerKit(PLAYER, 1)[0].getItemMeta().getDisplayName().equals(contents[0].getItemMeta().getDisplayName()), "cached kit could not load");
    }

    private void saveWithNewApi() throws Exception {
        ItemStack[] kit = new ItemStack[41];
        kit[0] = sword("Saved by new version");
        check(KitManager.get().savekit(PLAYER, 1, kit, true), "new save API rejected fixture");
        YamlConfiguration fixture = YamlConfiguration.loadConfiguration(fixtureFile.toFile());
        fixture.set("kits." + PERSONAL, Serializer.itemStackArrayToBase64(kit));
        fixture.save(fixtureFile.toFile());
        // The harness stops the server next. Shutdown must drain this accepted save.
    }

    private Map<String, Boolean> notifications() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (boolean op : List.of(false, true)) {
            for (String role : List.of("none", "use", "grant", "deny")) {
                PermissibleBase permissions = new PermissibleBase(new ServerOperator() {
                    public boolean isOp() { return op; }
                    public void setOp(boolean value) { throw new UnsupportedOperationException(); }
                });
                if (!role.equals("none")) permissions.addAttachment(this, "perplayerkit.use", true);
                if (role.equals("grant")) permissions.addAttachment(this, "perplayerkit.kitnotify", true);
                if (role.equals("deny")) permissions.addAttachment(this, "perplayerkit.kitnotify", false);
                result.put((op ? "op-" : "player-") + role, permissions.hasPermission("perplayerkit.kitnotify"));
                permissions.clearPermissions();
            }
        }
        return result;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
