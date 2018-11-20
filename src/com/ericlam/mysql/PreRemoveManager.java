package com.ericlam.mysql;

import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
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

    public static PreRemoveManager getInstance() {
        if (manager == null) manager = new PreRemoveManager();
        return manager;
    }

    private PreRemoveManager(){
        plugin = ItemAunction.plugin;
        config = Config.getInstance().getConfig();
    }

    private HashMap<OfflinePlayer, List<ItemStack>> playerItems = new HashMap<>();

    public ItemStack[] getTradeItems(String PlayerName){
        List<ItemStack> items = new ArrayList<>();
        try(PreparedStatement statement = MySQLManager.getInstance().getConneciton().prepareStatement("SELECT `ItemStack`,`Item-Name` FROM `"+Config.pre_remove_table+"` WHERE `Owner-PlayerName`=? AND `Owner-Server`=?")){
            statement.setString(1,PlayerName);
            statement.setString(2,Config.server);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()){
                ItemStack item = ItemStringConvert.itemStackFromBase64(resultSet.getString("ItemStack"));
                if (item == null){
                    plugin.getServer().getLogger().info("警告: 物品 \""+resultSet.getString("Item-Name")+"\" 已損壞，無法使用。");
                    continue;
                }
                items.add(item);
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return items.toArray(new ItemStack[0]);
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
                                if (period.getDays() >= config.getInt("delay-expire-days")){
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
}
