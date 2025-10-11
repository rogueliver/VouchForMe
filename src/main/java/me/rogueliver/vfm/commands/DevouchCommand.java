package me.rogueliver.vfm.commands;

import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.models.Vouch;
import me.rogueliver.vfm.models.VouchType;
import me.rogueliver.vfm.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.Arrays;

public class DevouchCommand implements CommandExecutor {

    private final VouchForMe plugin;

    public DevouchCommand(VouchForMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vouch.user")) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("prefix") + "<red>Usage: /devouch <player> [reason]"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("player-not-found")));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("cannot-vouch-self")));
            return true;
        }

        if (!plugin.getVouchManager().canVouch(player.getUniqueId(), target.getUniqueId())) {
            long daysLeft = plugin.getVouchManager().getDaysUntilCanVouch(player.getUniqueId(), target.getUniqueId());
            String message = plugin.getConfigManager().getMessage("on-cooldown")
                    .replace("{days}", String.valueOf(daysLeft))
                    .replace("{player}", target.getName());
            player.sendMessage(ColorUtil.color(message));
            return true;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : "No reason provided";

        Vouch vouch = Vouch.builder()
                .targetUuid(target.getUniqueId())
                .targetName(target.getName())
                .senderUuid(player.getUniqueId())
                .senderName(player.getName())
                .type(VouchType.DEVOUCH)
                .reason(reason)
                .createdAt(LocalDateTime.now())
                .build();

        plugin.getVouchManager().addVouch(vouch);

        String message = plugin.getConfigManager().getMessage("devouch-sent")
                .replace("{player}", target.getName());
        player.sendMessage(ColorUtil.color(message));

        try {
            Sound sound = Sound.valueOf(plugin.getConfig().getString("sounds.devouch-sent"));
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception ignored) {}

        return true;
    }
}
