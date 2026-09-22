package org.saintqd.asurelib.gui.holders;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.saintqd.asurelib.gui.AsureGUI;

public class AsureGUIHolder implements InventoryHolder {

    private final AsureGUI gui;

    public AsureGUIHolder(AsureGUI gui) {
        this.gui = gui;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }

    public AsureGUI getGui() {
        return gui;
    }
}
