package com.tiffnix.invisibleitemframes;

import org.bukkit.Location;
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

public class PluginListener implements Listener {
    Location aboutToPlaceLocation = null;
    BlockFace aboutToPlaceFace = null;

    /**
     * Listens for an item frame entity being created from an item that was tagged
     * as invisible, so that the new entity can also be tagged.
     */
    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity().getType() != EntityType.ITEM_FRAME) {
            return;
        }
        final Location location = event.getBlock().getLocation();
        final BlockFace face = event.getBlockFace();

        if (location.equals(aboutToPlaceLocation) && face == aboutToPlaceFace) {
            event.getEntity().getPersistentDataContainer().set(InvisibleItemFrames.KEY_IS_INVISIBLE,
                    PersistentDataType.BYTE, (byte) 1);
        }
    }

    /**
     * Listens to player right clicking an item, because HangingPlaceEvent does not
     * say which item was used to create the hanging entity.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final ItemStack item = event.getItem();
        final Block block = event.getClickedBlock();
        if (event.useInteractedBlock() == Event.Result.ALLOW && InvisibleItemFrames.isInvisibleItemFrame(item)
                && block != null) {
            aboutToPlaceLocation = block.getLocation();
            aboutToPlaceFace = event.getBlockFace();
        }
    }

    /**
     * Whenever the player right clicks on an item frame, it potentially needs to
     * have its visibility updated.
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Entity entity = event.getRightClicked();

        if (InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            final ItemFrame frame = (ItemFrame) entity;
            new ItemFrameUpdateRunnable(frame).runTask(InvisibleItemFrames.INSTANCE);
        }
    }

    /**
     * When the player "damages" an item frame, the item it's holding is popped out.
     * So it potentially needs to have its visibility updated.
     */
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();

        if (InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            final ItemFrame frame = (ItemFrame) entity;
            new ItemFrameUpdateRunnable(frame).runTask(InvisibleItemFrames.INSTANCE);
        }
    }
}
