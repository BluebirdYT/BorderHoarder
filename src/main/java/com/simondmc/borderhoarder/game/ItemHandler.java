package com.simondmc.borderhoarder.game;

import com.simondmc.borderhoarder.BorderHoarder;
import com.simondmc.borderhoarder.data.DataHandler;
import com.simondmc.borderhoarder.world.BorderExpander;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.*;

public class ItemHandler {

    private static final Map<Material, Player> collectedItems = new LinkedHashMap<>();

    public static void gainItem(Material itemType, Player p) {
        if (itemType == null) return;
        if (!ItemDictionary.getDict().containsKey(itemType)) return;
        if (!collectedItems.containsKey(itemType)) {
            // add to list of collected items
            collectedItems.put(itemType, p);
            // announce to players
            for (Player recipient : BorderHoarder.plugin.getServer().getOnlinePlayers()) {
                recipient.sendMessage("§aYou collected a " + ItemDictionary.getDict().get(itemType) + "!");
                recipient.playSound(recipient.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
            // expand border
            BorderExpander.expandBorder();
            // save data
            DataHandler.saveData();
            // update tab
            TabHandler.updateTab();
        }
    }

    public static Map<Material, Player> getCollectedItems() {
        return collectedItems;
    }

    public static void setCollectedItems(List<String> items, List<String> players) {
        collectedItems.clear();
        for (int i = 0; i < items.size(); i++) {
            collectedItems.put(Material.valueOf(items.get(i)), BorderHoarder.plugin.getServer().getPlayer(UUID.fromString(players.get(i))));
        }
    }

    public static void resetCollectedItems() {
        collectedItems.clear();
        DataHandler.saveData();
    }
}
