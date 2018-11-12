package com.ericlam.command;

import com.ericlam.config.Config;
import com.ericlam.inventory.GUIInventory;
import com.ericlam.main.ItemAunction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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

        if(strings.length < 1){
            player.sendMessage(Config.few_arug);
            player.sendMessage(Config.help);
            return false;
        }


        player.openInventory(gui.takeGUI(player).getRemove());

        return true;
    }
}
