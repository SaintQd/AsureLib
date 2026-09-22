package org.saintqd.asurelib.gui;

import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.function.Consumer;

public class AsureGUIButton {

    private Consumer<InventoryClickEvent> eventConsumer;
    public AsureGUIButton consumer(Consumer<InventoryClickEvent> consumer) {
        this.eventConsumer = consumer;
        return this;
    }
    public Consumer<InventoryClickEvent> getEventConsumer() {
        return eventConsumer;
    }
}
