package dev.noah.perplayerkit.util;

import java.util.Arrays;

/** Independently configurable gameplay actions. Admin data tools have no location restriction. */
public enum LocationFeature {
    GLOBAL("global"), MENU("menu"), KITS("kits"), ENDERCHESTS("enderchests"),
    PUBLIC_KITS("public-kits"), KIT_ROOM("kit-room"), SHARING("sharing"),
    REGEAR("regear"), HEAL("heal"), REPAIR("repair"),
    REKIT_RESPAWN("rekit-respawn"), REKIT_KILL("rekit-kill");

    private final String key;
    LocationFeature(String key) { this.key = key; }
    public String key() { return key; }
    public static LocationFeature fromKey(String key) {
        return Arrays.stream(values()).filter(feature -> feature.key.equals(key)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown location feature: " + key));
    }

    public static LocationFeature forPermission(String permission) {
        return switch (permission) {
            case "perplayerkit.menu" -> MENU;
            case "perplayerkit.kit", "perplayerkit.deletekit", "perplayerkit.swapkit" -> KITS;
            case "perplayerkit.enderchest", "perplayerkit.deleteenderchest" -> ENDERCHESTS;
            case "perplayerkit.publickit" -> PUBLIC_KITS;
            case "perplayerkit.editkitroom" -> KIT_ROOM;
            case "perplayerkit.regear" -> REGEAR;
            case "perplayerkit.heal" -> HEAL;
            case "perplayerkit.repair" -> REPAIR;
            case "perplayerkit.admin" -> null;
            default -> throw new IllegalArgumentException("No location feature for permission " + permission);
        };
    }
}
