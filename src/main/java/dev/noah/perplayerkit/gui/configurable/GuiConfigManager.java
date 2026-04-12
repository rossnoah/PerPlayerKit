package dev.noah.perplayerkit.gui.configurable;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class GuiConfigManager {
    private final Plugin plugin;
    private final File guiConfigFile;
    private FileConfiguration guiConfiguration;

    public GuiConfigManager(Plugin plugin) {
        this.plugin = plugin;
        this.guiConfigFile = new File(plugin.getDataFolder(), "guis.yml");
        load();
    }

    public final void load() {
        if (!guiConfigFile.exists()) {
            plugin.saveResource("guis.yml", false);
        }
        guiConfiguration = YamlConfiguration.loadConfiguration(guiConfigFile);
    }

    public ConfigurationSection getGuiSection(String guiId) {
        return guiConfiguration.getConfigurationSection("guis." + guiId);
    }
}
