package com.ericlam.mysql;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.inventory.ItemData;
import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
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

    public static MarketManager getInstance() {
        if (manager == null) manager = new MarketManager();
        return manager;
    }

    private Plugin plugin;
    private FileConfiguration config;

    private MarketManager(){
        plugin = ItemAunction.plugin;
        config = Config.getInstance().getConfig();
    }

    public LinkedHashMap<String, ItemData> listItems(Player player){
        LinkedHashMap<String,ItemData> map = new LinkedHashMap<>();
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `ItemStack`,`NameID`,`Money`,`TimeStamp` FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=? ORDER BY `NameID`")){
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3,Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                int money = resultSet.getInt("Money");
                long time = resultSet.getInt("TimeStamp");
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

    public boolean hasItem(Player player,String nameid){
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `NameID` FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `NameID`=?")){
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3,Config.server);
            statement.setString(4,nameid);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void oldPriceChecking(ItemStack item, Player player, int price) throws MoneyPriceException {
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `Money` FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `ItemStack`=?")){
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

    public boolean uploadItem(ItemStack item, Player player, int price,String nameid) throws MoneyPriceException {
        if (hasItem(player,nameid)) return false;
        oldPriceChecking(item, player, price);
        String base64 = ItemStringConvert.itemStackToBase64(item);
        int amount = item.getAmount();
        String itemname = item.getItemMeta().getDisplayName();
        Material material = item.getType();
        String playername = player.getName();
        UUID playeruuid = player.getUniqueId();
        long timestamp = Timestamp.from(Instant.now()).getTime();
           try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("INSERT INTO `"+ Config.selltable+"` VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")){
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

    public boolean uploadItem(ItemStack item, Player player, int price, String target, String targetserver, String nameid) throws MoneyPriceException {
        if (hasItem(player,nameid)) return false;
        oldPriceChecking(item, player, price);
        String base64 = ItemStringConvert.itemStackToBase64(item);
        int amount = item.getAmount();
        String itemname = item.getItemMeta().getDisplayName();
        Material material = item.getType();
        String playername = player.getName();
        UUID playeruuid = player.getUniqueId();
        long timestamp = Timestamp.from(Instant.now()).getTime();
            try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("INSERT INTO `"+ Config.selltable+"` VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")){
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

    public HashMap<ItemStack, Integer> getTradeItems(String PlayerName){
        HashMap<ItemStack, Integer> itemsPrices = new HashMap<>();
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `Money`,`ItemStack`,`Item-Name` FROM `"+Config.selltable+"` WHERE `Trader-PlayerName`=? AND `Trader-Server`=?")){
            statement.setString(1,PlayerName);
            statement.setString(2,Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                int price = resultSet.getInt("Money");
                if (item == null){
                    plugin.getServer().getLogger().info("警告: 物品 \""+resultSet.getString("Item-Name")+"\" 已損壞，無法使用。");
                    continue;
                }
                itemsPrices.put(item,price);
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return itemsPrices;
    }

    public ItemStack getBackItem(Player player,String nameid){
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `ItemStack` FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=? AND `NameID`=?")){
            statement.setString(1,player.getName());
            statement.setString(2,player.getUniqueId().toString());
            statement.setString(3,Config.server);
            statement.setString(4,nameid);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()){
               ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
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
                        PreparedStatement check = connection.prepareStatement("SELECT `TimeStamp`,`Item-Name`,`Item-Material`,`ItemStack`,`Item-Amount` FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=?");
                        PreparedStatement preRemove = connection.prepareStatement("INSERT INTO `"+Config.pre_remove_table+"` VALUES (?,?,?,?,?,?,?,?)");
                        PreparedStatement delete = connection.prepareStatement("DELETE FROM `"+Config.selltable+"` WHERE (`Seller-Name`=? OR `Seller-UUID`=?) AND `Seller-Server`=?")){

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
        }.runTaskTimerAsynchronously(plugin,0L,100L);
    }

    public int getPrice(ItemStack item,Player player){
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `Money` FROM `"+Config.selltable+"` WHERE `Trader-PlayerName`=? AND `Trader-Server`=? AND `ItemStack`=?")){
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
