package me.rogueliver.vouchforme;

import me.rogueliver.vouchforme.commands.VouchCommand;
import me.rogueliver.vouchforme.data.VouchManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.plugin.java.JavaPlugin;

public class VouchForMePlugin extends JavaPlugin {

    private static VouchForMePlugin instance;
    private VouchManager vouchManager;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        instance = this;
        miniMessage = MiniMessage.miniMessage();

        saveDefaultConfig();

        vouchManager = new VouchManager(this);

        VouchCommand commandHandler = new VouchCommand(this, vouchManager);

        getCommand("vfm").setExecutor(commandHandler);
        getCommand("vfm").setTabCompleter(commandHandler);
        getCommand("vouch").setExecutor(commandHandler);
        getCommand("vouch").setTabCompleter(commandHandler);
        getCommand("devouch").setExecutor(commandHandler);
        getCommand("devouch").setTabCompleter(commandHandler);
        getCommand("vouches").setExecutor(commandHandler);
        getCommand("vouches").setTabCompleter(commandHandler);

        getLogger().info("VouchForMe enabled successfully");
    }

    @Override
    public void onDisable() {
        if (vouchManager != null) {
            vouchManager.closeConnection();
        }
        getLogger().info("VouchForMe disabled");
    }

    public static VouchForMePlugin getInstance() {
        return instance;
    }

    public VouchManager getVouchManager() {
        return vouchManager;
    }

    public Component formatMessage(String messageKey, String... replacements) {
        String message = getConfig().getString("messages." + messageKey, messageKey);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
            }
        }

        return miniMessage.deserialize(message);
    }

    public void reloadConfiguration() {
        reloadConfig();
        if (vouchManager != null) {
            vouchManager.reloadData();
        }
    }
}