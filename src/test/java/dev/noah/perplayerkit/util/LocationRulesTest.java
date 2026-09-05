package dev.noah.perplayerkit.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocationRulesTest {
    private LocationRules.Rule rule(boolean allow, String... entries) {
        return new LocationRules.Rule(allow, java.util.Arrays.stream(entries).map(LocationRules.Selector::parse).toList());
    }
    @Test void emptyListsHaveExplicitOppositeMeanings() {
        assertTrue(rule(false).permits("pvp", () -> { throw new AssertionError("No region query needed"); }));
        assertFalse(rule(true).permits("pvp", () -> { throw new AssertionError("No region query needed"); }));
    }
    @Test void eitherWorldOrRegionCanMatchWithoutCaseSensitivity() {
        var allow = rule(true, " Lobby ", "PVP: Arena ");
        assertTrue(allow.permits("lobby", List::of));
        assertTrue(allow.permits("pvp", () -> List.of("arena", "spawn")));
        assertFalse(allow.permits("survival", () -> List.of("arena")));
        assertFalse(allow.permits("pvp", () -> List.of("spawn")));
    }
    @Test void overlappingFilterRegionsUseAnyMatchRegardlessOfPriority() {
        var deny = rule(false, "pvp:outer");
        assertFalse(deny.permits("pvp", () -> List.of("inner", "outer")));
        assertFalse(deny.permits("pvp", () -> List.of("outer", "inner")));
    }
    @Test void worldMatchShortCircuitsRegionDependencyAndOtherWorldsDoNotQuery() {
        assertFalse(rule(false, "pvp:arena", "pvp").permits("pvp", () -> { throw new AssertionError(); }));
        assertTrue(rule(false, "pvp:arena").permits("survival", () -> { throw new AssertionError(); }));
    }
    @ParameterizedTest @ValueSource(strings = {"", "world:", ":arena", "a:b:c", "world:__global__"})
    void malformedSelectorsAreRejected(String selector) {
        assertThrows(IllegalArgumentException.class, () -> LocationRules.Selector.parse(selector));
    }
    @ParameterizedTest @ValueSource(strings = {
            "locations: []", "locations: {regeer: {mode: deny, entries: []}}",
            "locations: {regear: {mode: whitelist, entries: []}}", "locations: {regear: {mode: allow}}",
            "locations: {regear: {entries: []}}", "locations: {regear: {mode: deny, entries: arena}}",
            "locations: {regear: {mode: deny, entries: [123]}}", "locations: {regear: {mode: deny, entries: [], typo: 1}}"})
    void rejectsInvalidConfiguration(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration(); config.loadFromString(yaml);
        assertThrows(IllegalArgumentException.class, () -> LocationRules.parse(config));
    }
}
