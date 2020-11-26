/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.tiffnix.invisibleitemframes;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;

public class PluginListener implements Listener {
    Location aboutToPlaceLocation = null;
    BlockFace aboutToPlaceFace = null;

    long hangingBrokenAtTick = -1;

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
            aboutToPlaceLocation = null;
            aboutToPlaceFace = null;
            event.getEntity().getPersistentDataContainer().set(InvisibleItemFrames.IS_INVISIBLE_KEY,
                    PersistentDataType.BYTE, (byte) 1);
        }
    }

    /**
     * Enforces the invisibleitemframes.break permission and stores a note of the
     * tick an item frame was broken in for when the item drop spawns. Since there's
     * no drops list on the event.
     */
    @EventHandler
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        final Hanging entity = event.getEntity();

        final boolean isFrame = InvisibleItemFrames.isInvisibleItemFrame(entity);
        Entity remover = event.getRemover();
        if (remover == null) {
            return;
        }

        // Treat projectiles shot by living beings as having the same permissions as
        // the entity that shot them.
        if (remover instanceof Projectile) {
            final Projectile projectile = (Projectile) remover;
            final ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof LivingEntity) {
                remover = (LivingEntity) shooter;
            }
        }

        final boolean hasPermission = remover.hasPermission("invisibleitemframes.break");
        if (isFrame && !hasPermission) {
            event.setCancelled(true);
            return;
        }

        if (isFrame) {
            hangingBrokenAtTick = entity.getWorld().getFullTime();
        }
    }

    /**
     * Since there's no connection between HangingBreakByEntityEvent and the items
     * created, this separate handler checks a saved value for whether to turn the
     * dropped item into an invisible item frame.
     */
    @EventHandler
    public void onItemSpawn(ItemSpawnEvent event) {
        final Item entity = event.getEntity();
        final ItemStack stack = entity.getItemStack();
        final long now = entity.getWorld().getFullTime();
        if (stack.getType() != Material.ITEM_FRAME || now != hangingBrokenAtTick) {
            return;
        }
        hangingBrokenAtTick = -1;

        stack.setItemMeta(InvisibleItemFrames.INVISIBLE_FRAME.getItemMeta());
        entity.setItemStack(stack);
    }

    /**
     * Listens to player right clicking an item, because HangingPlaceEvent does not
     * say which item was used to create the hanging entity.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        final ItemStack item = event.getItem();

        if (!InvisibleItemFrames.isInvisibleItemFrame(item) || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        final Player player = event.getPlayer();
        if (!player.hasPermission("invisibleitemframes.place")) {
            final String message = "You don't have permission to place these.";
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(message, ChatColor.RED));

            event.setUseItemInHand(Event.Result.DENY);
            // Item is consumed if useItemInHand is set to DENY, so cancel the event to
            // prevent that.
            event.setCancelled(true);
            return;
        }

        final Block block = event.getClickedBlock();
        if (block != null) {
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
        if (!InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            return;
        }

        final ItemFrame frame = (ItemFrame) entity;

        if (!event.getPlayer().hasPermission("invisibleitemframes.interact")) {
            event.setCancelled(true);
            return;
        }

        new ItemFrameUpdateRunnable(frame).runTask(InvisibleItemFrames.INSTANCE);
    }

    /**
     * When the player "damages" an item frame, the item it's holding is popped out.
     * So it potentially needs to have its visibility updated.
     */
    @EventHandler
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        final Entity entity = event.getEntity();
        if (!InvisibleItemFrames.isInvisibleItemFrame(entity)) {
            return;
        }

        final ItemFrame frame = (ItemFrame) entity;

        if (!event.getDamager().hasPermission("invisibleitemframes.interact")) {
            event.setCancelled(true);
            return;
        }

        new ItemFrameUpdateRunnable(frame).runTask(InvisibleItemFrames.INSTANCE);
    }

    /**
     * Spigot does not respect the doLimitedCrafting gamerule or allow permission
     * nodes for recipes, this re-adds that functionality.
     */
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        // event.getRecipe() is not the same object as the Recipe you pass to addRecipe.
        // So this ugly hack is required of checking the craft result for whether it's
        // the item to be crafted.
        if (!InvisibleItemFrames.isInvisibleItemFrame(event.getInventory().getResult())) {
            return;
        }

        if (!InvisibleItemFrames.INSTANCE.recipeEnabled) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        final HumanEntity entity = event.getView().getPlayer();

        if (!entity.hasPermission("invisibleitemframes.craft")) {
            event.getInventory().setResult(new ItemStack(Material.AIR));
            return;
        }

        final Boolean limitedCrafting = entity.getWorld().getGameRuleValue(GameRule.DO_LIMITED_CRAFTING);
        final boolean entityHasRecipe = entity.hasDiscoveredRecipe(InvisibleItemFrames.RECIPE_KEY);
        if (limitedCrafting == Boolean.TRUE && !entityHasRecipe) {
             event.getInventory().setResult(new ItemStack(Material.AIR));
        }
    }
}
