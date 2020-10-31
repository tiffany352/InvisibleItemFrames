package com.tiffnix.invisibleitemframes;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

class UpdateItemFrame extends BukkitRunnable {
    ItemFrame itemFrame;

    UpdateItemFrame(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }

    @Override
    public void run() {
        final ItemStack item = itemFrame.getItem();
        final boolean hasItem = item.getType() != Material.AIR;
        itemFrame.setVisible(!hasItem);
        System.out.println("has item: " + hasItem);
    }
}

public class PluginListener implements Listener {
    Location aboutToPlaceLocation = null;
    BlockFace aboutToPlaceFace = null;

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity().getType() != EntityType.ITEM_FRAME) {
            return;
        }
        final Location location = event.getBlock().getLocation();
        final BlockFace face = event.getBlockFace();
        System.out.println("Item frame placed at " + location + " : " + face);

        if (location.equals(aboutToPlaceLocation) && face == aboutToPlaceFace) {
            event.getEntity().getPersistentDataContainer().set(InvisibleItemFrames.KEY_IS_INVISIBLE, PersistentDataType.BYTE, (byte) 1);
            System.out.println("Added tag to item frame " + event.getEntity());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        final Block block = event.getClickedBlock();
        if (event.useInteractedBlock() == Event.Result.ALLOW && InvisibleItemFrames.isInvisibleItemFrame(item) && block != null) {
            aboutToPlaceLocation = block.getLocation();
            aboutToPlaceFace = event.getBlockFace();
            System.out.println("About to place at " + aboutToPlaceLocation + " : " + aboutToPlaceFace);
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Entity entity = event.getRightClicked();

        if (InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            final ItemFrame frame = (ItemFrame) entity;
            new UpdateItemFrame(frame).runTask(InvisibleItemFrames.INSTANCE);
        }
    }

    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();

        if (InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            final ItemFrame frame = (ItemFrame) entity;
            new UpdateItemFrame(frame).runTask(InvisibleItemFrames.INSTANCE);
        }
    }
}
