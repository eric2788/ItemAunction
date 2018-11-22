package com.ericlam.inventory;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MySQLManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUIInventory {
    private HashMap<UUID, Inventories> playerInv = new HashMap<>();
    private HashMap<UUID, HashMap<ItemStack,BuyItems>> playerItemsList = new HashMap<>();
    private HashMap<UUID, ItemStack[]> playerItems = new HashMap<>();
    private HashMap<UUID, Boolean> changed = new HashMap<>();
    private FileConfiguration gui;
    private Inventory buyInventory;
    private Inventory preRemoveInventory;
    private ItemStack previous;
    private ItemStack next;
    private Plugin plugin;

    private static GUIInventory instance;

    public static GUIInventory getInstance() {
        if (instance == null) instance = new GUIInventory();
        return instance;
    }

    public HashMap<UUID, Boolean> getChanged() {
        return changed;
    }

    public HashMap<ItemStack, BuyItems> getBuyItemsMap(Player player) {
        return playerItemsList.get(player.getUniqueId());
    }

    private BuyItems getBuyItems(Player player, ItemStack itemStack){
        for (ItemStack stack : playerItemsList.get(player.getUniqueId()).keySet()){
            if (stack.isSimilar(itemStack)) return playerItemsList.get(player.getUniqueId()).get(stack);
        }
        return null;
    }


    private void removeBuyItems(Player player, ItemStack itemStack){
        playerItemsList.get(player.getUniqueId()).remove(itemStack);
    }

    public ItemStack[] getRemoveItems(Player player) {
        return playerItems.get(player.getUniqueId());
    }

    private void removeRemoveItems(Player player, ItemStack itemStack){
        ItemStack[] items = Arrays.stream(playerItems.get(player.getUniqueId())).filter(item -> !item.isSimilar(itemStack)).toArray(ItemStack[]::new);
        playerItems.put(player.getUniqueId(),items);
    }

    public void setPlayerItemsList(Player player, HashMap<ItemStack, BuyItems> map){
        if (map.size() == 0) return;
        if (playerItemsList.containsKey(player.getUniqueId()) && map.size() == playerItemsList.get(player.getUniqueId()).size()) return;
        HashMap<ItemStack, BuyItems> newmap = new HashMap<>();
        for (ItemStack item : map.keySet()) {
            final int price = map.get(item).getPrice();
            final String nameId = map.get(item).getNameId();
            List<String> extralore = gui.getStringList("buy-inventory.extra-lore").stream().map(text -> ChatColor.translateAlternateColorCodes('&',text.replace("<money>", price+""))).collect(Collectors.toList());
            List<String> lore = item.getItemMeta().getLore();
            if (lore == null || lore.size() == 0 ){
                item.setLore(extralore);
            } else {
                lore.addAll(extralore);
                item.setLore(lore);
            }
            newmap.put(item,new BuyItems(price,nameId));
        }
        playerItemsList.put(player.getUniqueId(),newmap);
        changed.put(player.getUniqueId(),true);
    }

    public void setPlayerItems(Player player,ItemStack[] item){
        if (item.length == 0 ) return;
        if (playerItems.containsKey(player.getUniqueId()) && item.length == playerItems.get(player.getUniqueId()).length) return;
        playerItems.put(player.getUniqueId(),item);
        changed.put(player.getUniqueId(),true);
    }

    public ItemStack getNext() {
        return next;
    }

    public ItemStack getPrevious() {
        return previous;
    }

    private GUIInventory(){
        plugin = ItemAunction.plugin;
        gui = Config.getInstance().getInventory();
        ItemStack previous = new ItemStack(Material.valueOf(gui.getString("item-material")));
        ItemStack next = previous.clone();
        ItemMeta previousMeta = previous.getItemMeta();
        ItemMeta nextMeta = next.getItemMeta();
        previousMeta.setDisplayName("§1§2§3§4§5§6§7§8§9"+gui.getString("item-previous").replace('&','§'));
        nextMeta.setDisplayName("§1§2§3§4§5§6§7§8§9"+gui.getString("item-next").replace('&','§'));
        previous.setItemMeta(previousMeta);
        next.setItemMeta(nextMeta);
        this.previous = previous;
        this.next = next;
        buyInventory = Bukkit.createInventory(null,54,gui.getString("buy-inventory.title").replace('&','§'));
        buyInventory.setItem(45,previous);
        buyInventory.setItem(53,next);
        preRemoveInventory = Bukkit.createInventory(null,54,gui.getString("pre-remove-inventory.title").replace('&','§'));
        preRemoveInventory.setItem(45,previous);
        preRemoveInventory.setItem(53,next);
    }

    public void makeGUI(Player player){
        if (!playerInv.containsKey(player.getUniqueId())){
            playerInv.put(player.getUniqueId(),new Inventories(buyInventory,preRemoveInventory));
        }
    }

    public int findPage(Inventory inventory,Player player,boolean isbuy){
        if (!takeGUI(player).getBuy().contains(inventory) && !takeGUI(player).getRemove().contains(inventory)) return 0;
        int page = 0;
        if (isbuy){
            for (Inventory inv : takeGUI(player).getBuy()) {
                if (inv.equals(inventory)) return page;
                page++;
            }
        }else{
            for (Inventory inv : takeGUI(player).getRemove()) {
                if (inv.equals(inventory)) return page;
                page++;
            }
        }
        return 0;
    }

    public Inventories takeGUI(Player player){
        return playerInv.get(player.getUniqueId());
    }

    public void addRemoveItemsToGUI(Player player){
        if (!changed.get(player.getUniqueId())) return;
        playerInv.put(player.getUniqueId(), new Inventories(buyInventory,preRemoveInventory));
        ItemStack[] items = getRemoveItems(player);
        if (items == null || items.length == 0) return;
        List<Inventory> remove = takeGUI(player).getRemove();
        int page = 0;

        for (int i = 0; i < items.length;i++) {
            ItemStack item = items[i];
            List<String> extralore = gui.getStringList("pre-remove-inventory.extra-lore");
            List<String> lore = item.getItemMeta().getLore();
            if (lore == null || lore.size() == 0) {
                item.setLore(extralore);
            } else {
                lore.addAll(extralore);
                item.setLore(lore);
            }
            if (i % 45 == 0 && i > 0){
                remove.add(preRemoveInventory);
                page++;
            }
            remove.get(page).addItem(item);
        }
        takeGUI(player).setRemove(remove);
        changed.put(player.getUniqueId(),false);
    }

    public void addBuyItemsToGUI(Player player){
        if (!changed.get(player.getUniqueId())) return;
        playerInv.put(player.getUniqueId(), new Inventories(buyInventory,preRemoveInventory));
        HashMap<ItemStack, BuyItems> items = getBuyItemsMap(player);
        if (items == null || items.size() == 0) return;
        List<Inventory> buy = takeGUI(player).getBuy();
        int i = 0;
        int page = 0;
        for (ItemStack item : items.keySet()) {
            if (i % 45 == 0 && i > 0){
                buy.add(buyInventory);
                page++;
            }
            buy.get(page).addItem(item);
            i++;
        }

        takeGUI(player).setBuy(buy);
        changed.put(player.getUniqueId(),false);
    }

    public boolean buyItem(ItemStack item, Player player,Inventory invbuy){
        BuyItems buy = getBuyItems(player,item);
        if (buy == null) return false;
        int price = buy.getPrice();
        String nameId = buy.getNameId();
        if (price == -1) return false;
        if (!takeGUI(player).getBuy().contains(invbuy)) return false;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (ItemAunction.economy.getBalance(offlinePlayer) < price) {
            Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.no_money));
            return false;
        }
        List<String> extralore = gui.getStringList("buy-inventory.extra-lore").stream().map(txt -> ChatColor.translateAlternateColorCodes('&',txt)).collect(Collectors.toList());
        List<String> lore = item.getItemMeta().getLore();
        final int extralength = extralore.size();
        final int lorelength = lore.size();
        ItemStack check = item.clone();
        restoreLore(lore, extralength, lorelength, check);
        if (checkInventoryFull(player)) {
            Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.full_inv));
            return false;
        }

        if (ItemAunction.economy.withdrawPlayer(offlinePlayer,price).type != EconomyResponse.ResponseType.SUCCESS){
            Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.no_money));
            return false;
        }

        try(Connection connection = MySQLManager.getInstance().getConneciton();
            PreparedStatement delete = connection.prepareStatement("DELETE FROM `"+Config.selltable+"` WHERE `ItemStack`=? AND `Trader-PlayerName`=? AND `Trader-Server`=? AND `NameID`=?");
            PreparedStatement checker = connection.prepareStatement("SELECT `ItemStack` FROM `"+Config.selltable+"` WHERE `ItemStack`=? AND `Trader-PlayerName`=? AND `Trader-Server`=? AND `NameID`=?")){
            checker.setString(1, ItemStringConvert.itemStackToBase64(check));
            checker.setString(2,player.getName());
            checker.setString(3,Config.server);
            checker.setString(4,nameId);
            ResultSet resultSet = checker.executeQuery();
            if (resultSet.next()) {
                delete.setString(1, ItemStringConvert.itemStackToBase64(check));
                delete.setString(2, player.getName());
                delete.setString(3, Config.server);
                delete.setString(4,nameId);
                delete.execute();
            }else return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }



        Bukkit.getScheduler().runTask(plugin,()->{
            player.getInventory().addItem(check);
            invbuy.removeItem(item);
        });

        changed.put(player.getUniqueId(),true);
        removeBuyItems(player,item);
        return true;
    }

    private void restoreLore(List<String> lore, int extralength, int lorelength, ItemStack check) {
        Bukkit.getScheduler().runTask(plugin,()->{
            int i = lorelength-1;
            while (i > lorelength - extralength -1){
                lore.remove(i);
                i--;
            }
            check.setLore(lore);
        });
    }

    public boolean removeItem(ItemStack item,Player player,Inventory invremove){
        if (!takeGUI(player).getRemove().contains(invremove)) return false;
        List<String> extralore = gui.getStringList("pre-remove-inventory.extra-lore");
        List<String> lore = item.getItemMeta().getLore();
        final int extralength = extralore.size();
        final int lorelength = lore.size();
        ItemStack check = item.clone();
        restoreLore(lore, extralength, lorelength, check);

        if (checkInventoryFull(player)) {
            Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.full_inv));
            return false;
        }


        try(Connection connection = MySQLManager.getInstance().getConneciton();
            PreparedStatement delete = connection.prepareStatement("DELETE FROM `"+Config.pre_remove_table+"` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `ItemStack`=? AND `Owner-Server`=? AND `TimeStamp`=?");
            PreparedStatement checker = connection.prepareStatement("SELECT `TimeStamp` FROM `"+Config.pre_remove_table+"` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `ItemStack`=? AND `Owner-Server`=?")){
            checker.setString(1,player.getName());
            checker.setString(2,player.getUniqueId().toString());
            checker.setString(3, ItemStringConvert.itemStackToBase64(check));
            checker.setString(4,Config.server);
            ResultSet resultSet = checker.executeQuery();
            if (resultSet.next()){
                long time = resultSet.getLong("TimeStamp");
                delete.setString(1,player.getName());
                delete.setString(2,player.getUniqueId().toString());
                delete.setString(3, ItemStringConvert.itemStackToBase64(check));
                delete.setString(4,Config.server);
                delete.setLong(5,time);
                delete.execute();
            }else return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        Bukkit.getScheduler().runTask(plugin,()->{
            player.getInventory().addItem(check);
            invremove.removeItem(item);
        });
        changed.put(player.getUniqueId(),true);
        removeRemoveItems(player,item);
        return true;
    }

    public boolean checkInventoryFull(Player player){
        ItemStack[] stacks = player.getInventory().getContents();
        if (stacks == null || stacks.length == 0) return false;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType() == Material.AIR) return false;
        }
        return true;
    }
}
