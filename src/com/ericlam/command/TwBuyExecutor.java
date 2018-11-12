package com.ericlam.command;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.main.ItemAunction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TwBuyExecutor implements CommandExecutor {
    private final ItemAunction plugin;
    public TwBuyExecutor(ItemAunction plugin){
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

        player.openInventory(gui.takeGUI(player).getBuy());

        return true;
    }
}
