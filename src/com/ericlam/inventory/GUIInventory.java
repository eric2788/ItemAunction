package com.ericlam.inventory;

import com.ericlam.config.Config;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;

public class GUIInventory {
    private HashMap<Player, Inventory> playerInventory = new HashMap<>();
    private FileConfiguration gui;
    private Inventory buyInventory;
    private Inventory preRemoveInventory;

    private static GUIInventory instance;

    public static GUIInventory getInstance() {
        if (instance == null) instance = new GUIInventory();
        return instance;
    }

    private GUIInventory(){
        gui = Config.getInstance().getInventory();
        buyInventory = Bukkit.createInventory(null,54,gui.getString("buy-inventory.title"));
        preRemoveInventory = Bukkit.createInventory(null,54,"pre-remove-inventory.title");
    }

    public void addItemsToBuy(ItemStack[] items){
        for (ItemStack item : items) {
            addItemLore(item,buyInventory);
        }
        buyInventory.addItem(items);
    }

    public void addItemToPre(ItemStack[] items){
        for (ItemStack item : items) {
            addItemLore(item,preRemoveInventory);
        }
        preRemoveInventory.addItem(items);
    }

    private void addItemLore(ItemStack item, Inventory inventory){
        if (inventory != preRemoveInventory && inventory != buyInventory) return;
        List<String> extralore = inventory == buyInventory ? gui.getStringList("buy-inventory.extra-lore") : gui.getStringList("pre-remove-inventory.extra-lore");
        List<String> lore = item.getLore();
        extralore.forEach(lores -> {
            assert lore != null;
            lore.add(lores);
        });
        item.setLore(lore);
    }

    public void clickedItem(ItemStack item, Inventory inventory){
        if (inventory != preRemoveInventory && inventory != buyInventory) return;
        List<String> extralore = inventory == buyInventory ? gui.getStringList("buy-inventory.extra-lore") : gui.getStringList("pre-remove-inventory.extra-lore");
        List<String> lore = item.getLore();
        int extralength = extralore.size();
        int lorelength = lore.size();
        if (lorelength >= lorelength - extralength + 1) {
            lore.subList(lorelength - extralength + 1, lorelength + 1).clear();
        }
    }
}
