package me.rogueliver.vfm.utils;

import net.md_5.bungee.api.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<gradient:(#[A-Fa-f0-9]{6}):(#[A-Fa-f0-9]{6})>(.*?)</gradient>");

    public static String color(String message) {
        message = translateGradients(message);
        message = translateHexColors(message);
        message = ChatColor.translateAlternateColorCodes('&', message);
        return message;
    }

    private static String translateHexColors(String message) {
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            matcher.appendReplacement(buffer, ChatColor.of("#" + hexColor).toString());
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String translateGradients(String message) {
        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String endColor = matcher.group(2);
            String text = matcher.group(3);

            String gradient = applyGradient(text, startColor, endColor);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(gradient));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String applyGradient(String text, String startColor, String endColor) {
        StringBuilder result = new StringBuilder();
        int length = text.length();

        if (length == 0) return text;

        int[] start = hexToRgb(startColor);
        int[] end = hexToRgb(endColor);

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            int r = (int) (start[0] + ratio * (end[0] - start[0]));
            int g = (int) (start[1] + ratio * (end[1] - start[1]));
            int b = (int) (start[2] + ratio * (end[2] - start[2]));

            String hex = String.format("#%02x%02x%02x", r, g, b);
            result.append(ChatColor.of(hex)).append(text.charAt(i));
        }

        return result.toString();
    }

    private static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
                Integer.parseInt(hex.substring(0, 2), 16),
                Integer.parseInt(hex.substring(2, 4), 16),
                Integer.parseInt(hex.substring(4, 6), 16)
        };
    }
}
