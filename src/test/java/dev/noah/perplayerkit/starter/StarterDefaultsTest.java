package dev.noah.perplayerkit.starter;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the hand-written starter content in {@code src/main/resources/defaults}.
 * Every definition has to parse, land in a real slot exactly once, and name a
 * material and potion this Minecraft version actually has.
 */
class StarterDefaultsTest {

    private final List<String> warnings = new ArrayList<>();

    private YamlConfiguration load(String resource) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(in, resource + " is missing from the build output");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return fail("Could not read " + resource, e);
        }
    }

    private void assertNoWarnings() {
        assertTrue(warnings.isEmpty(), () -> "defaults did not parse cleanly: " + warnings);
    }

    @Test
    void kitRoomDefaultsAreLaidOutSanely() {
        List<StarterDefaults.Section> pages =
                StarterDefaults.parseKitRoomPages(load(StarterDefaults.KIT_ROOM_RESOURCE), warnings::add);
        assertNoWarnings();

        assertEquals(5, pages.size(), "the kit room menu has five category buttons");
        for (StarterDefaults.Section page : pages) {
            assertSectionIsSane(page, StarterDefaults.KIT_ROOM_PAGE_SIZE, true);
            // Pages leave gaps between groups on purpose. What would be a bug
            // is a page that has quietly gone half empty because a slot key
            // stopped matching anything.
            int used = usedSlots(page).size();
            assertTrue(used >= 24 && used <= StarterDefaults.KIT_ROOM_PAGE_SIZE,
                    page.label() + " fills " + used + " of " + StarterDefaults.KIT_ROOM_PAGE_SIZE + " slots");
        }
    }

    @Test
    void publicKitDefaultsCoverTheConfiguredKits() {
        Map<String, StarterDefaults.Section> kits =
                StarterDefaults.parsePublicKits(load(StarterDefaults.PUBLIC_KITS_RESOURCE), warnings::add);
        assertNoWarnings();

        assertEquals(Set.of("crystal", "mace", "netherite", "pot", "uhc", "sword", "axe", "ffa", "cart"),
                kits.keySet());
        for (Map.Entry<String, StarterDefaults.Section> entry : kits.entrySet()) {
            assertSectionIsSane(entry.getValue(), StarterDefaults.KIT_SIZE, isOfferedHere(entry.getKey()));
        }
    }

    /**
     * A kit is only listed when the icon config.yml gives it exists on the
     * running server, so a kit built out of gear this API has never heard of -
     * the mace kit on anything older than 1.21 - is never seeded either. Those
     * are the kits allowed to name materials that do not resolve here.
     */
    private boolean isOfferedHere(String id) {
        String icon = load("config.yml").getString("publickits." + id + ".icon", "");
        return Material.matchMaterial(icon) != null;
    }

    /**
     * A player should be able to rebuild any shipped public kit by hand, so
     * every material and potion a kit uses has to be somewhere in the kit room -
     * loose on a page or inside one of its shulkers.
     */
    @Test
    void everyPublicKitItemIsStockedInTheKitRoom() {
        Map<String, StarterDefaults.Section> kits =
                StarterDefaults.parsePublicKits(load(StarterDefaults.PUBLIC_KITS_RESOURCE), warnings::add);
        List<StarterDefaults.Section> pages =
                StarterDefaults.parseKitRoomPages(load(StarterDefaults.KIT_ROOM_RESOURCE), warnings::add);
        assertNoWarnings();

        Set<String> stocked = new HashSet<>();
        pages.forEach(page -> collectItems(page.items(), stocked));

        kits.forEach((id, kit) -> {
            Set<String> needed = new HashSet<>();
            collectItems(kit.items(), needed);
            needed.removeAll(stocked);
            assertTrue(needed.isEmpty(), () -> "public kit " + id + " uses " + needed
                    + ", which the kit room does not stock");
        });
    }

    /**
     * Every item a section can hand out, as "MATERIAL" or "MATERIAL/POTION".
     * Both sides of a fallback chain count, since the same chain decides what
     * the kit and the kit room end up with on any given server.
     */
    private void collectItems(List<StarterDefaults.SlotItem> items, Set<String> into) {
        for (StarterDefaults.SlotItem slotItem : items) {
            for (ItemSpec spec : slotItem.options()) {
                into.add(spec.getPotionType() == null
                        ? spec.getMaterial()
                        : spec.getMaterial() + "/" + spec.getPotionType());
                spec.getContents().values().forEach(nested -> into.add(nested.getPotionType() == null
                        ? nested.getMaterial()
                        : nested.getMaterial() + "/" + nested.getPotionType()));
            }
        }
    }

    /** The shipped config.yml must list exactly the public kits we have contents for. */
    @Test
    void publicKitIdsMatchTheShippedConfig() {
        Map<String, StarterDefaults.Section> kits =
                StarterDefaults.parsePublicKits(load(StarterDefaults.PUBLIC_KITS_RESOURCE), warnings::add);
        YamlConfiguration config = load("config.yml");

        assertEquals(kits.keySet(), config.getConfigurationSection("publickits").getKeys(false));
    }

    private void assertSectionIsSane(StarterDefaults.Section section, int size, boolean requireAvailable) {
        Set<Integer> used = new HashSet<>();

        for (StarterDefaults.SlotItem slotItem : section.items()) {
            assertOptionsAreSane(section.label(), slotItem.options(), requireAvailable);

            for (int slot : slotItem.slots()) {
                assertTrue(slot >= 0 && slot < size, section.label() + " uses out of range slot " + slot);
                assertTrue(used.add(slot), section.label() + " assigns slot " + slot + " twice");
            }
        }
    }

    /**
     * A slot may ask for gear newer than the API we build against, as long as
     * something behind the {@code ||} still lands. Options this API does not
     * know are skipped the same way a live server skips them; what is checked
     * is that every option is well formed, that a newer item always has a
     * fallback behind it, and that at least one option exists all the way down
     * at the oldest supported version - unless the whole section is one this
     * version never offers in the first place.
     */
    private void assertOptionsAreSane(String label, List<ItemSpec> options, boolean requireAvailable) {
        boolean anyAvailable = false;

        for (ItemSpec spec : options) {
            spec.getEnchantments().forEach((id, level) -> {
                assertTrue(id.matches("[a-z0-9_]+"), label + " has odd enchantment id " + id);
                assertTrue(level >= 1, label + " has level " + level + " for " + id);
            });

            Material material = Material.matchMaterial(spec.getMaterial());
            if (material == null) {
                assertTrue(options.size() > 1 || !requireAvailable,
                        label + " uses " + spec.getMaterial() + " with nothing to fall back to");
                continue;
            }

            assertFalse(material.isAir(), label + " uses " + spec.getMaterial());
            assertTrue(spec.getAmount() >= 1 && spec.getAmount() <= material.getMaxStackSize(),
                    label + " asks for " + spec.getAmount() + " " + spec.getMaterial()
                            + " but the stack limit is " + material.getMaxStackSize());

            if (spec.getPotionType() != null) {
                assertNotNull(PotionCompat.legacyType(spec.getPotionType()),
                        label + " uses unknown potion " + spec.getPotionType());
            }
            anyAvailable = true;
        }

        assertTrue(anyAvailable || !requireAvailable,
                label + " has no option that exists on the oldest supported version");
    }

    private Set<Integer> usedSlots(StarterDefaults.Section section) {
        Set<Integer> slots = new HashSet<>();
        section.items().forEach(item -> slots.addAll(item.slots()));
        return slots;
    }
}
