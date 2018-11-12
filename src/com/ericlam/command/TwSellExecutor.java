package com.ericlam.command;

import com.ericlam.config.Config;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MarketManager;
import com.ericlam.mysql.MoneyPriceException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class TwSellExecutor implements CommandExecutor {
    private final ItemAunction plugin;
    public TwSellExecutor(ItemAunction plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {

        // /twsell <money> <item-name> [target-name] [target-server]

        if (!Config.enable){
            commandSender.sendMessage("§c此插件已被禁用。 詳情請查看伺服器記錄.");
            return false;
        }


        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("you are not player");
            return false;
        }

        Player player = (Player) commandSender;
        FileConfiguration config = Config.getInstance().getConfig();

        if (strings.length < 2){
            commandSender.sendMessage(Config.few_arug);
            commandSender.sendMessage(Config.help);
            return false;
        }

        try{Integer.parseInt(strings[0]);} catch (NumberFormatException e) {
            player.sendMessage(Config.not_number);
            return false;
        }
        int money = Integer.parseInt(strings[0]);
        if (money > config.getInt("max-money")){
            player.sendMessage(Config.max_number);
            return false;
        }
        String itemname = strings[1];
        ItemStack item = player.getInventory().getItemInMainHand();
        MarketManager market = MarketManager.getInstance();

        if (item.getType() == Material.AIR){
            player.sendMessage(Config.air);
            return false;
        }

        if (strings.length == 2){
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                try {
                    if (market.hasItem(player, itemname)) {
                        boolean success = market.uploadItem(item, player, money, itemname);
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(success ? Config.upload_success : Config.upload_fail));
                    } else {
                        Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(Config.exist));
                    }
                }catch (MoneyPriceException e){
                    Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.money_not_same.replace("<price>",e.getMessage())));
                }
            });

            return true;
        }

        if (strings.length == 3){
            player.sendMessage(Config.help);
            return false;
        }

        if (strings.length > 4){
            player.sendMessage(Config.long_arug);
            return false;
        }

        String name = strings[2];
        String server = strings[3];
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            try {
                if (market.hasItem(player, itemname)) {
                    boolean success = market.uploadItem(item, player, money, name, server, itemname);
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(success ? Config.upload_success : Config.upload_fail));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> player.sendMessage(Config.exist));
                }
            }catch (MoneyPriceException e){
                Bukkit.getScheduler().runTask(plugin,()->player.sendMessage(Config.money_not_same.replace("<price>",e.getMessage())));
            }
        });
        return true;
    }
}
