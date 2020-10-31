/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.tiffnix.invisibleitemframes;

import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Updates visibility of an item frame in the next game tick. This is queued up
 * so that the item frame's item is up to date, which it hasn't been changed yet
 * while we're still processing events.
 */
class ItemFrameUpdateRunnable extends BukkitRunnable {
    ItemFrame itemFrame;

    ItemFrameUpdateRunnable(ItemFrame itemFrame) {
        this.itemFrame = itemFrame;
    }

    @Override
    public void run() {
        final ItemStack item = itemFrame.getItem();
        final boolean hasItem = item.getType() != Material.AIR;
        itemFrame.setVisible(!hasItem);
    }
}
