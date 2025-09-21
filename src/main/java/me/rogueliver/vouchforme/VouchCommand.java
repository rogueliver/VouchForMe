package me.rogueliver.vouchforme;

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
            case "vfm", "vouchforme" -> {
                if (args.length == 0) {
                    player.sendMessage(plugin.formatMessage("usage-vfm"));
                    return true;
                }

                if (args[0].equalsIgnoreCase("reload")) {
                    if (!player.hasPermission("vouchforme.reload")) {
                        player.sendMessage(plugin.formatMessage("config-reload-error"));
                        return true;
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

            case "vouch" -> {
                if (args.length == 0) {
                    player.sendMessage(plugin.formatMessage("usage-vouch"));
                    return true;
                }

                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
                handleVouchCommand(player, args[0], reason);
            }

            case "devouch" -> {
                if (args.length == 0) {
                    player.sendMessage(plugin.formatMessage("usage-devouch"));
                    return true;
                }

                String reason = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : null;
                handleDevouchCommand(player, args[0], reason);
            }

            case "remvouch", "removevouch" -> {
                if (args.length == 0) {
                    player.sendMessage(plugin.formatMessage("usage-remvouch"));
                    return true;
                }

                handleRemoveVouchCommand(player, args[0]);
            }

            case "vouches" -> {
                if (args.length == 0) {
                    player.sendMessage(plugin.formatMessage("usage-vouches"));
                    return true;
                }

                handleVouchesCommand(player, args[0]);
            }
        }

        return true;
    }

    private void handleVouchCommand(Player player, String targetName, String reason) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.formatMessage("player-not-found", "player", targetName));
            return;
        }

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
            String timeLeft = formatTime(cooldownRemaining);
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

    private void handleDevouchCommand(Player player, String targetName, String reason) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.formatMessage("player-not-found", "player", targetName));
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("cannot-vouch-self"));
            return;
        }

        long cooldownRemaining = vouchManager.getCooldownRemaining(player.getUniqueId());
        if (cooldownRemaining > 0) {
            String timeLeft = formatTime(cooldownRemaining);
            player.sendMessage(plugin.formatMessage("cooldown-active", "time", timeLeft));
            return;
        }

        vouchManager.addDevouch(player.getUniqueId(), target.getUniqueId(), reason);

        if (reason != null && !reason.trim().isEmpty()) {
            player.sendMessage(plugin.formatMessage("devouch-with-reason", "player", target.getName(), "reason", reason));
        } else {
            player.sendMessage(plugin.formatMessage("devouch-success", "player", target.getName()));
        }
    }

    private void handleRemoveVouchCommand(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.formatMessage("player-not-found", "player", targetName));
            return;
        }

        if (!vouchManager.hasVouched(player.getUniqueId(), target.getUniqueId()) &&
                !vouchManager.hasDevouched(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(plugin.formatMessage("no-vouch-or-devouch", "player", target.getName()));
            return;
        }

        vouchManager.removeVouch(player.getUniqueId(), target.getUniqueId(), null); // removes any vouch/devouch
        player.sendMessage(plugin.formatMessage("remvouch-success", "player", target.getName()));
    }

    private void handleVouchesCommand(Player player, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(plugin.formatMessage("player-not-found", "player", targetName));
            return;
        }

        List<VouchEntry> vouches = vouchManager.getVouchesFor(target.getUniqueId());

        if (vouches.isEmpty()) {
            player.sendMessage(plugin.formatMessage("no-vouches", "player", target.getName()));
            return;
        }

        player.sendMessage(plugin.formatMessage("vouches-header", "player", target.getName()));

        // Display vouches
        for (VouchEntry vouch : vouches) {
            OfflinePlayer voucher = Bukkit.getOfflinePlayer(vouch.getVoucherUuid());
            String voucherName = voucher.getName() != null ? voucher.getName() : "Unknown";
            String reason = vouch.getReason() != null ? vouch.getReason() : "No reason provided";

            if (vouch.isActive()) {
                player.sendMessage(plugin.formatMessage("vouches-entry", "voucher", voucherName, "reason", reason));
            }
        }

        // Display devouches
        for (VouchEntry vouch : vouches) {
            OfflinePlayer voucher = Bukkit.getOfflinePlayer(vouch.getVoucherUuid());
            String voucherName = voucher.getName() != null ? voucher.getName() : "Unknown";
            String reason = vouch.getReason() != null ? vouch.getReason() : "No reason provided";

            if (!vouch.isActive()) {
                player.sendMessage(plugin.formatMessage("devouches-entry", "devoucher", voucherName, "reason", reason));
            }
        }
    }

    private String formatTime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder timeString = new StringBuilder();

        if (days > 0) {
            timeString.append(plugin.getConfig().getString("time-format.days", "{days}d").replace("{days}", String.valueOf(days)));
        }
        if (hours > 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(plugin.getConfig().getString("time-format.hours", "{hours}h").replace("{hours}", String.valueOf(hours)));
        }
        if (minutes > 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(plugin.getConfig().getString("time-format.minutes", "{minutes}m").replace("{minutes}", String.valueOf(minutes)));
        }
        if (secs > 0 && days == 0 && hours == 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(plugin.getConfig().getString("time-format.seconds", "{seconds}s").replace("{seconds}", String.valueOf(secs)));
        }

        return timeString.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String commandName = command.getName().toLowerCase();

        if (commandName.equals("vfm") || commandName.equals("vouchforme")) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("vouchforme.reload")) {
                    completions.add("reload");
                }
                return completions;
            }
        } else if (commandName.equals("vouch") || commandName.equals("devouch") ||
                commandName.equals("remvouch") || commandName.equals("vouches")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
