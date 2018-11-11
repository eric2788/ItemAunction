package com.ericlam.command;

import com.ericlam.main.ItemAunction;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TwGetExecutor implements CommandExecutor {
    private final ItemAunction plugin;
    public TwGetExecutor(ItemAunction plugin){
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        return false;
    }
}
