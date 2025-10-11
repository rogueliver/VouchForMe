package me.rogueliver.vfm.listeners;

import me.rogueliver.vfm.VouchForMe;
import me.rogueliver.vfm.gui.VouchesGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    private final VouchForMe plugin;

    public GuiListener(VouchForMe plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof VouchesGui)) {
            return;
        }

        event.setCancelled(true);

        VouchesGui gui = (VouchesGui) holder;
        int slot = event.getSlot();

        if (slot == 48) {
            gui.previousPage();
        } else if (slot == 50) {
            gui.nextPage();
        }
    }
}
