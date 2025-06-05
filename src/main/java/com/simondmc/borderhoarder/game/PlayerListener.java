package com.simondmc.borderhoarder.game;

import com.simondmc.borderhoarder.inventory.InventoryBuilder;
import com.simondmc.borderhoarder.world.BorderWorldCreator;
import org.bukkit.*;
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
import org.bukkit.event.entity.EntityPortalEvent;

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
        Player player = e.getPlayer();
        World deathLoc = player.getWorld();
        if (deathLoc == Bukkit.getWorld(BorderWorldCreator.worldName) || deathLoc == Bukkit.getWorld(BorderWorldCreator.netherWorldName) || deathLoc == Bukkit.getWorld(BorderWorldCreator.endWorldName)) {
            if (player.getRespawnLocation() == null) {
                e.setRespawnLocation(Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation().add(0.5, 0, 0.5));
                if (e.getPlayer().getWorld().getBlockAt(e.getPlayer().getWorld().getSpawnLocation().add(0, 1, 0)).getType() != Material.AIR) {
                    e.setRespawnLocation(Bukkit.getWorld(BorderWorldCreator.worldName).getHighestBlockAt(Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation()).getLocation().add(0.5, 1, 0.5));
                }
            }
        }
    }

    Map<Material, Material> saplings = new HashMap<>() {{
        put(Material.OAK_LEAVES, Material.OAK_SAPLING);
        put(Material.SPRUCE_LEAVES, Material.SPRUCE_SAPLING);
        put(Material.BIRCH_LEAVES, Material.BIRCH_SAPLING);
        put(Material.JUNGLE_LEAVES, Material.JUNGLE_SAPLING);
        put(Material.ACACIA_LEAVES, Material.ACACIA_SAPLING);
        put(Material.DARK_OAK_LEAVES, Material.DARK_OAK_SAPLING);
        put(Material.MANGROVE_LEAVES, Material.MANGROVE_PROPAGULE);
        put(Material.CHERRY_LEAVES, Material.CHERRY_SAPLING);
        put(Material.PALE_OAK_LEAVES, Material.PALE_OAK_SAPLING);
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
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        if (from.getWorld() == Bukkit.getWorld(BorderWorldCreator.worldName) || from.getWorld() == Bukkit.getWorld(BorderWorldCreator.netherWorldName) || from.getWorld() == Bukkit.getWorld(BorderWorldCreator.endWorldName)) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
                event.setCanCreatePortal(true);
                if (player.getWorld() == Bukkit.getWorld(BorderWorldCreator.worldName)) {
                    to.setWorld(Bukkit.getWorld(BorderWorldCreator.netherWorldName));
                    checkForFirstNetherEnter(to);
                } else {
                    to.setWorld(Bukkit.getWorld(BorderWorldCreator.worldName));
                }
            } else if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
                if (player.getWorld() == Bukkit.getWorld(BorderWorldCreator.worldName)) {
                    generateEndPlatform();
                    to = new Location(Bukkit.getWorld(BorderWorldCreator.endWorldName), 100.5, 49, 0.5, 90, to.getPitch());
                }
            }
        }
        event.setTo(to);
    }

    @EventHandler
    public void onEntityPortal(EntityPortalEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getWorld() == Bukkit.getWorld(BorderWorldCreator.worldName) || from.getWorld() == Bukkit.getWorld(BorderWorldCreator.netherWorldName) || from.getWorld() == Bukkit.getWorld(BorderWorldCreator.endWorldName)) {
            if (to.getWorld() == Bukkit.getWorld("world_nether")) {
                event.setCanCreatePortal(true);
                to.setWorld(Bukkit.getWorld(BorderWorldCreator.netherWorldName)); // overwrite the world to be the game one
                checkForFirstNetherEnter(to);
            } else if (to.getWorld() == Bukkit.getWorld("world_the_end")) {
                to = new Location(Bukkit.getWorld(BorderWorldCreator.endWorldName), 100.5, 49, 0.5, to.getYaw(), to.getPitch());
                generateEndPlatform();
            } else if (to.getWorld() == Bukkit.getWorld("world")) {
                if (from.getWorld() == Bukkit.getWorld(BorderWorldCreator.netherWorldName)) {
                    event.setCanCreatePortal(true);
                    to.setWorld(Bukkit.getWorld(BorderWorldCreator.worldName));
                } else if (from.getWorld() == Bukkit.getWorld(BorderWorldCreator.endWorldName)) {
                    to = Bukkit.getWorld(BorderWorldCreator.worldName).getSpawnLocation();
                }
            } else return;
        }
        event.setTo(to);
    }

    private void generateEndPlatform() {
        Location loc = new Location(Bukkit.getWorld(BorderWorldCreator.endWorldName), 100, 49, 0);
        Block block = loc.getBlock();
        for (int x = block.getX() - 2; x <= block.getX() + 2; x++) {
            for (int z = block.getZ() - 2; z <= block.getZ() + 2; z++) {
                Block platformBlock = loc.getWorld().getBlockAt(x, block.getY() - 1, z);
                if (platformBlock.getType() != Material.OBSIDIAN) {
                    platformBlock.breakNaturally();
                    platformBlock.setType(Material.OBSIDIAN);
                }
                for (int yMod = 1; yMod <= 3; yMod++) {
                    Block b = platformBlock.getRelative(BlockFace.UP, yMod);
                    if (b.getType() != Material.AIR) {
                        b.breakNaturally();
                        b.setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void checkForFirstNetherEnter(Location location) {
        if (!GameData.getBoolean("nether-initialized")) {
            WorldBorder wb = Bukkit.getWorld(BorderWorldCreator.netherWorldName).getWorldBorder();
            wb.setCenter(location.clone().add(0.5, 0, 0.5));
            wb.setSize(ItemHandler.getCollectedItems().size() * 2 + 1);
            GameData.set("nether-initialized", true);
        }
    }
}
