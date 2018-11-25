package com.ericlam.inventory;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.inventory.constructors.BuyItems;
import com.ericlam.inventory.constructors.Inventories;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class GUIInventory {
    private HashMap<UUID, Inventories> playerInv = new HashMap<>();
    private HashMap<UUID, ArrayList<BuyItems>> playerItemsList = new HashMap<>();
    private HashMap<UUID, List<ItemStack>> playerItems = new HashMap<>();
    private HashMap<UUID, Boolean> changed = new HashMap<>();
    private FileConfiguration gui;
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

    public ArrayList<BuyItems> getBuyItemsMap(Player player) {
        return playerItemsList.get(player.getUniqueId());
    }

    private BuyItems getBuyItems(Player player, ItemStack itemStack){
        for (BuyItems buyItems : playerItemsList.get(player.getUniqueId())) {
            if (buyItems.getItem().isSimilar(itemStack)) return buyItems;
        }
        return null;
    }


    private void removeBuyItems(Player player, String nameId) {
        playerItemsList.get(player.getUniqueId()).removeIf(buyItems -> buyItems.getNameId().equals(nameId));
    }

    public List<ItemStack> getRemoveItems(Player player) {
        return playerItems.get(player.getUniqueId());
    }

    private void removeRemoveItems(Player player, ItemStack itemStack){
        playerItems.get(player.getUniqueId()).removeIf(item -> item.isSimilar(itemStack));
    }

    public void setPlayerItemsList(Player player, ArrayList<BuyItems> items) {
        if (items.size() == 0) return;
        if (playerItemsList.containsKey(player.getUniqueId()) && items.size() == playerItemsList.get(player.getUniqueId()).size())
            return;
        playerItemsList.put(player.getUniqueId(), items);
        changed.put(player.getUniqueId(),true);
    }

    public void setPlayerItems(Player player, List<ItemStack> item) {
        if (item.size() == 0) return;
        if (playerItems.containsKey(player.getUniqueId()) && item.size() == playerItems.get(player.getUniqueId()).size())
            return;
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
    }

    private Inventory getNewRemoveInv() {
        Inventory preRemoveInventory = Bukkit.createInventory(null, 54, gui.getString("pre-remove-inventory.title").replace('&', '§'));
        preRemoveInventory.setItem(45, previous);
        preRemoveInventory.setItem(53, next);
        return preRemoveInventory;
    }

    private Inventory getNewBuyInv() {
        Inventory buyInventory = Bukkit.createInventory(null, 54, gui.getString("buy-inventory.title").replace('&', '§'));
        buyInventory.setItem(45, previous);
        buyInventory.setItem(53, next);
        return buyInventory;
    }

    public void makeGUI(Player player){
        if (!playerInv.containsKey(player.getUniqueId())){
            playerInv.put(player.getUniqueId(), new Inventories(getNewBuyInv(), getNewRemoveInv()));
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
        playerInv.put(player.getUniqueId(), new Inventories(getNewBuyInv(), getNewRemoveInv()));
        List<ItemStack> items = getRemoveItems(player);
        if (items == null || items.size() == 0) return;
        List<Inventory> remove = takeGUI(player).getRemove();
        int page = 0;
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = items.get(i);
            if (i % 45 == 0 && i > 0){
                remove.add(getNewRemoveInv());
                page++;
            }
            remove.get(page).addItem(item);
        }
        takeGUI(player).setRemove(remove);
        changed.put(player.getUniqueId(),false);
    }

    public void addBuyItemsToGUI(Player player){
        if (!changed.get(player.getUniqueId())) return;
        playerInv.put(player.getUniqueId(), new Inventories(getNewBuyInv(), getNewRemoveInv()));
        ArrayList<BuyItems> items = getBuyItemsMap(player);
        if (items == null || items.size() == 0) return;
        List<Inventory> buy = takeGUI(player).getBuy();
        int i = 0;
        int page = 0;
        for (BuyItems buyItems : items) {
            if (i % 45 == 0 && i > 0){
                buy.add(getNewBuyInv());
                page++;
            }
            buy.get(page).addItem(buyItems.getItem());
            i++;
        }

        takeGUI(player).setBuy(buy);
        changed.put(player.getUniqueId(),false);
    }

    public boolean buyItem(ItemStack item, Player player,Inventory invbuy){
        final BuyItems buy = getBuyItems(player, item);
        if (buy == null) {
            return false;
        }
        final int price = buy.getPrice();
        final String nameId = buy.getNameId();
        if (!takeGUI(player).getBuy().contains(invbuy)) {
            return false;
        }
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(player.getUniqueId());
        if (ItemAunction.economy.getBalance(offlinePlayer) < price) {
            player.sendMessage(Config.no_money);
            return false;
        }
        List<String> extralore = gui.getStringList("buy-inventory.extra-lore").stream().map(txt -> ChatColor.translateAlternateColorCodes('&',txt)).collect(Collectors.toList());
        List<String> lore = item.getItemMeta().getLore();
        final int extralength = extralore.size();
        final int lorelength = lore.size();
        ItemStack check = item.clone();
        restoreLore(lore, extralength, lorelength, check);
        if (checkInventoryFull(player)) {
            player.sendMessage(Config.full_inv);
            return false;
        }

        try(Connection connection = MySQLManager.getInstance().getConneciton();
            PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + Config.selltable + "` WHERE  `NameID`=? AND `Trader-PlayerName`=? AND `Trader-Server`=?");
            PreparedStatement checker = connection.prepareStatement("SELECT `ItemStack` FROM `" + Config.selltable + "` WHERE `NameID`=? AND `Trader-PlayerName`=? AND `Trader-Server`=?")) {
            checker.setString(1, nameId);
            checker.setString(2,player.getName());
            checker.setString(3,Config.server);
            ResultSet resultSet = checker.executeQuery();
            if (resultSet.next()) {
                delete.setString(1, nameId);
                delete.setString(2, player.getName());
                delete.setString(3, Config.server);
                delete.execute();
            } else {
                player.sendMessage(Config.wait);
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (ItemAunction.economy.withdrawPlayer(offlinePlayer, price).type != EconomyResponse.ResponseType.SUCCESS) {
            player.sendMessage(Config.no_money);
            return false;
        }

        player.getInventory().addItem(check);
        invbuy.removeItem(item);

        changed.put(player.getUniqueId(),true);
        removeBuyItems(player, nameId);
        return true;
    }

    private void restoreLore(List<String> lore, int extralength, int lorelength, ItemStack check) {
            int i = lorelength-1;
            while (i > lorelength - extralength -1){
                lore.remove(i);
                i--;
            }
            check.setLore(lore);
    }

    public boolean removeItem(ItemStack item,Player player,Inventory invremove){
        if (!takeGUI(player).getRemove().contains(invremove)) return false;
        List<String> extralore = gui.getStringList("pre-remove-inventory.extra-lore");
        List<String> lore = item.getItemMeta().getLore();
        final int extralength = extralore.size();
        final int lorelength = lore.size();
        ItemStack check = item.clone();
        restoreLore(lore, extralength, lorelength, check);


        try(Connection connection = MySQLManager.getInstance().getConneciton();
            PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + Config.pre_remove_table + "` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `ItemStack`=? AND `Owner-Server`=?");
            PreparedStatement checker = connection.prepareStatement("SELECT `ItemStack` FROM `" + Config.pre_remove_table + "` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `ItemStack`=? AND `Owner-Server`=?")) {
            checker.setString(1,player.getName());
            checker.setString(2,player.getUniqueId().toString());
            checker.setString(3, ItemStringConvert.itemStackToBase64(check));
            checker.setString(4,Config.server);

            delete.setString(1, player.getName());
            delete.setString(2, player.getUniqueId().toString());

            ResultSet resultSet = checker.executeQuery();

            String sqlbase64 = "";
            int i = 0;
            while (resultSet.next()) {
                sqlbase64 = resultSet.getString("ItemStack");
                i++;
            }

            if (checkInventoryFull(player, i)) {
                player.sendMessage(Config.full_inv);
                return false;
            }

            if (i == 0) {
                player.sendMessage(Config.wait);
                return false;
            } else {
                if (sqlbase64.isEmpty()) return false;
                delete.setString(3, sqlbase64);
                delete.setString(4, Config.server);
                delete.execute();
            }

            for (int i1 = 0; i1 < i; i1++) {
                ItemStack itemStack = ItemStringConvert.itemStackFromBase64(sqlbase64);
                player.getInventory().addItem(itemStack);
                invremove.removeItem(item);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        removeRemoveItems(player, item);
        changed.put(player.getUniqueId(),true);
        return true;
    }

    public boolean checkInventoryFull(Player player){
        ItemStack[] stacks = player.getInventory().getStorageContents();
        if (stacks == null || stacks.length == 0) return false;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType() == Material.AIR) return false;
        }
        return true;
    }

    private boolean checkInventoryFull(Player player, int slots) {
        ItemStack[] stacks = player.getInventory().getStorageContents();
        if (stacks == null || stacks.length == 0) return false;
        int i = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.getType() == Material.AIR) i++;
        }
        return i < slots;
    }
}
