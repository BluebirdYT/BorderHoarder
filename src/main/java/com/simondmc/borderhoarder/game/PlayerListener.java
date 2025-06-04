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
public void onEntityPortal(EntityPortalEvent event) {
    Entity entity = event.getEntity();

    // Handle Nether Portal
    if (event.getCause() == TeleportCause.NETHER_PORTAL) {
        event.setCanCreatePortal(true);
        Location location;

        if (entity.getWorld().getName().equals(BorderWorldCreator.worldName)) {
            location = new Location(
                Bukkit.getWorld(BorderWorldCreator.netherWorldName),
                event.getFrom().getBlockX() / 8,
                event.getFrom().getBlockY(),
                event.getFrom().getBlockZ() / 8
            );

            if (!GameData.getBoolean("nether-initialized")) {
                WorldBorder wb = Bukkit.getWorld(BorderWorldCreator.netherWorldName).getWorldBorder();
                wb.setCenter(location.clone().add(0.5, 0, 0.5));
                wb.setSize(ItemHandler.getCollectedItems().size() * 2 + 1);
                GameData.set("nether-initialized", true);
            }
        } else if (entity.getWorld().getName().equals(BorderWorldCreator.netherWorldName)) {
            location = new Location(
                Bukkit.getWorld(BorderWorldCreator.worldName),
                event.getFrom().getBlockX() * 8,
                event.getFrom().getBlockY(),
                event.getFrom().getBlockZ() * 8
            );
        } else {
            return;
        }

        event.setTo(location);
    }

    // Handle End Portal
    if (event.getCause() == TeleportCause.END_PORTAL) {
        Location destination;

        if (entity.getWorld().getName().equals(BorderWorldCreator.worldName)) {
            destination = new Location(Bukkit.getWorld(BorderWorldCreator.endWorldName), 100, 50, 0);
            event.setTo(destination);

            // Generate obsidian platform at destination
            Block centerBlock = destination.getBlock();
            for (int x = centerBlock.getX() - 2; x <= centerBlock.getX() + 2; x++) {
                for (int z = centerBlock.getZ() - 2; z <= centerBlock.getZ() + 2; z++) {
                    Block platformBlock = destination.getWorld().getBlockAt(x, centerBlock.getY() - 1, z);
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
        } else if (entity.getWorld().getName().equals(BorderWorldCreator.endWorldName)) {
            destination = Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation();
            event.setTo(destination);
        }
    }
}
}
