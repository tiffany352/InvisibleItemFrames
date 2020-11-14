/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.tiffnix.invisibleitemframes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class InvisibleItemFrames extends JavaPlugin {
    public static InvisibleItemFrames INSTANCE;
    public static NamespacedKey KEY_IS_INVISIBLE;
    public static ItemStack INVISIBLE_FRAME;

    /**
     * Returns whether the given ItemStack is an invisible item frame item.
     *
     * @param item The stack to test.
     * @return Whether the item stack is an invisible item frame item.
     */
    public static boolean isInvisibleItemFrame(ItemStack item) {
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(KEY_IS_INVISIBLE, PersistentDataType.BYTE);
    }

    /**
     * Returns whether the given Entity is an ItemFrame which should become
     * invisible when it has an item.
     *
     * @param entity The entity to test.
     * @return Whether it is an invisible item frame entity.
     */
    public static boolean isInvisibleItemFrame(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity.getType() != EntityType.ITEM_FRAME) {
            return false;
        }
        return entity.getPersistentDataContainer().has(KEY_IS_INVISIBLE, PersistentDataType.BYTE);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        KEY_IS_INVISIBLE = new NamespacedKey(this, "invisible");

        getServer().getPluginManager().registerEvents(new PluginListener(), this);

        saveDefaultConfig();

        loadConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void loadConfig() {
        final FileConfiguration config = getConfig();

        config.addDefault("item.name", ChatColor.RESET + "Invisible Item Frame");
        config.addDefault("recipe.count", 8);
        config.addDefault("recipe.shape", Arrays.asList("FFF", "F F", "FFF"));
        config.addDefault("recipe.ingredients.F", "minecraft:item_frame");

        // Create the actual item that should be used.
        ItemStack item = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(config.getString("item.name"));
        meta.setLore(config.getStringList("item.lore"));
        meta.getPersistentDataContainer().set(KEY_IS_INVISIBLE, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        item.setAmount(config.getInt("recipe.count"));
        INVISIBLE_FRAME = item;

        // Register the crafting recipe.
        NamespacedKey key = new NamespacedKey(this, "invisible_item_frame");
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        List<String> shape = config.getStringList("recipe.shape");
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredients = config.getConfigurationSection("recipe.ingredients");
        // If this is null, then the defaults above are incorrect.
        assert ingredients != null;
        for (Map.Entry<String, Object> entry : ingredients.getValues(false).entrySet()) {
            Material material = Material.matchMaterial(entry.getValue().toString());
            if (material == null) {
                getLogger()
                        .severe("Failed to find material " + entry.getValue().toString() + ", recipe might not work.");
                continue;
            }
            recipe.setIngredient(entry.getKey().charAt(0), material);
        }
        Bukkit.addRecipe(recipe);
    }
}
