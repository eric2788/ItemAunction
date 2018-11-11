package com.ericlam.mysql;

import com.ericlam.config.Config;
import com.ericlam.main.ItemAunction;
import org.bukkit.Bukkit;
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

    public void updateTimeStamp(){
            HashSet<OfflinePlayer> players = new HashSet<>(Arrays.asList(Bukkit.getOfflinePlayers()));
            Iterator<OfflinePlayer> playerIterator = players.iterator();
            new BukkitRunnable(){
                @Override
                public void run() {
                    if (playerIterator.hasNext()){
                        OfflinePlayer offline = playerIterator.next();
                        try(Connection connection = MySQLManager.getInstance().getConneciton();
                            PreparedStatement check = connection.prepareStatement("SELECT `TimeStamp` FROM `"+ Config.pre_remove_table+"` WHERE `Owner-Name=?` OR `Owner-UUID`=? AND `Owner-Server`=?");
                            PreparedStatement delete = connection.prepareStatement("DELETE FROM `"+Config.pre_remove_table+"` WHERE `Owner-Name=?` OR `Owner-UUID`=? AND `Owner-Server`=?")){

                            check.setString(1,offline.getUniqueId().toString());
                            check.setString(2,offline.getName());
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
