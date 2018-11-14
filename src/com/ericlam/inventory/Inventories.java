package com.ericlam.inventory;

import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.List;

public class Inventories {
    private List<Inventory> buy = new ArrayList<>();
    private List<Inventory> remove = new ArrayList<>();

    public Inventories(Inventory buy, Inventory remove) {
        this.buy.add(buy);
        this.remove.add(remove);
    }

    public List<Inventory> getBuy() {
        return buy;
    }

    public void setBuy(List<Inventory> buy) {
        this.buy = buy;
    }

    public List<Inventory> getRemove() {
        return remove;
    }

    public void setRemove(List<Inventory> remove) {
        this.remove = remove;
    }

}
