package com.ericlam.inventory.constructors;

import org.bukkit.inventory.ItemStack;

public class BuyItems {
    private int price;
    private String nameId;
    private ItemStack item;

    public BuyItems(int price, String nameId, ItemStack item) {
        this.price = price;
        this.nameId = nameId;
        this.item = item;
    }

    public int getPrice() {
        return price;
    }

    public String getNameId() {
        return nameId;
    }

    public ItemStack getItem() {
        return item;
    }
}
