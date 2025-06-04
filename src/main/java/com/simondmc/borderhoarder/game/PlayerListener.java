package com.simondmc.borderhoarder.game;

import com.simondmc.borderhoarder.inventory.InventoryBuilder;
import com.simondmc.borderhoarder.world.BorderWorldCreator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.util.HashMap;
import java.util.Map;

public class PlayerListener implements Listener {

    @EventHandler
    public void cancelViewClick(InventoryClickEvent e) {
        if (e.getView().getTitle().contains("Collected Items§a")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            if (!e.getCurrentItem().hasItemMeta()) return;
            if (!e.getCurrentItem().getItemMeta().hasDisplayName()) return;
            // current page
            int page = Integer.parseInt(e.getView().getTitle().split("/")[0].split("-")[1].substring(6));
            if (e.getCurrentItem().getItemMeta().getDisplayName().equals("§aNext Page")) {
                e.getWhoClicked().openInventory(InventoryBuilder.buildCompletedInventory(page + 1));
            } else if (e.getCurrentItem().getItemMeta().getDisplayName().equals("§aPrevious Page")) {
                e.getWhoClicked().openInventory(InventoryBuilder.buildCompletedInventory(page - 1));
            }
        }
        if (e.getView().getTitle().contains("Missing Items§a")) {
            e.setCancelled(true);
            if (e.getCurrentItem() == null) return;
            if (!e.getCurrentItem().hasItemMeta()) return;
            if (!e.getCurrentItem().getItemMeta().hasDisplayName()) return;
            // current page
            int page = Integer.parseInt(e.getView().getTitle().split("/")[0].split("-")[1].substring(6));
            if (e.getCurrentItem().getItemMeta().getDisplayName().equals("§aNext Page")) {
                e.getWhoClicked().openInventory(InventoryBuilder.buildMissingInventory(page + 1));
            } else if (e.getCurrentItem().getItemMeta().getDisplayName().equals("§aPrevious Page")) {
                e.getWhoClicked().openInventory(InventoryBuilder.buildMissingInventory(page - 1));
            }
        }
    }

    // clear title if kicked during generation
    @EventHandler
    public void joinBorderWorld(PlayerJoinEvent e) {
        if (e.getPlayer().getWorld().getName().equals(BorderWorldCreator.worldName)) {
            e.getPlayer().resetTitle();
        }
    }

    // force respawn position if bed is destroyed
    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        // bed broken
        if ((e.getPlayer().getWorld().getName().equals(BorderWorldCreator.worldName)
                || e.getPlayer().getWorld().getName().equals(BorderWorldCreator.netherWorldName)
                || e.getPlayer().getWorld().getName().equals(BorderWorldCreator.endWorldName))
                && e.getPlayer().getBedSpawnLocation() == null) {
            e.setRespawnLocation(Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation());
        }
        // spawn obstructed
        if ((e.getPlayer().getWorld().getName().equals(BorderWorldCreator.worldName)
                || e.getPlayer().getWorld().getName().equals(BorderWorldCreator.netherWorldName)
                || e.getPlayer().getWorld().getName().equals(BorderWorldCreator.endWorldName))
                && e.getPlayer().getWorld().getBlockAt(e.getPlayer().getWorld().getSpawnLocation()).getType() != Material.AIR) {
            e.setRespawnLocation(Bukkit.getWorld(BorderWorldCreator.worldName).getHighestBlockAt(Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation()).getLocation().add(0.5, 1, 0.5));
        }
    }

    Map<Material, Material> saplings = new HashMap<>() {{
        put(Material.OAK_LEAVES, Material.OAK_SAPLING);
        put(Material.SPRUCE_LEAVES, Material.SPRUCE_SAPLING);
        put(Material.BIRCH_LEAVES, Material.BIRCH_SAPLING);
        put(Material.JUNGLE_LEAVES, Material.JUNGLE_SAPLING);
        put(Material.ACACIA_LEAVES, Material.ACACIA_SAPLING);
        put(Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING);
    }};

    // guarantee sapling and seed on first break
    @EventHandler
    public void breakBlock(BlockBreakEvent e) {
        if (!GameData.getBoolean("dropped-first-seed") &&
                e.getBlock().getWorld().getName().equals(BorderWorldCreator.worldName) &&
                e.getBlock().getType().equals(Material.SHORT_GRASS)) {
            e.setDropItems(false);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.WHEAT_SEEDS));
            GameData.set("dropped-first-seed", true);
        }
        if (!GameData.getBoolean("dropped-first-sapling") &&
                e.getBlock().getWorld().getName().equals(BorderWorldCreator.worldName) &&
                saplings.containsKey(e.getBlock().getType())) {
            e.setDropItems(false);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(saplings.get(e.getBlock().getType())));
            GameData.set("dropped-first-sapling", true);
        }
    }

    @EventHandler
public void onEntityPortal(EntityPortalEvent event) {
    Entity entity = event.getEntity();
    Location from = event.getFrom();
    String fromWorld = from.getWorld().getName();

    // Infer Nether portal use
    if (fromWorld.equals(BorderWorldCreator.worldName)) {
        // Going to Nether
        Location to = new Location(
            Bukkit.getWorld(BorderWorldCreator.netherWorldName),
            from.getBlockX() / 8.0,
            from.getBlockY(),
            from.getBlockZ() / 8.0
        );
        event.setTo(to);

        if (!GameData.getBoolean("nether-initialized")) {
            WorldBorder wb = Bukkit.getWorld(BorderWorldCreator.netherWorldName).getWorldBorder();
            wb.setCenter(to.clone().add(0.5, 0, 0.5));
            wb.setSize(ItemHandler.getCollectedItems().size() * 2 + 1);
            GameData.set("nether-initialized", true);
        }

    } else if (fromWorld.equals(BorderWorldCreator.netherWorldName)) {
        // Going to Overworld from Nether
        Location to = new Location(
            Bukkit.getWorld(BorderWorldCreator.worldName),
            from.getBlockX() * 8.0,
            from.getBlockY(),
            from.getBlockZ() * 8.0
        );
        event.setTo(to);

    } else if (fromWorld.equals(BorderWorldCreator.worldName)) {
        // Possibly entering the End
        Location to = new Location(Bukkit.getWorld(BorderWorldCreator.endWorldName), 100, 50, 0);
        event.setTo(to);

        Block centerBlock = to.getBlock();
        for (int x = centerBlock.getX() - 2; x <= centerBlock.getX() + 2; x++) {
            for (int z = centerBlock.getZ() - 2; z <= centerBlock.getZ() + 2; z++) {
                Block platformBlock = to.getWorld().getBlockAt(x, centerBlock.getY() - 1, z);
                if (platformBlock.getType() != Material.OBSIDIAN) {
                    platformBlock.setType(Material.OBSIDIAN);
                }
                for (int y = 1; y <= 3; y++) {
                    Block airBlock = platformBlock.getRelative(BlockFace.UP, y);
                    if (airBlock.getType() != Material.AIR) {
                        airBlock.setType(Material.AIR);
                    }
                }
            }
        }

    } else if (fromWorld.equals(BorderWorldCreator.endWorldName)) {
        // Returning from End
        event.setTo(Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation());
    }
}
}
