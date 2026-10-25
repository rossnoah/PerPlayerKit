/*

    Style Manager
        by kirushkinx (https://github.com/kirushkinx)

 */

package dev.noah.perplayerkit.util;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;

public class StyleManager {
    private static StyleManager instance;
    private final Plugin plugin;

    private Material glassMaterial;
    private String titleColor;
    private String titleColorTag;

    public StyleManager(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
        loadConfig();
    }

    public static StyleManager get() {
        return instance;
    }

    public void loadConfig() {
        try {
            this.glassMaterial = Material.valueOf(plugin.getConfig().getString("interface.glass-material", "BLUE_STAINED_GLASS_PANE"));
        } catch (IllegalArgumentException e) {
            this.glassMaterial = Material.BLUE_STAINED_GLASS_PANE;
        }

        String colorTag = plugin.getConfig().getString("interface.main-color", "<blue>");
        this.titleColorTag = colorTag;
        this.titleColor = miniMessageToLegacy(colorTag);
    }

    private String miniMessageToLegacy(String miniMessage) {
        try {
            return TextUtil.formattingPrefix(miniMessage);
        } catch (Exception e) {
            return "§9";
        }
    }

    public static String convertMiniMessage(String miniMessage) {
        try {
            return TextUtil.toLegacy(miniMessage);
        } catch (Exception e) {
            return miniMessage;
        }
    }

    public Material getGlassMaterial() {
        return glassMaterial;
    }

    public String getPrimaryColor() {
        return titleColor;
    }

    public String getPrimaryColorTag() {
        return titleColorTag;
    }
}