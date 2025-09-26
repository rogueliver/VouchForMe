package me.rogueliver.vouchforme.utils;

import org.bukkit.configuration.file.FileConfiguration;

public class TimeFormatter {

    public static String formatTime(long seconds, FileConfiguration config) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder timeString = new StringBuilder();

        if (days > 0) {
            timeString.append(config.getString("time-format.days", "{days}d").replace("{days}", String.valueOf(days)));
        }
        if (hours > 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(config.getString("time-format.hours", "{hours}h").replace("{hours}", String.valueOf(hours)));
        }
        if (minutes > 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(config.getString("time-format.minutes", "{minutes}m").replace("{minutes}", String.valueOf(minutes)));
        }
        if (secs > 0 && days == 0 && hours == 0) {
            if (!timeString.isEmpty()) timeString.append(" ");
            timeString.append(config.getString("time-format.seconds", "{seconds}s").replace("{seconds}", String.valueOf(secs)));
        }

        return timeString.toString();
    }
}