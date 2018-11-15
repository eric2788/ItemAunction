package com.ericlam.listener;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.main.ItemAunction;
import com.ericlam.mysql.MarketManager;
import com.ericlam.mysql.PreRemoveManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;


public class OnPlayerEvent implements Listener {
    private Plugin plugin = ItemAunction.plugin;
    private HashMap<Player, Boolean> changed = GUIInventory.getInstance().getChanged();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if (!Config.enable) return;
        Player player = e.getPlayer();

        MarketManager market = MarketManager.getInstance();
        GUIInventory gui = GUIInventory.getInstance();
        PreRemoveManager remove = PreRemoveManager.getInstance();

        gui.makeGUI(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            gui.setPlayerItemsList(player,market.getTradeItems(player.getName()));
            gui.setPlayerItems(player,remove.getTradeItems(player.getName()));
            if (!changed.get(player)) return;
            Bukkit.getScheduler().runTask(plugin,()->{
                if (gui.getBuyItems(player).size() > 0){
                    gui.addBuyItemsToGUI(player);
                    changed.put(player,false);
                }
                if (gui.getRemoveItems(player).length > 0){
                    gui.addRemoveItemsToGUI(player);
                    changed.put(player,false);
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
        boolean haveItem = false;
        GUIInventory gui = GUIInventory.getInstance();

        if (gui.takeGUI(player).getBuy().contains(inventory)){

            for (Inventory inv : gui.takeGUI(player).getBuy()) {
                for (ItemStack stack : inv) {
                    if (itemStack.isSimilar(stack)) haveItem = true;
                }
            }
            if (!haveItem) return;
            int page = gui.findPage(inventory,player,true);
            if (itemStack.isSimilar(gui.getNext())){
                player.closeInventory();
                if (gui.takeGUI(player).getBuy().get(++page) != null)player.openInventory(gui.takeGUI(player).getBuy().get(++page));
                else player.sendMessage(Config.no_next);
                return;
            }
            if (itemStack.isSimilar(gui.getPrevious())){
                player.closeInventory();
                if (gui.takeGUI(player).getBuy().get(--page) != null)player.openInventory(gui.takeGUI(player).getBuy().get(--page));
                else player.sendMessage(Config.no_previous);
                return;
            }
            player.sendMessage(gui.buyItem(itemStack,player,gui.getBuyItems(player).get(itemStack),inventory) ? Config.take_success : Config.take_fail);
            e.setCancelled(true);
        }

        if (gui.takeGUI(player).getRemove().contains(inventory)){
            for (Inventory inv : gui.takeGUI(player).getRemove()) {
                for (ItemStack stack : inv) {
                    if (itemStack.isSimilar(stack)) haveItem = true;
                }
            }
            if (!haveItem) return;
            int page = gui.findPage(inventory,player,false);
            if (itemStack.isSimilar(gui.getNext())){
                player.closeInventory();
                if (gui.takeGUI(player).getRemove().get(++page) != null)player.openInventory(gui.takeGUI(player).getRemove().get(++page));
                else player.sendMessage(Config.no_next);
                return;
            }
            if (itemStack.isSimilar(gui.getPrevious())){
                player.closeInventory();
                if (gui.takeGUI(player).getRemove().get(--page) != null)player.openInventory(gui.takeGUI(player).getRemove().get(--page));
                else player.sendMessage(Config.no_previous);
                return;
            }
            player.sendMessage(gui.removeItem(itemStack,player,inventory) ? Config.take_success : Config.take_fail);
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e){
        if (!Config.enable) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        Inventory inventory = e.getInventory();
        GUIInventory gui = GUIInventory.getInstance();

        if (gui.takeGUI(player).getBuy().contains(inventory)){
            gui.addBuyItemsToGUI(player);
        }

        if (gui.takeGUI(player).getRemove().contains(inventory)){
            gui.addRemoveItemsToGUI(player);
        }
    }

    @EventHandler
    public void onOpenInventory(InventoryOpenEvent e){
        if (!Config.enable) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        Inventory inventory = e.getInventory();
        GUIInventory gui = GUIInventory.getInstance();
        ItemStack pageitem = new ItemStack(Material.PAPER);
        ItemMeta pageMeta = pageitem.getItemMeta();
        if (gui.takeGUI(player).getBuy().contains(inventory)){
            int page = gui.findPage(inventory,player,true);
            pageMeta.setDisplayName("§1§2§3§4§5§f頁 "+ ++page+" / "+gui.takeGUI(player).getBuy().size());
        }else if (gui.takeGUI(player).getRemove().contains(inventory)){
            int page = gui.findPage(inventory,player,false);
            pageMeta.setDisplayName("§1§2§3§4§5§6§f頁 "+ ++page+" / "+gui.takeGUI(player).getRemove().size());
        } else{
            return;
        }
        pageitem.setItemMeta(pageMeta);
        inventory.setItem(49,pageitem);
    }
}
