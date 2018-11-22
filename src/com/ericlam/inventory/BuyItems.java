package com.ericlam.inventory;

public class BuyItems {
    private int price;
    private String nameId;

    public BuyItems(int price, String nameId) {
        this.price = price;
        this.nameId = nameId;
    }

    public int getPrice() {
        return price;
    }

    public String getNameId() {
        return nameId;
    }
}
