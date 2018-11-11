package com.ericlam.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.HashMap;

public class GUIInventory {
    private HashMap<Player, Inventory> playerInventory = new HashMap<>();
    private Inventory buyInventory;
    private Inventory preRemoveInventory;
}
