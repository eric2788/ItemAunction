package com.ericlam.main;

import com.ericlam.command.TwBuyExecutor;
import com.ericlam.command.TwGetExecutor;
import com.ericlam.command.TwSellExecutor;
import com.ericlam.config.Config;
import com.ericlam.converter.ItemStringConvert;
import com.ericlam.listener.OnPlayerEvent;
import com.ericlam.mysql.MarketManager;
import com.ericlam.mysql.MySQLManager;
import com.ericlam.mysql.PreRemoveManager;
import com.ericlam.plugin.CheckUpdate;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

public class ItemAunction extends JavaPlugin {
    public static Plugin plugin;
    public static Economy economy;

    @Override
    public void onEnable() {
        plugin = this;
        Config.getInstance();

        ConsoleCommandSender console = getServer().getConsoleSender();

        if (!Config.enable){
            console.sendMessage(ChatColor.RED + "由於你尚未在 config.yml 把 enabled 改成 true, 因此本插件並不會啟用。");
            return;
        }

        if (this.getServer().getPluginManager().isPluginEnabled("Vault")){
            RegisteredServiceProvider<Economy> rsp = this.getServer().getServicesManager().getRegistration(Economy.class);
            economy = rsp.getProvider();
        }else{
            console.sendMessage(ChatColor.RED+" 找不到Vault插件! 因此本插件並不會啟用。");
            Config.enable = false;
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
        this.getCommand("twget").setExecutor(new TwGetExecutor(this));

        this.getServer().getPluginManager().registerEvents(new OnPlayerEvent(),this);

        try(Connection connection = MySQLManager.getInstance().getConneciton();PreparedStatement sellTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `"+Config.selltable +"` (" +
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
                "`NameID` VARCHAR(255) PRIMARY KEY NOT NULL )");
            PreparedStatement preRemoveTable = connection.prepareStatement("CREATE TABLE IF NOT EXISTS `"+Config.pre_remove_table+"` (" +
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
            Config.enable = false;
            return;
        }

        console.sendMessage(ChatColor.GREEN+"拍賣插件已啟用。");
        console.sendMessage(ChatColor.AQUA+"作者: Eric Lam");

        Bukkit.getScheduler().runTaskTimerAsynchronously(this,()->{

            MarketManager.getInstance().updateTimeStamp();
            PreRemoveManager.getInstance().updateTimeStamp();

        },100L,Config.getInstance().getConfig().getInt("check-interval") * 60 * 20L);

        Bukkit.getScheduler().runTaskAsynchronously(this, CheckUpdate::new);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof ConsoleCommandSender) return false;
        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("twrm")) {
            if (!player.hasPermission("tw.admin")) return false;
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() == Material.AIR) {
                player.sendMessage(Config.air);
                return false;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                boolean success = testRemove(player, item);
                player.sendMessage(success ? Config.upload_success : Config.upload_fail);
                if (success) player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            });
        }
        return true;
    }

    private boolean testRemove(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        String name = meta.getDisplayName();
        int amount = item.getAmount();
        Material mt = item.getType();
        long now = Timestamp.from(Instant.now()).getTime();
        String base64 = ItemStringConvert.itemStackToBase64(item);
        try (Connection connection = MySQLManager.getInstance().getConneciton(); PreparedStatement statement = connection.prepareStatement("INSERT INTO `" + Config.pre_remove_table + "` VALUES (?,?,?,?,?,?,?,?)")) {
            statement.setString(1, Config.server);
            statement.setString(2, player.getName());
            statement.setString(3, player.getUniqueId().toString());
            statement.setString(4, (name.isEmpty() ? mt.toString().replace("_", " ").toLowerCase() : name));
            statement.setString(5, mt.toString());
            statement.setInt(6, amount);
            statement.setString(7, base64);
            statement.setLong(8, now);
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
