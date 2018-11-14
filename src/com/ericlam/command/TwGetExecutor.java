package com.ericlam.command;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.inventory.ItemData;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MarketManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TwGetExecutor implements CommandExecutor {
    private final ItemAunction plugin;
    public TwGetExecutor(ItemAunction plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (!Config.enable){
            commandSender.sendMessage("§c此插件已被禁用。 詳情請查看伺服器記錄.");
            return false;
        }

        if (!(commandSender instanceof Player)){
            commandSender.sendMessage("you are not player");
            return false;
        }

        Player player = (Player) commandSender;
        GUIInventory gui = GUIInventory.getInstance();
        MarketManager marketManager = MarketManager.getInstance();

        List<List<String>> pages = new ArrayList<>();



        if(strings.length < 1){
            player.sendMessage(Config.few_arug);
            player.sendMessage(Config.help);
            return true;
        }
        if (strings[0].equalsIgnoreCase("list")){
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                pages.add(new ArrayList<>());
                LinkedHashMap<String, ItemData> map = marketManager.listItems(player);
                int page = 0;
                int i = 0;
                for (String item : map.keySet()){
                    ItemData data = map.get(item);
                    ItemStack stack = data.getItem();
                    if (i % 20 == 0) {
                        pages.add(new ArrayList<>());
                        page++;
                    }
                    pages.get(page).add(Config.list_item
                            .replace("<num>",i+"")
                            .replace("<item-id>",item)
                            .replace("<material>",stack.getType().toString())
                            .replace("<amount>",stack.getAmount()+"")
                            .replace("<item-name>",stack.getItemMeta().getDisplayName())
                            .replace("<price>",data.getPrice()+"")
                            .replace("<date>",data.getTimestamp()));
                    i++;
                }
            });
        }
        if (strings.length == 1){
            switch (strings[0]){
                case "remove":
                    if (gui.takeGUI(player).getRemove().get(0) == null){
                        player.sendMessage(Config.wait);
                        return false;
                    }
                    player.openInventory(gui.takeGUI(player).getRemove().get(0));
                    break;
                case "list":
                    player.sendMessage(Config.list);
                    pages.get(0).forEach(player::sendMessage);
                    player.sendMessage(Config.list_remind);
                    player.sendMessage(Config.list_page.replace("<page>",0+"").replace("<max>",pages.size()+""));
                default:
                    player.sendMessage(Config.help);
                    break;
            }

            return true;
        }
        switch (strings[0]){
            case "item":
                String itemname = strings[1];

                if (gui.checkInventoryFull(player)){
                    player.sendMessage(Config.full_inv);
                    return false;
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    ItemStack item = marketManager.getBackItem(player,itemname);
                    Bukkit.getScheduler().runTask(plugin,()->{
                        if (item == null || item.getType() == Material.AIR){
                            player.sendMessage(Config.no_exist);
                            player.sendMessage(Config.take_fail);
                            return;
                        }
                        if (player.getInventory().addItem(item).size() == 0) player.sendMessage(Config.take_success);
                        else player.sendMessage(Config.take_fail);
                    });
                });
                break;
            case "list":
                try{Integer.parseInt(strings[1]);}catch (NumberFormatException e){player.sendMessage(Config.not_number);}
                int page = Integer.parseInt(strings[1]);
                if (pages.get(page) == null){
                    player.sendMessage(Config.no_this_page);
                    return false;
                }
                player.sendMessage(Config.list);
                pages.get(page).forEach(player::sendMessage);
                player.sendMessage(Config.list_remind);
                player.sendMessage(Config.list_page.replace("<page>",page+"").replace("<max>",pages.size()+""));
                break;
            default:
                player.sendMessage(Config.help);
                break;
        }


        return true;
    }
}
