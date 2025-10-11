package me.rogueliver.vfm.utils;

import me.rogueliver.vfm.VouchForMe;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final VouchForMe plugin;

    public ConfigManager(VouchForMe plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    public String getMessage(String key) {
        FileConfiguration config = plugin.getConfig();
        String prefix = config.getString("messages.prefix", "");
        String message = config.getString("messages." + key, "");
        return prefix + message;
    }

    public String getMessageWithoutPrefix(String key) {
        return plugin.getConfig().getString("messages." + key, "");
    }
}
