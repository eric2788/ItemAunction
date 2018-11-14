package com.ericlam.inventory;

import org.bukkit.inventory.ItemStack;

import java.sql.Timestamp;
import java.time.LocalDate;

public class ItemData {
    private ItemStack item;
    private int price;
    private LocalDate timestamp;

    public ItemData(ItemStack item, int price, long timestamp) {
        this.item = item;
        this.price = price;
        this.timestamp = new Timestamp(timestamp).toLocalDateTime().toLocalDate();
    }

    public ItemStack getItem() {
        return item;
    }

    public int getPrice() {
        return price;
    }

    public String getTimestamp() {
        return timestamp.toString();
    }
}
