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
import java.util.UUID;


public class OnPlayerEvent implements Listener {
    private Plugin plugin = ItemAunction.plugin;
    private HashMap<UUID, Boolean> changed = GUIInventory.getInstance().getChanged();
    private ItemStack pageitem;

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e){
        if (!Config.enable) return;
        Player player = e.getPlayer();

        MarketManager market = MarketManager.getInstance();
        GUIInventory gui = GUIInventory.getInstance();
        PreRemoveManager remove = PreRemoveManager.getInstance();

        gui.makeGUI(player);

        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
            if (!changed.containsKey(player.getUniqueId())) changed.put(player.getUniqueId(),false);
            gui.setPlayerItemsList(player,market.getTradeItems(player.getName()));
            gui.setPlayerItems(player,remove.getTradeItems(player.getName()));
            Bukkit.getScheduler().runTask(plugin,()->{
                if (gui.getBuyItemsMap(player) != null && gui.getBuyItemsMap(player).size() > 0) {
                    gui.addBuyItemsToGUI(player);
                }
                if (gui.getRemoveItems(player) != null && gui.getRemoveItems(player).size() > 0) {
                    gui.addRemoveItemsToGUI(player);
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
        Inventory local = e.getInventory();
        ItemStack itemStack = e.getCurrentItem();
        if (inventory == null || e.getSlotType() == InventoryType.SlotType.OUTSIDE) return;
        boolean haveItem = false;
        GUIInventory gui = GUIInventory.getInstance();
        if (gui.takeGUI(player).getBuy().contains(local)){
            e.setCancelled(true);
            for (Inventory inv : gui.takeGUI(player).getBuy()) {
                for (ItemStack stack : inv) {
                    if (itemStack.isSimilar(stack)) haveItem = true;
                }
            }
            if (itemStack.isSimilar(pageitem)) return;
            if (!haveItem) return;
            int page = gui.findPage(inventory,player,true);
            if (itemStack.isSimilar(gui.getNext())){
                if (gui.takeGUI(player).getBuy().size()-1 > page++){
                    player.closeInventory();
                    player.openInventory(gui.takeGUI(player).getBuy().get(++page));
                }
                else player.sendMessage(Config.no_next);
                return;
            }
            if (itemStack.isSimilar(gui.getPrevious())){
                if (gui.takeGUI(player).getBuy().size()-1 < page--){
                    player.closeInventory();
                    player.openInventory(gui.takeGUI(player).getBuy().get(--page));
                }
                else player.sendMessage(Config.no_previous);
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                boolean success = gui.buyItem(itemStack,player,inventory);
                player.sendMessage(success ? Config.take_success : Config.take_fail);
            });
        }

        if (gui.takeGUI(player).getRemove().contains(local)){
            e.setCancelled(true);
            for (Inventory inv : gui.takeGUI(player).getRemove()) {
                for (ItemStack stack : inv) {
                    if (itemStack.isSimilar(stack)) haveItem = true;
                }
            }
            if (itemStack.isSimilar(pageitem)) return;
            if (!haveItem) return;
            int page = gui.findPage(inventory,player,false);
            if (itemStack.isSimilar(gui.getNext())){
                if (gui.takeGUI(player).getRemove().size()-1 < page++){
                    player.closeInventory();
                    player.openInventory(gui.takeGUI(player).getRemove().get(++page));
                }
                else player.sendMessage(Config.no_next);
                return;
            }
            if (itemStack.isSimilar(gui.getPrevious())){
                if (gui.takeGUI(player).getRemove().size()-1 < page--){
                    player.closeInventory();
                    player.openInventory(gui.takeGUI(player).getRemove().get(--page));
                }
                else player.sendMessage(Config.no_previous);
                return;
            }
            Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{
                boolean success = gui.removeItem(itemStack,player,inventory);
                player.sendMessage(success ? Config.take_success : Config.take_fail);
            });
        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent e){
        if (!Config.enable) return;
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        Inventory inventory = e.getInventory();
        GUIInventory gui = GUIInventory.getInstance();
        if (!changed.get(player.getUniqueId())) return;
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
        pageitem = new ItemStack(Material.PAPER);
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
