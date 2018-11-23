package com.ericlam.mysql;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;

public class PreRemoveManager {
    private static PreRemoveManager manager;
    private Plugin plugin;
    private FileConfiguration config;
    private MySQLManager mysql;

    public static PreRemoveManager getInstance() {
        if (manager == null) manager = new PreRemoveManager();
        return manager;
    }

    private PreRemoveManager(){
        plugin = ItemAunction.plugin;
        config = Config.getInstance().getConfig();
        mysql = MySQLManager.getInstance();
    }

    public ItemStack[] getTradeItems(String PlayerName){
        List<ItemStack> items = new ArrayList<>();
        ItemStack[] st = new ItemStack[0];
        try(Connection connection = mysql.getConneciton(); PreparedStatement statement = connection.prepareStatement("SELECT `ItemStack`,`Item-Name` FROM `"+Config.pre_remove_table+"` WHERE `Owner-PlayerName`=? AND `Owner-Server`=?")){
            statement.setString(1,PlayerName);
            statement.setString(2,Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                if (item == null){
                    plugin.getServer().getLogger().info("警告: 物品 \""+resultSet.getString("Item-Name")+"\" 已損壞，無法使用。");
                    continue;
                }
                List<String> extralore = new ArrayList<>();
                for (String s : Config.getInstance().getInventory().getStringList("pre-remove-inventory.extra-lore")) {
                    extralore.add(ChatColor.translateAlternateColorCodes('&',s));
                }
                ItemStack clone = item.clone();
                List<String> lore = item.getItemMeta().getLore();
                if (lore == null || lore.size() == 0) {
                    clone.setLore(extralore);
                } else {
                    lore.addAll(extralore);
                    clone.setLore(lore);
                }
                items.add(clone);
            }
             st = items.toArray(new ItemStack[0]);
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return st;
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
                            PreparedStatement check = connection.prepareStatement("SELECT `TimeStamp` FROM `"+ Config.pre_remove_table+"` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `Owner-Server`=?");
                            PreparedStatement delete = connection.prepareStatement("DELETE FROM `"+Config.pre_remove_table+"` WHERE (`Owner-PlayerName`=? OR `Owner-UUID`=?) AND `Owner-Server`=?")){

                            check.setString(1,offline.getName());
                            check.setString(2,offline.getUniqueId().toString());
                            check.setString(3,Config.server);

                            delete.setString(1,offline.getName());
                            delete.setString(2,offline.getUniqueId().toString());
                            delete.setString(3,Config.server);

                            ResultSet resultSet = check.executeQuery();
                            while (resultSet.next()){
                                long time = resultSet.getLong("TimeStamp");
                                LocalDate now = LocalDate.now();
                                LocalDate first = new Timestamp(time).toLocalDateTime().toLocalDate();
                                Period period = Period.between(first,now);
                                if (period.getDays() >= config.getInt("remove-days")){
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
            }.runTaskTimerAsynchronously(plugin,20L,100L);
    }
}
