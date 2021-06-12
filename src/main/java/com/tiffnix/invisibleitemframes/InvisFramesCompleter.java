package com.tiffnix.invisibleitemframes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvisFramesCompleter implements TabCompleter {
    private static final String[] COMMANDS = new String[]{"reload", "grant", "give"};

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Arrays.asList(COMMANDS);
        }

        return switch (args[0]) {
            case "reload" -> new ArrayList<>();
            case "grant" -> null;
            case "give" -> switch (args.length) {
                case 2 -> Arrays.asList("regular", "glow");
                case 3 -> null;
                case 4 -> Arrays.asList("1", "8", "64");
                default -> new ArrayList<>();
            };
            default -> args.length > 1 ? null : StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), new ArrayList<>());
        };
    }
}
