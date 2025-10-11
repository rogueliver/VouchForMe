package me.rogueliver.vfm.utils;

import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.models.Vouch;
import me.rogueliver.vfm.models.VouchMessage;
import me.rogueliver.vfm.models.VouchType;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class VouchManager {

    private final VouchForMe plugin;

    public VouchManager(VouchForMe plugin) {
        this.plugin = plugin;
    }

    public boolean canVouch(UUID senderUuid, UUID targetUuid) {
        Vouch existingVouch = plugin.getDatabaseManager().getVouch(targetUuid, senderUuid);
        if (existingVouch == null) {
            return true;
        }

        long daysSinceLastVouch = ChronoUnit.DAYS.between(existingVouch.getCreatedAt(), LocalDateTime.now());
        int cooldownDays = plugin.getConfig().getInt("cooldown.days");

        return daysSinceLastVouch >= cooldownDays;
    }

    public long getDaysUntilCanVouch(UUID senderUuid, UUID targetUuid) {
        Vouch existingVouch = plugin.getDatabaseManager().getVouch(targetUuid, senderUuid);
        if (existingVouch == null) {
            return 0;
        }

        int cooldownDays = plugin.getConfig().getInt("cooldown.days");
        long daysSinceLastVouch = ChronoUnit.DAYS.between(existingVouch.getCreatedAt(), LocalDateTime.now());

        return Math.max(0, cooldownDays - daysSinceLastVouch);
    }

    public void addVouch(Vouch vouch) {
        plugin.getDatabaseManager().addVouch(vouch);
        plugin.getRedisManager().publishVouch(vouch, "ADD");

        notifyPlayers(vouch);
    }

    public void removeAllVouches(UUID targetUuid, String targetName) {
        plugin.getDatabaseManager().removeAllVouches(targetUuid);
        plugin.getRedisManager().publishRemoveAll(targetUuid.toString(), targetName);
    }

    public void handleRedisMessage(VouchMessage message) {
        if (message.getAction().equals("REMOVE_ALL")) {
            plugin.getVouchManager().refreshOpenGuis(message.getVouch().getTargetUuid());
            return;
        }

        Vouch vouch = message.getVouch();
        notifyPlayers(vouch);
        refreshOpenGuis(vouch.getTargetUuid());
    }

    private void notifyPlayers(Vouch vouch) {
        Player target = Bukkit.getPlayer(vouch.getTargetUuid());
        if (target != null && target.isOnline()) {
            String messageKey = vouch.getType() == VouchType.VOUCH ? "vouch-received" : "devouch-received";
            String soundKey = vouch.getType() == VouchType.VOUCH ? "vouch-received" : "devouch-received";

            String message = plugin.getConfigManager().getMessage(messageKey)
                    .replace("{player}", vouch.getSenderName());
            target.sendMessage(ColorUtil.color(message));

            try {
                Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds." + soundKey));
                target.playSound(target.getLocation(), sound, 1.0f, 1.0f);
            } catch (Exception ignored) {}
        }
    }

    private void refreshOpenGuis(UUID targetUuid) {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof me.rogueliver.vfm.gui.VouchesGui) {
                me.rogueliver.vfm.gui.VouchesGui gui = (me.rogueliver.vfm.gui.VouchesGui) player.getOpenInventory().getTopInventory().getHolder();
                if (gui.getTargetUuid().equals(targetUuid)) {
                    gui.refresh();
                }
            }
        });
    }
}
