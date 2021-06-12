package com.tiffnix.invisibleitemframes;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class InvisFramesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        return switch (args[0]) {
            case "reload" -> onReload(sender);
            case "grant" -> onGrant(sender, args);
            case "give" -> onGive(sender, args);
            default -> false;
        };
    }

    private boolean onReload(CommandSender sender) {
        InvisibleItemFrames.INSTANCE.reloadConfig();
        InvisibleItemFrames.INSTANCE.loadConfig();
        sender.sendMessage("Reloaded successfully.");

        return true;
    }

    private boolean onGrant(CommandSender sender, String[] args) {
        if (args.length > 2) {
            sender.sendMessage("Usage: /invisframes grant [player]");
            return true;
        }

        Player receiver = null;

        if (sender instanceof Player) {
            receiver = (Player) sender;
        }

        if (args.length > 1) {
            receiver = Bukkit.getPlayer(args[1]);
            if (receiver == null) {
                sender.sendMessage("Player could not be found.");
                return true;
            }
        }

        if (receiver == null) {
            sender.sendMessage("No player argument provided, and sender is not a player.");
            return true;
        }

        if (receiver.hasDiscoveredRecipe(InvisibleItemFrames.RECIPE_KEY) && receiver.hasDiscoveredRecipe(InvisibleItemFrames.GLOW_RECIPE_KEY)) {
            sender.sendMessage("Player " + receiver.getDisplayName() + " already has the recipes.");
            return true;
        }

        receiver.discoverRecipe(InvisibleItemFrames.RECIPE_KEY);
        receiver.discoverRecipe(InvisibleItemFrames.GLOW_RECIPE_KEY);
        sender.sendMessage("Granted recipes to " + receiver.getDisplayName());

        return true;
    }

    private boolean onGive(CommandSender sender, String[] args) {
        LivingEntity receiver = null;
        int count = 64;

        if (sender instanceof LivingEntity) {
            receiver = (LivingEntity) sender;
        }

        ItemStack itemToGive = InvisibleItemFrames.INVISIBLE_FRAME;

        switch (args.length) {
            default:
                sender.sendMessage("Usage: /invisframes give [regular|glow] [player] [count]");
                return true;
            case 4:
                count = Integer.parseInt(args[3]);
                // Intentional fallthrough
            case 3:
                receiver = Bukkit.getPlayer(args[2]);
                if (receiver == null) {
                    sender.sendMessage("Player could not be found.");
                    return true;
                }
                // Intentional fallthrough
            case 2:
                if (args[1].equals("regular")) {
                    itemToGive = InvisibleItemFrames.INVISIBLE_FRAME;
                } else if (args[1].equals("glow")) {
                    itemToGive = InvisibleItemFrames.INVISIBLE_GLOW_FRAME;
                } else {
                    sender.sendMessage("Item type must be either regular or glow.");
                    return true;
                }
                // Intentional fallthrough
            case 1:
                // Intentional fallthrough
                break;
        }

        if (receiver == null) {
            sender.sendMessage("No player argument provided, and sender is not alive.");
            return true;
        }

        giveItem(receiver, itemToGive, count);

        if (receiver != sender) {
            sender.sendMessage("Gave " + count + " invisible item frames to " + receiver.getName());
        }

        return true;
    }

    private void giveItem(LivingEntity receiver, ItemStack template, int amount) {
        final ItemStack stack = template.clone();
        stack.setAmount(amount);

        final Item entity = receiver.getWorld().dropItem(receiver.getEyeLocation(), stack);
        entity.setPickupDelay(0);
        entity.setThrower(receiver.getUniqueId());
        entity.setVelocity(receiver.getEyeLocation().getDirection().multiply(1.0f / 20.0f));
    }
}
