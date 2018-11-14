package com.ericlam.inventory;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MySQLManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class GUIInventory {
    private HashMap<Player, Inventories> playerInv = new HashMap<>();
    private FileConfiguration gui;
    private ItemStack previousPage;
    private ItemStack nextPage;
    private Inventory buyInventory;
    private Inventory preRemoveInventory;
    private Plugin plugin;

    private static GUIInventory instance;

    public static GUIInventory getInstance() {
        if (instance == null) instance = new GUIInventory();
        return instance;
    }

    private GUIInventory(){
        plugin = ItemAunction.plugin;
        gui = Config.getInstance().getInventory();
        ItemStack previous = new ItemStack(Material.valueOf(gui.getString("item-material")));
        ItemStack next = previous.clone();
        ItemMeta previousMeta = previous.getItemMeta();
        ItemMeta nextMeta = next.getItemMeta();
        previousMeta.setDisplayName(gui.getString("item-previous").replace('&','§'));
        nextMeta.setDisplayName(gui.getString("item-next").replace('&','§'));
        previous.setItemMeta(previousMeta);
        next.setItemMeta(nextMeta);
        previousPage = previous;
        nextPage = next;
        buyInventory = Bukkit.createInventory(null,54,gui.getString("buy-inventory.title").replace('&','§'));
        buyInventory.setItem(45,previous);
        buyInventory.setItem(53,next);
        preRemoveInventory = Bukkit.createInventory(null,54,gui.getString("pre-remove-inventory.title").replace('&','§'));
        preRemoveInventory.setItem(45,previous);
        preRemoveInventory.setItem(53,next);
    }

    public void makeGUI(Player player){
        if (!playerInv.containsKey(player)){
            playerInv.put(player,new Inventories(buyInventory,preRemoveInventory));
        }
    }

    public Inventories takeGUI(Player player){
        return playerInv.get(player);
    }

    public void addItemsToGUI(ItemStack[] items,Player player){
        List<Inventory> remove = takeGUI(player).getRemove();
        int page = 0;
        for (int i = 0; i < items.length;i++) {
            ItemStack item = items[i];
            List<String> extralore = gui.getStringList("pre-remove-inventory.extra-lore");
            List<String> lore = item.getItemMeta().getLore();
            lore.addAll(extralore);
            item.setLore(lore);
            if (i % 45 == 0){
                remove.add(preRemoveInventory);
                page++;
            }
            remove.get(page).addItem(item);
        }
        takeGUI(player).setRemove(remove);
    }

    public void addItemsToGUI(HashMap<ItemStack, Integer> items,Player player){
        List<Inventory> buy = takeGUI(player).getBuy();
        int i = 0;
        int page = 0;
        for (ItemStack item : items.keySet()) {
            List<String> extralore = gui.getStringList("buy-inventory.extra-lore").stream().map(text -> text.replace("<money>", items.get(item)+"")).collect(Collectors.toList());
            List<String> lore = item.getItemMeta().getLore();
            lore.addAll(extralore);
            item.setLore(lore);
            if (i % 45 == 0){
                buy.add(buyInventory);
                page++;
            }
            buy.get(page).addItem(item);
            i++;
        }
        takeGUI(player).setBuy(buy);
    }

    public boolean buyItem(ItemStack item, Player player,int price,Inventory invbuy){
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (ItemAunction.economy.getBalance(offlinePlayer) < price) {
            player.sendMessage(Config.no_money);
            return false;
        }
        List<String> extralore = gui.getStringList("buy-inventory.extra-lore");
        List<String> lore = item.getItemMeta().getLore();
        int extralength = extralore.size();
        int lorelength = lore.size();


        if (checkInventoryFull(player)) {
            player.sendMessage(Config.full_inv);
            return false;
        }

        if (ItemAunction.economy.withdrawPlayer(offlinePlayer,price).type != EconomyResponse.ResponseType.SUCCESS){
            player.sendMessage(Config.no_money);
            return false;
        }

        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("DELETE FROM `"+Config.selltable+"` WHERE `ItemStack`=? AND `Trader-PlayerName=?` AND `Trader-Server`=?")){
            statement.setString(1, ItemStringConvert.itemStackToBase64(item));
            statement.setString(2,player.getName());
            statement.setString(3,Config.server);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (lorelength >= lorelength - extralength + 1) {
            lore.subList(lorelength - extralength + 1, lorelength + 1).clear();
        }
        item.setLore(lore);
        player.getInventory().addItem(item);
        invbuy.removeItem(item);
        takeGUI(player).getBuy().forEach(this::updateInventorySlot);
        clearEmptyInventory(takeGUI(player).getBuy());
        return true;
    }

    public boolean removeItem(ItemStack item,Player player,Inventory invremove){

        List<String> extralore = gui.getStringList("pre-remove-inventory.extra-lore");
        List<String> lore = item.getItemMeta().getLore();
        int extralength = extralore.size();
        int lorelength = lore.size();


        if (checkInventoryFull(player)) {
            player.sendMessage(Config.full_inv);
            return false;
        }


        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("DELETE FROM `"+Config.pre_remove_table+"` WHERE `Owner-PlayerName=?` OR `Owner-UUID`=? AND `ItemStack`=? AND `Owner-Server`=?")){
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3, ItemStringConvert.itemStackToBase64(item));
            statement.setString(4,Config.server);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (lorelength >= lorelength - extralength + 1) {
            lore.subList(lorelength - extralength + 1, lorelength + 1).clear();
        }

        item.setLore(lore);
        player.getInventory().addItem(item);
        invremove.removeItem(item);
        takeGUI(player).getRemove().forEach(this::updateInventorySlot);
        clearEmptyInventory(takeGUI(player).getRemove());
        return true;
    }

    private boolean checkInventoryFull(Player player){
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack.getType() == Material.AIR) return false;
        }
        return true;
    }

    private void updateInventorySlot(Inventory inventory){
        ItemStack[] items = inventory.getContents();
        int empty = inventory.firstEmpty();
        if (empty <= -1) return;
        int i = 0;
        inventory.removeItem(items);
        for (ItemStack item : items) {
            inventory.setItem(i,item);
            i++;
        }
    }

    private void clearEmptyInventory(List<Inventory> inventories){
        for (Inventory inventory : inventories) {
            boolean empty = true;
            for (ItemStack content : inventory.getContents()) {
                if (content!=null) empty = false;
            }
            if (empty) inventories.remove(inventory);
        }
    }
}
