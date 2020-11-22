package com.tiffnix.invisibleitemframes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InvisFramesCompleter implements TabCompleter {
    private static final String[] COMMANDS = new String[] { "reload", "grant", "give" };

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Arrays.asList(COMMANDS);
        }

        switch (args[0]) {
            case "reload":
                return new ArrayList<>();
            case "grant":
                return null;
            case "give":
                switch (args.length) {
                    case 2:
                        return null;
                    case 3:
                        return Arrays.asList("1", "8", "64");
                    default:
                        return new ArrayList<>();
                }
            default:
                if (args.length > 1) {
                    return null;
                } else {
                    return StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), new ArrayList<>());
                }
        }
    }

}
