package me.rogueliver.vouchforme.commands;

import me.rogueliver.vouchforme.VouchForMePlugin;
import me.rogueliver.vouchforme.data.VouchManager;
import me.rogueliver.vouchforme.data.VouchEntry;
import me.rogueliver.vouchforme.utils.TimeFormatter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VouchCommand implements CommandExecutor, TabCompleter {

    private final VouchForMePlugin plugin;
    private final VouchManager vouchManager;

    public VouchCommand(VouchForMePlugin plugin, VouchManager vouchManager) {
        this.plugin = plugin;
        this.vouchManager = vouchManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players!");
            return true;
        }

        String commandName = command.getName().toLowerCase();

        switch (commandName) {
            case "vfm", "vouchforme" -> handleMainCommand(player, args);
            case "vouch" -> handleVouchCommand(player, args);
            case "devouch" -> handleDevouchCommand(player, args);
            case "vouches" -> handleVouchesCommand(player, args);
        }

        return true;
    }

    private void handleMainCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.formatMessage("usage-vfm"));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("vouchforme.reload")) {
                player.sendMessage(plugin.formatMessage("config-reload-error"));
                return;
            }

            try {
                plugin.reloadConfiguration();
                player.sendMessage(plugin.formatMessage("config-reloaded"));
            } catch (Exception e) {
                player.sendMessage(plugin.formatMessage("config-reload-error"));
                plugin.getLogger().severe("Error reloading configuration: " + e.getMessage());
            }
        } else {
            player.sendMessage(plugin.formatMessage("usage-vfm"));
        }
    }

    private void handleVouchCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.formatMessage("usage-vouch"));
            return;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        OfflinePlayer target = getValidTarget(player, args[0]);
        if (target == null) return;

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("cannot-vouch-self"));
            return;
        }

        if (vouchManager.hasVouched(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("already-vouched", "player", target.getName()));
            return;
        }

        long cooldownRemaining = vouchManager.getCooldownRemaining(player.getUniqueId());
        if (cooldownRemaining > 0) {
            String timeLeft = TimeFormatter.formatTime(cooldownRemaining, plugin.getConfig());
            player.sendMessage(plugin.formatMessage("cooldown-active", "time", timeLeft));
            return;
        }

        vouchManager.addVouch(player.getUniqueId(), target.getUniqueId(), reason);

        if (reason != null && !reason.trim().isEmpty()) {
            player.sendMessage(plugin.formatMessage("vouch-with-reason", "player", target.getName(), "reason", reason));
        } else {
            player.sendMessage(plugin.formatMessage("vouch-success", "player", target.getName()));
        }
    }

    private void handleDevouchCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.formatMessage("usage-devouch"));
            return;
        }

        String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
        OfflinePlayer target = getValidTarget(player, args[0]);
        if (target == null) return;

        if (!vouchManager.hasVouched(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("no-vouch-exists", "player", target.getName()));
            return;
        }

        boolean applyDevouchCooldown = plugin.getConfig().getBoolean("cooldown.apply-to-devouch", false);
        if (applyDevouchCooldown) {
            long cooldownRemaining = vouchManager.getCooldownRemaining(player.getUniqueId());
            if (cooldownRemaining > 0) {
                String timeLeft = TimeFormatter.formatTime(cooldownRemaining, plugin.getConfig());
                player.sendMessage(plugin.formatMessage("cooldown-active", "time", timeLeft));
                return;
            }
        }

        vouchManager.removeVouch(player.getUniqueId(), target.getUniqueId(), reason);

        if (reason != null && !reason.trim().isEmpty()) {
            player.sendMessage(plugin.formatMessage("devouch-with-reason", "player", target.getName(), "reason", reason));
        } else {
            player.sendMessage(plugin.formatMessage("devouch-success", "player", target.getName()));
        }
    }

    private void handleVouchesCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(plugin.formatMessage("usage-vouches"));
            return;
        }

        OfflinePlayer target = getValidTarget(player, args[0]);
        if (target == null) return;

        List<VouchEntry> vouches = vouchManager.getVouchesFor(target.getUniqueId());

        if (vouches.isEmpty()) {
            player.sendMessage(plugin.formatMessage("no-vouches", "player", target.getName()));
            return;
        }

        player.sendMessage(plugin.formatMessage("vouches-header", "player", target.getName()));

        for (VouchEntry vouch : vouches) {
            OfflinePlayer voucher = Bukkit.getOfflinePlayer(vouch.getVoucherUuid());
            String voucherName = voucher.getName() != null ? voucher.getName() : "Unknown";
            String reason = vouch.getReason() != null ? vouch.getReason() : "No reason provided";

            if (vouch.isActive()) {
                player.sendMessage(plugin.formatMessage("vouches-entry", "voucher", voucherName, "reason", reason));
            } else {
                player.sendMessage(plugin.formatMessage("devouches-entry", "devoucher", voucherName, "reason", reason));
            }
        }
    }

    private OfflinePlayer getValidTarget(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.formatMessage("player-not-found", "player", targetName));
            return null;
        }

        return target;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();

        if (commandName.equals("vfm") || commandName.equals("vouchforme")) {
            if (args.length == 1 && sender.hasPermission("vouchforme.reload")) {
                return List.of("reload").stream()
                        .filter(s -> s.startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        } else if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}