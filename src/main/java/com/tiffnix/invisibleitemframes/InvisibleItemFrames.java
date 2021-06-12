/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/. */

package com.tiffnix.invisibleitemframes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
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
    public static NamespacedKey IS_INVISIBLE_KEY;
    public static NamespacedKey RECIPE_KEY;
    public static NamespacedKey GLOW_RECIPE_KEY;
    public static ItemStack INVISIBLE_FRAME;
    public static ItemStack INVISIBLE_GLOW_FRAME;
    private static boolean firstLoad = true;

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
        return meta.getPersistentDataContainer().has(IS_INVISIBLE_KEY, PersistentDataType.BYTE);
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
        final EntityType type = entity.getType();
        if (type != EntityType.ITEM_FRAME && type != EntityType.GLOW_ITEM_FRAME) {
            return false;
        }
        return entity.getPersistentDataContainer().has(IS_INVISIBLE_KEY, PersistentDataType.BYTE);
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        IS_INVISIBLE_KEY = new NamespacedKey(this, "invisible");
        RECIPE_KEY = new NamespacedKey(this, "invisible_item_frame");
        GLOW_RECIPE_KEY = new NamespacedKey(this, "invisible_glow_item_frame");

        getServer().getPluginManager().registerEvents(new PluginListener(), this);

        final PluginCommand command = getServer().getPluginCommand("invisframes");
        assert command != null;
        command.setTabCompleter(new InvisFramesCompleter());
        command.setExecutor(new InvisFramesCommand());

        saveDefaultConfig();

        loadConfig();

        firstLoad = false;
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private ItemStack createItem(Material material, ConfigurationSection config) {
        if (!config.getBoolean("enabled")) {
            getLogger().info("Item " + config.getName() + " is disabled in the config");
            return null;
        }

        ItemStack item = new ItemStack(material, 1);

        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        meta.setDisplayName(config.getString("name"));
        meta.setLore(config.getStringList("lore"));
        meta.getPersistentDataContainer().set(IS_INVISIBLE_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);

        return item;
    }

    private void addRecipeFromConfig(NamespacedKey key, ConfigurationSection config, ItemStack item) {
        if (!config.getBoolean("enabled")) {
            getLogger().info("Recipe " + config.getName() + " is disabled in the config");
            return;
        }

        item = item.clone();
        item.setAmount(config.getInt("count"));
        ShapedRecipe recipe = new ShapedRecipe(key, item);
        List<String> shape = config.getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredients = config.getConfigurationSection("ingredients");
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

        try {
            Bukkit.addRecipe(recipe);
        } catch (IllegalStateException ignored) {
            if (firstLoad) {
                getLogger().severe("Failed to add recipe " + config.getName() + ". This is likely an issue in the config");
            } else {
                getLogger().warning("Failed to add recipe " + config.getName() + ", because Spigot doesn't support reloading recipes.");
            }
        }
    }

    public void loadConfig() {
        final FileConfiguration config = getConfig();

        boolean migrateLegacy = false;
        ConfigurationSection legacyItemSection = config.getConfigurationSection("item");
        if (config.contains("item", true) && legacyItemSection != null) {
            config.createSection("items.invisible_item_frame", legacyItemSection.getValues(true));
            config.set("items.invisible_item_frame.enabled", true);
            config.set("item", null);
            getLogger().info("Found legacy item section");
            migrateLegacy = true;
        }
        ConfigurationSection legacyRecipeSection = config.getConfigurationSection("recipe");
        if (config.contains("recipe", true) && legacyRecipeSection != null) {
            config.createSection("recipes.invisible_item_frame", legacyRecipeSection.getValues(true));
            config.set("recipe", null);
            getLogger().info("Found legacy recipe section");
            migrateLegacy = true;
        }

        if (migrateLegacy) {
            getLogger().info("Converting config to new format");
            saveConfig();
        }

        config.addDefault("items.invisible_item_frame.enabled", true);
        config.addDefault("items.invisible_item_frame.name", ChatColor.RESET + "Invisible Item Frame");

        config.addDefault("items.invisible_glow_item_frame.enabled", true);
        config.addDefault("items.invisible_glow_item_frame.name", ChatColor.RESET + "Invisible Glow Item Frame");

        config.addDefault("recipes.invisible_item_frame.enabled", true);
        config.addDefault("recipes.invisible_item_frame.count", 8);
        config.addDefault("recipes.invisible_item_frame.shape", Arrays.asList("FFF", "F F", "FFF"));
        config.addDefault("recipes.invisible_item_frame.ingredients.F", "minecraft:item_frame");

        config.addDefault("recipes.invisible_glow_item_frame.enabled", true);
        config.addDefault("recipes.invisible_glow_item_frame.count", 8);
        config.addDefault("recipes.invisible_glow_item_frame.shape", Arrays.asList("FFF", "F F", "FFF"));
        config.addDefault("recipes.invisible_glow_item_frame.ingredients.F", "minecraft:glow_item_frame");

        ConfigurationSection regularItem = config.getConfigurationSection("items.invisible_item_frame");
        assert regularItem != null;
        INVISIBLE_FRAME = createItem(Material.ITEM_FRAME, regularItem);

        ConfigurationSection glowItem = config.getConfigurationSection("items.invisible_glow_item_frame");
        assert glowItem != null;
        INVISIBLE_GLOW_FRAME = createItem(Material.GLOW_ITEM_FRAME, glowItem);

        ConfigurationSection regularRecipe = config.getConfigurationSection("recipes.invisible_item_frame");
        assert regularRecipe != null;
        addRecipeFromConfig(RECIPE_KEY, regularRecipe, INVISIBLE_FRAME);

        ConfigurationSection glowRecipe = config.getConfigurationSection("recipes.invisible_glow_item_frame");
        assert glowRecipe != null;
        addRecipeFromConfig(GLOW_RECIPE_KEY, glowRecipe, INVISIBLE_GLOW_FRAME);
    }
}
