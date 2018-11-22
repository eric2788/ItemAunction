package com.ericlam.config;

import com.ericlam.main.ItemAunction;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class Config {
    private static Config manager;
    private final FileConfiguration config;
    private final FileConfiguration msg;
    private final FileConfiguration inventory;

    public static String selltable;
    public static String pre_remove_table;

    public static String air;
    public static String upload_success;
    public static String few_arug;
    public static String long_arug;
    public static String[] help;
    public static String exist;
    public static String upload_fail;
    public static String take_success;
    public static String take_fail;
    public static String no_exist;
    public static String full_inv;
    public static String list;
    public static String list_item;
    public static String[] list_remind;
    public static String no_perm;
    public static String not_number;
    public static String max_number;
    public static String no_money;
    public static String money_not_same;
    public static String no_next;
    public static String no_previous;
    public static String wait;
    public static String list_page;
    public static String no_this_page;
    public static String empty;
    public static String name_long;

    public static boolean enable;

    public static int max_money;
    public static String server;

    private Config(){
        Plugin plugin = ItemAunction.plugin;
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) plugin.saveResource("config.yml", true);
        File msgFile = new File(plugin.getDataFolder(),"lang.yml");
        if (!msgFile.exists()) plugin.saveResource("lang.yml", true);
        File inventoryFile = new File(plugin.getDataFolder(),"inventory.yml");
        if (!inventoryFile.exists()) plugin.saveResource("inventory.yml",true);

        config = YamlConfiguration.loadConfiguration(configFile);
        msg = YamlConfiguration.loadConfiguration(msgFile);
        inventory = YamlConfiguration.loadConfiguration(inventoryFile);

        selltable = config.getString("MySQL.sell-table");
        pre_remove_table = config.getString("MySQL.pre-remove-table");

        String prefix = msg.getString("prefix");
        air = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("air"));
        upload_success = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("upload-success"));
        few_arug = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("few-arug"));
        long_arug = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("long-arug"));
        help = msg.getStringList("help").stream().map(help -> ChatColor.translateAlternateColorCodes('&',prefix+help)).toArray(String[]::new);
        exist = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("exist"));
        upload_fail = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("upload-fail"));
        take_success = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("take-success"));
        take_fail = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("take-fail"));
        no_exist = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("no-exist"));
        full_inv = ChatColor.translateAlternateColorCodes('&', prefix +msg.getString("full-inv"));
        list = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("list"));
        list_item = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("list-item"));
        list_remind = msg.getStringList("list-remind").stream().map(remind -> ChatColor.translateAlternateColorCodes('&',prefix+remind)).toArray(String[]::new);
        no_perm = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("no-perm"));
        not_number = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("not-number"));
        max_number = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("max-number"));
        no_money = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("no-money"));
        money_not_same = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("money-not-same"));
        no_next = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("no-next"));
        no_previous = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("no-previous"));
        wait = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("wait"));
        list_page = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("list-page"));
        no_this_page = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("no-this-page"));
        empty = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("empty"));
        name_long = ChatColor.translateAlternateColorCodes('&',prefix+msg.getString("name-long"));

        enable = config.getBoolean("enabled");

        max_money = config.getInt("max-money");
        server = config.getString("server");
    }

    public static Config getInstance() {
        if (manager == null) manager = new Config();
        return manager;
    }

    public final FileConfiguration getConfig() {
        return config;
    }

    public final FileConfiguration getMsg(){
        return msg;
    }

    public final FileConfiguration getInventory() {
        return inventory;
    }
}
