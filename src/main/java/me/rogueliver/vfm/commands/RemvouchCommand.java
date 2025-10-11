package me.rogueliver.vfm.commands;

import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RemvouchCommand implements CommandExecutor {

    private final VouchForMe plugin;

    public RemvouchCommand(VouchForMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("vouch.admin")) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("no-permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("prefix") + "<red>Usage: /remvouch <player>"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);

        if (!target.hasPlayedBefore() && !target.isOnline()) {
            player.sendMessage(ColorUtil.color(plugin.getConfigManager().getMessage("player-not-found")));
            return true;
        }

        plugin.getVouchManager().removeAllVouches(target.getUniqueId(), target.getName());

        String message = plugin.getConfigManager().getMessage("all-vouches-removed")
                .replace("{player}", target.getName());
        player.sendMessage(ColorUtil.color(message));

        return true;
    }
}
