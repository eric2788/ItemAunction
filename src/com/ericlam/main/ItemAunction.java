package com.ericlam.main;

import com.ericlam.command.TwBuyExecutor;
import com.ericlam.command.TwSellExecutor;
import com.ericlam.config.Config;
import com.ericlam.listener.OnPlayerEvent;
import com.ericlam.mysql.MySQLManager;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemAunction extends JavaPlugin {
    public static Plugin plugin;

    @Override
    public void onEnable() {
        plugin = this;
        Config.getInstance();

        ConsoleCommandSender console = getServer().getConsoleSender();

        if (!Config.enable){
            console.sendMessage(ChatColor.RED + "由於你尚未在 config.yml 把 enabled 改成 true, 因此本插件並不會啟用。");
            return;
        }

        if (Config.server == null || Config.server.isEmpty()){
            console.sendMessage(ChatColor.RED + "伺服器ID是空的! 因此此插件並不會啟用。");
            Config.enable = false;
            return;
        }

        if (Config.max_money >= 2147483647){
            console.sendMessage(ChatColor.RED + "最大金錢允許數超出許可範圍的數值! 因此此插件並不會啟用");
            Config.enable = false;
            return;
        }

        this.getCommand("twsell").setExecutor(new TwSellExecutor(this));
        this.getCommand("twbuy").setExecutor(new TwBuyExecutor(this));

        this.getServer().getPluginManager().registerEvents(new OnPlayerEvent(),this);

        try(PreparedStatement sellTable = MySQLManager.getInstance().getConneciton().prepareStatement("CREATE TABLE IF NOT EXISTS `"+Config.selltable +"` (" +
                "`Seller-Server` TINYTEXT NOT NULL, " +
                "`Seller-PlayerName` TINYTEXT NOT NULL, " +
                "`Seller-UUID` VARCHAR(40) NOT NULL," +
                "`Item-Name` TINYTEXT NOT NULL," +
                "`Item-Material` TINYTEXT NOT NULL," +
                "`Item-Amount` TINYINT NOT NULL, "+
                "`ItemStack` LONGTEXT NOT NULL, " +
                "`Money` INT NOT NULL,"+
                "`Trader-Server` TINYTEXT NOT NULL," +
                "`Trader-PlayerName` TINYTEXT NOT NULL," +
                "`TimeStamp` BIGINT NOT NULL," +
                "`NameID` TINYINT NOT NULL )");
            PreparedStatement preRemoveTable = MySQLManager.getInstance().getConneciton().prepareStatement("CREATE TABLE IF NOT EXISTS `"+Config.pre_remove_table+"` (" +
                    "`Owner-Server` TINYTEXT NOT NULL," +
                    "`Owner-PlayerName` TINYTEXT NOT NULL," +
                    "`Owner-UUID` VARCHAR(40) NOT NULL," +
                    "`Item-Name` TINYTEXT NOT NULL," +
                    "`Item-Maerial` TINYTEXT NOT NULL," +
                    "`Item-Amount` TINYINT NOT NULL, "+
                    "`ItemStack` LONGTEXT NOT NULL," +
                    "`TimeStamp` BIGINT NOT NULL)")){
            sellTable.execute();
            preRemoveTable.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {

    }
}
