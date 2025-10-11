package me.rogueliver.vfm;

import lombok.Getter;
import me.rogueliver.vfm.commands.*;
import me.rogueliver.vfm.database.DatabaseManager;
import me.rogueliver.vfm.listeners.GuiListener;
import me.rogueliver.vfm.redis.RedisManager;
import me.rogueliver.vfm.utils.ConfigManager;
import me.rogueliver.vfm.utils.VouchManager;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class VouchForMe extends JavaPlugin {

    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private RedisManager redisManager;
    private VouchManager vouchManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.loadConfig();

        databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        redisManager = new RedisManager(this);
        if (!redisManager.connect()) {
            getLogger().severe("Failed to connect to Redis. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        vouchManager = new VouchManager(this);

        registerCommands();
        registerListeners();

        getLogger().info("Imagine cool ASCII art here");
        getLogger().info("VouchForMe has been enabled!");
        getLogger().info("You are running VFM version " + getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.disconnect();
        }
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        getLogger().info("VouchForMe has been disabled!");
    }

    private void registerCommands() {
        getCommand("vouch").setExecutor(new VouchCommand(this));
        getCommand("devouch").setExecutor(new DevouchCommand(this));
        getCommand("vouches").setExecutor(new VouchesCommand(this));
        getCommand("remvouch").setExecutor(new RemvouchCommand(this));
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    }
}
