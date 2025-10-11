package me.rogueliver.vfm.gui;

import lombok.Getter;
import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.models.Vouch;
import me.rogueliver.vfm.models.VouchType;
import me.rogueliver.vfm.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VouchesGui implements InventoryHolder {

    private final VouchForMe plugin;
    @Getter
    private final UUID targetUuid;
    private final String targetName;
    private List<Vouch> vouches;
    private final Inventory inventory;
    @Getter
    private int currentPage;
    private final int itemsPerPage = 45;

    public VouchesGui(VouchForMe plugin, UUID targetUuid, String targetName, List<Vouch> vouches) {
        this.plugin = plugin;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.vouches = vouches;
        this.currentPage = 0;

        String title = plugin.getConfig().getString("gui.title", "{player}'s Vouches")
                .replace("{player}", targetName);
        this.inventory = Bukkit.createInventory(this, 54, ColorUtil.color(title));

        populate();
    }

    private void populate() {
        inventory.clear();

        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, vouches.size());

        for (int i = startIndex; i < endIndex; i++) {
            Vouch vouch = vouches.get(i);
            ItemStack item = createVouchItem(vouch);
            inventory.setItem(i - startIndex, item);
        }

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, createFillerItem());
        }

        if (currentPage > 0) {
            inventory.setItem(48, createPreviousPageItem());
        }

        if (endIndex < vouches.size()) {
            inventory.setItem(50, createNextPageItem());
        }
    }

    private ItemStack createVouchItem(Vouch vouch) {
        String configPath = vouch.getType() == VouchType.VOUCH ? "gui.items.vouch" : "gui.items.devouch";

        Material material = Material.valueOf(plugin.getConfig().getString(configPath + ".material", "PAPER"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString(configPath + ".name", "{player}")
                .replace("{player}", vouch.getSenderName());
        meta.setDisplayName(ColorUtil.color(name));

        List<String> loreTemplate = plugin.getConfig().getStringList(configPath + ".lore");
        List<String> lore = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String date = vouch.getCreatedAt().format(formatter);

        for (String line : loreTemplate) {
            lore.add(ColorUtil.color(line
                    .replace("{player}", vouch.getSenderName())
                    .replace("{date}", date)
                    .replace("{reason}", vouch.getReason())));
        }

        meta.setLore(lore);
        item.setItemMeta(meta);

        return item;
    }

    private ItemStack createFillerItem() {
        Material material = Material.valueOf(plugin.getConfig().getString("gui.items.filler.material", "GRAY_STAINED_GLASS_PANE"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("gui.items.filler.name", " ");
        meta.setDisplayName(ColorUtil.color(name));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createNextPageItem() {
        Material material = Material.valueOf(plugin.getConfig().getString("gui.items.next-page.material", "ARROW"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("gui.items.next-page.name", "Next Page")
                .replace("{page}", String.valueOf(currentPage + 2));
        meta.setDisplayName(ColorUtil.color(name));

        List<String> loreTemplate = plugin.getConfig().getStringList("gui.items.next-page.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ColorUtil.color(line.replace("{page}", String.valueOf(currentPage + 2))));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPreviousPageItem() {
        Material material = Material.valueOf(plugin.getConfig().getString("gui.items.previous-page.material", "ARROW"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getConfig().getString("gui.items.previous-page.name", "Previous Page")
                .replace("{page}", String.valueOf(currentPage));
        meta.setDisplayName(ColorUtil.color(name));

        List<String> loreTemplate = plugin.getConfig().getStringList("gui.items.previous-page.lore");
        List<String> lore = new ArrayList<>();
        for (String line : loreTemplate) {
            lore.add(ColorUtil.color(line.replace("{page}", String.valueOf(currentPage))));
        }
        meta.setLore(lore);

        item.setItemMeta(meta);
        return item;
    }

    public void nextPage() {
        if ((currentPage + 1) * itemsPerPage < vouches.size()) {
            currentPage++;
            populate();
        }
    }

    public void previousPage() {
        if (currentPage > 0) {
            currentPage--;
            populate();
        }
    }

    public void refresh() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            vouches = plugin.getDatabaseManager().getVouches(targetUuid);
            Bukkit.getScheduler().runTask(plugin, this::populate);
        });
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
