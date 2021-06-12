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

        if (receiver.hasDiscoveredRecipe(InvisibleItemFrames.RECIPE_KEY)) {
            sender.sendMessage("Player " + receiver.getDisplayName() + " already has this recipe.");
            return true;
        }

        receiver.discoverRecipe(InvisibleItemFrames.RECIPE_KEY);
        sender.sendMessage("Granted recipe to " + receiver.getDisplayName());

        return true;
    }

    private boolean onGive(CommandSender sender, String[] args) {
        LivingEntity receiver = null;
        int count = 64;

        if (sender instanceof LivingEntity) {
            receiver = (LivingEntity) sender;
        }

        switch (args.length) {
            default:
                sender.sendMessage("Usage: /invisframes give [player] [count]");
                return true;
            case 3:
                count = Integer.parseInt(args[2]);
                // Intentional fallthrough
            case 2:
                receiver = Bukkit.getPlayer(args[1]);
                if (receiver == null) {
                    sender.sendMessage("Player could not be found.");
                    return true;
                }
                // Intentional fallthrough
            case 1:
                break;
        }

        if (receiver == null) {
            sender.sendMessage("No player argument provided, and sender is not alive.");
            return true;
        }

        giveItem(receiver, InvisibleItemFrames.INVISIBLE_FRAME, count);

        if (receiver != sender) {
            sender.sendMessage("Gave " + count + " invisible item frames to " + receiver.getName());
        }

        return true;
    }

    private void giveItem(LivingEntity receiver, ItemStack template, int amount) {
        final ItemStack stack = template.clone();
        stack.setAmount(amount);

        final Item entity = (Item) receiver.getWorld().spawnEntity(receiver.getEyeLocation(), EntityType.DROPPED_ITEM);
        entity.setItemStack(stack);
        entity.setPickupDelay(0);
        entity.setThrower(receiver.getUniqueId());
        entity.setVelocity(receiver.getEyeLocation().getDirection().multiply(1.0f / 20.0f));
    }
}
