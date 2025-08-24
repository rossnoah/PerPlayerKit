/*

    Style Manager
        by kirushkinx (https://github.com/kirushkinx)

 */

package dev.noah.perplayerkit.util;

import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class StyleManager {
    private static StyleManager instance;
    private final Plugin plugin;

    private Material glassMaterial;
    private String titleColor;

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
        this.titleColor = miniMessageToLegacy(colorTag);
    }

    private String miniMessageToLegacy(String miniMessage) {
        try {
            Component component = MiniMessage.miniMessage().deserialize(miniMessage + "test");
            return LegacyComponentSerializer.legacySection().serialize(component).replace("test", "");
        } catch (Exception e) {
            return "<blue>";
        }
    }

    public Material getGlassMaterial() {
        return glassMaterial;
    }

    public String getMainColor() {
        return titleColor;
    }
}