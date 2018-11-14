package com.ericlam.listener;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MarketManager;
import com.ericlam.mysql.PreRemoveManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;

public class OnPlayerEvent implements Listener {
    private Plugin plugin = ItemAunction.plugin;
    private HashMap<Player, HashMap<ItemStack,Integer>> playerItemsList = new HashMap<>();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if (!Config.enable) return;
        Player player = e.getPlayer();

        MarketManager market = MarketManager.getInstance();
        GUIInventory gui = GUIInventory.getInstance();
        PreRemoveManager remove = PreRemoveManager.getInstance();

        gui.makeGUI(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            playerItemsList.put(player,market.getTradeItems(player.getName()));
            ItemStack[] removeItems = remove.getTradeItems(player.getName());
            Bukkit.getScheduler().runTask(plugin,()->{
                if (playerItemsList.get(player).size() > 0){
                    gui.addItemsToGUI(playerItemsList.get(player),player);
                }
                if (removeItems.length > 0){
                    gui.addItemsToGUI(removeItems,player);
                }
            });
        });
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        if (!Config.enable) return;
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        Inventory inventory = e.getClickedInventory();
        ItemStack itemStack = e.getCurrentItem();
        if (inventory == null || e.getSlotType() == InventoryType.SlotType.OUTSIDE) return;

        GUIInventory gui = GUIInventory.getInstance();

        if (inventory.getName().equals(gui.takeGUI(player).getBuy().getName())){
            if (!gui.takeGUI(player).getBuy().contains(itemStack)) return;
            player.sendMessage(gui.buyItem(itemStack,player,playerItemsList.get(player).get(itemStack)) ? Config.take_success : Config.take_fail);
            e.setCancelled(true);
        }

        if (inventory.getName().equals(gui.takeGUI(player).getRemove().getName())){
            if (!gui.takeGUI(player).getRemove().contains(itemStack)) return;
            player.sendMessage(gui.removeItem(itemStack,player) ? Config.take_success : Config.take_fail);
            e.setCancelled(true);
        }
    }
}
