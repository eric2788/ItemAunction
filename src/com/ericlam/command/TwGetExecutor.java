package com.ericlam.command;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.inventory.constructors.ItemData;
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
import java.util.HashMap;
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
                    Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                        pages.add(new ArrayList<>());
                        LinkedHashMap<String, ItemData> map = marketManager.listItems(player);
                        int page = 0;
                        int i = 0;
                        if (map.size() == 0) return;
                        createPage(pages, map, page, i);
                            if (pages.size() == 0){
                                player.sendMessage(Config.empty);
                                return;
                            }
                        player.sendMessage(Config.list);
                        pages.get(0).forEach(player::sendMessage);
                        player.sendMessage(Config.list_remind);
                        player.sendMessage(Config.list_page.replace("<page>", 1 + "").replace("<max>", pages.size() + ""));
                    });
                    break;
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
                    if (item == null || item.getType() == Material.AIR) {
                        player.sendMessage(Config.no_exist);
                        player.sendMessage(Config.take_fail);
                        return;
                    }
                    if (player.getInventory().addItem(item).size() == 0) player.sendMessage(Config.take_success);
                    else player.sendMessage(Config.take_fail);
                });
                break;
            case "list":
                try{Integer.parseInt(strings[1]);}catch (NumberFormatException e){player.sendMessage(Config.not_number);}
                int getpage = Integer.parseInt(strings[1]);
                Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                    pages.add(new ArrayList<>());
                    LinkedHashMap<String, ItemData> map = marketManager.listItems(player);
                    int page = 0;
                    int i = 0;
                    if (map.size() == 0) return;
                    createPage(pages, map, page, i);
                        if (pages.size() < getpage || getpage <= 0){
                            player.sendMessage(Config.no_this_page);
                            return;
                        }
                    player.sendMessage(Config.list);
                    pages.get(getpage - 1).forEach(player::sendMessage);
                    player.sendMessage(Config.list_remind);
                    player.sendMessage(Config.list_page.replace("<page>", getpage + "").replace("<max>", pages.size() + ""));

                });
                break;
            default:
                player.sendMessage(Config.help);
                break;
        }
        return true;
    }

    private void createPage(List<List<String>> pages, HashMap<String, ItemData> map, int page, int i) {
        for (String item : map.keySet()){
            ItemData data = map.get(item);
            ItemStack stack = data.getItem();
            if (i % 5 == 0 && i > 0) {
                pages.add(new ArrayList<>());
                page++;
            }
            pages.get(page).add(Config.list_item
                    .replace("<num>", i + 1 + "")
                    .replace("<item-id>",item)
                    .replace("<material>",stack.getType().toString())
                    .replace("<amount>",stack.getAmount()+"")
                    .replace("<item-name>",(stack.getItemMeta().getDisplayName().isEmpty() ? stack.getType().toString().replace("_"," ").toLowerCase() : stack.getItemMeta().getDisplayName()))
                    .replace("<price>",data.getPrice()+"")
                    .replace("<date>",data.getTimestamp()));
            i++;
        }
    }
}
