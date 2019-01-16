package com.ericlam.mysql;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.inventory.constructors.BuyItems;
import com.ericlam.inventory.constructors.ItemData;
import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public class MarketManager {
    private static MarketManager manager;
    private MySQLManager mysql;

    public static MarketManager getInstance() {
        if (manager == null) manager = new MarketManager();
        return manager;
    }

    private Plugin plugin;
    private FileConfiguration config;

    private MarketManager(){
        plugin = ItemAunction.plugin;
        config = Config.getInstance().getConfig();
        mysql = MySQLManager.getInstance();
    }

    public LinkedHashMap<String, ItemData> listItems(Player player) {
        LinkedHashMap<String, ItemData> map = new LinkedHashMap<>();
        try (Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `Item-Name`,`ItemStack`,`NameID`,`Money`,`TimeStamp` FROM `" + Config.selltable + "` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=? ORDER BY `NameID`")) {
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3,Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                int money = resultSet.getInt("Money");
                long time = resultSet.getLong("TimeStamp");
                if (item == null){
                    plugin.getServer().getLogger().info("警告: 物品 \""+resultSet.getString("Item-Name")+"\" 已損壞，無法使用。");
                    continue;
                }
                map.put(resultSet.getString("NameID"),new ItemData(item,money,time));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    public boolean hasItem(String nameid){
        try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `NameID` FROM `"+Config.selltable+"` WHERE `NameID`=?")){
            statement.setString(1,nameid);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void oldPriceChecking(ItemStack item, Player player, int price) throws MoneyPriceException {
        try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `Money` FROM `"+Config.selltable+"` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `ItemStack`=?")){
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3,Config.server);
            statement.setString(4,ItemStringConvert.itemStackToBase64(item));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()){
                if (resultSet.getInt("Money") != price){
                    throw new MoneyPriceException(resultSet.getInt("Money")+"");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String genRandomUUID() {
        String uuid;
        while (true) {
            uuid = UUID.randomUUID().toString();
            if (!hasItem(uuid)) return uuid;
        }
    }

    public synchronized boolean uploadItem(ItemStack item, Player player, int price, String nameid) throws MoneyPriceException {
        oldPriceChecking(item, player, price);
        String base64 = ItemStringConvert.itemStackToBase64(item);
        int amount = item.getAmount();
        String itemname = item.getItemMeta().getDisplayName();
        Material material = item.getType();
        if (itemname == null || itemname.isEmpty()) itemname = material.toString().replace("_","").toLowerCase();
        String playername = player.getName();
        UUID playeruuid = player.getUniqueId();
        long timestamp = Timestamp.from(Instant.now()).getTime();
           try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("INSERT INTO `"+ Config.selltable+"` VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")){
               statement.setString(1,Config.server);
               statement.setString(2,playername);
               statement.setString(3,playeruuid.toString());
               statement.setString(4,itemname);
               statement.setString(5,material.toString());
               statement.setInt(6,amount);
               statement.setString(7,base64);
               statement.setInt(8,price);
               statement.setString(9,"");
               statement.setString(10,"");
               statement.setLong(11,timestamp);
               statement.setString(12,nameid);
               statement.execute();
               return true;
           } catch (SQLException e) {
               e.printStackTrace();
               return false;
           }
    }

    public ArrayList<BuyItems> getTradeItems(String PlayerName) {
        ArrayList<BuyItems> sqlItems = new ArrayList<>();
        ArrayList<ItemStack> repeats = new ArrayList<>();
        try (Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `NameID`,`Money`,`ItemStack`,`Item-Name` FROM `" + Config.selltable + "` WHERE `Trader-PlayerName`=? AND `Trader-Server`=?")) {
            statement.setString(1, PlayerName);
            statement.setString(2, Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                final ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                if (item == null) {
                    plugin.getServer().getLogger().info("警告: 物品 \"" + resultSet.getString("Item-Name") + "\" 已損壞，無法使用。");
                    continue;
                }
                final int price = resultSet.getInt("Money");
                int repeat = 0;
                final String nameId = resultSet.getString("NameID");
                List<String> extralore = new ArrayList<>();
                for (ItemStack itemre : repeats) {
                    if (itemre.isSimilar(item)) {
                        ++repeat;
                    }
                }
                repeats.add(item);
                for (String s : Config.getInstance().getInventory().getStringList("buy-inventory.extra-lore")) {
                    extralore.add(ChatColor.translateAlternateColorCodes('&', s
                            .replace("<money>", price + "")
                            .replace("<repeat>", (repeat == 0 ? "" : repeat + ""))));
                }
                List<String> lore = item.getItemMeta().getLore();
                ItemStack clone = item.clone();
                if (lore == null || lore.size() == 0) {
                    clone.setLore(extralore);
                } else {
                    lore.addAll(extralore);
                    clone.setLore(lore);
                }
                sqlItems.add(new BuyItems(price, nameId, clone));
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return sqlItems;
    }
    public boolean uploadItem(ItemStack item, Player player, int price, String target, String targetserver, String nameid) throws MoneyPriceException {
        if (hasItem(nameid)) return false;
        oldPriceChecking(item, player, price);
        String base64 = ItemStringConvert.itemStackToBase64(item);
        int amount = item.getAmount();
        String itemname = item.getItemMeta().getDisplayName();
        Material material = item.getType();
        if (itemname == null || itemname.isEmpty()) itemname = material.toString().replace("_","").toLowerCase();
        String playername = player.getName();
        UUID playeruuid = player.getUniqueId();
        long timestamp = Timestamp.from(Instant.now()).getTime();
            try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("INSERT INTO `"+ Config.selltable+"` VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")){
                statement.setString(1,Config.server);
                statement.setString(2,playername);
                statement.setString(3,playeruuid.toString());
                statement.setString(4,itemname);
                statement.setString(5,material.toString());
                statement.setInt(6,amount);
                statement.setString(7,base64);
                statement.setInt(8,price);
                statement.setString(9,targetserver);
                statement.setString(10,target);
                statement.setLong(11,timestamp);
                statement.setString(12,nameid);
                statement.execute();
                return true;
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
    }

    public ItemStack getBackItem(Player player,String nameid){
        try (Connection connection = MySQLManager.getInstance().getConneciton();
             PreparedStatement get = connection.prepareStatement("SELECT `ItemStack` FROM `" + Config.selltable + "` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `NameID`=?");
             PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + Config.selltable + "` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `NameID`=?")) {
            get.setString(1,player.getName());
            get.setString(2,player.getUniqueId().toString());
            get.setString(3,Config.server);
            get.setString(4,nameid);
            ResultSet resultSet = get.executeQuery();
            if (resultSet.next()){
               ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                delete.setString(1,player.getName());
                delete.setString(2,player.getUniqueId().toString());
                delete.setString(3,Config.server);
                delete.setString(4,nameid);
                delete.execute();
               if (item != null) return item;
                plugin.getServer().getLogger().info("警告: 物品 \""+resultSet.getString("Item-Name")+"\" 已損壞，無法使用。");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void updateTimeStamp(){
        HashSet<OfflinePlayer> players = new HashSet<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        Iterator<OfflinePlayer> playerIterator = players.iterator();
        new BukkitRunnable(){
            @Override
            public void run() {
                if (playerIterator.hasNext()){
                    OfflinePlayer offline = playerIterator.next();
                    try(Connection connection = MySQLManager.getInstance().getConneciton();
                        PreparedStatement check = connection.prepareStatement("SELECT `NameID`,`TimeStamp`,`Item-Name`,`Item-Material`,`ItemStack`,`Item-Amount` FROM `" + Config.selltable + "` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=?");
                        PreparedStatement preRemove = connection.prepareStatement("INSERT INTO `"+Config.pre_remove_table+"` VALUES (?,?,?,?,?,?,?,?)");
                        PreparedStatement delete = connection.prepareStatement("DELETE FROM `" + Config.selltable + "` WHERE (`Seller-PlayerName`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `NameID`=?")) {

                        check.setString(1,offline.getUniqueId().toString());
                        check.setString(2,offline.getName());
                        check.setString(3,Config.server);

                        delete.setString(1,offline.getName());
                        delete.setString(2,offline.getUniqueId().toString());
                        delete.setString(3,Config.server);

                        preRemove.setString(1,Config.server);
                        preRemove.setString(2,offline.getName());
                        preRemove.setString(3,offline.getUniqueId().toString());

                        ResultSet resultSet = check.executeQuery();
                        while (resultSet.next()){
                            String nameId = resultSet.getString("NameID");
                            String base64 = resultSet.getString("ItemStack");
                            String itemname = resultSet.getString("Item-Name");
                            String material = resultSet.getString("Item-Material");
                            int amount = resultSet.getInt("Item-Amount");
                            long time = resultSet.getLong("TimeStamp");
                            LocalDate now = LocalDate.now();
                            LocalDate first = new Timestamp(time).toLocalDateTime().toLocalDate();
                            Period period = Period.between(first,now);
                            if (period.getDays() >= config.getInt("expire-days")){
                                preRemove.setString(4,itemname);
                                preRemove.setString(5,material);
                                preRemove.setInt(6,amount);
                                preRemove.setString(7,base64);
                                preRemove.setLong(8,Timestamp.from(Instant.now()).getTime());
                                preRemove.execute();
                                delete.setString(4, nameId);
                                delete.execute();
                            }
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }else{
                    cancel();
                }
            }
        }.runTaskTimerAsynchronously(plugin,10L,100L);
    }

    public int getPrice(ItemStack item,Player player){
        try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `Money` FROM `"+Config.selltable+"` WHERE `Trader-PlayerName`=? AND `Trader-Server`=? AND `ItemStack`=?")){
            statement.setString(1,player.getName());
            statement.setString(2,Config.server);
            statement.setString(3,ItemStringConvert.itemStackToBase64(item));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()){
                return resultSet.getInt("Money");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}
