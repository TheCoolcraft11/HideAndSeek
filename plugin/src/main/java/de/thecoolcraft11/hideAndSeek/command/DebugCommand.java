package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.command.debug.*;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugCommand implements MinigameSubcommand {
    private static final String PERMISSION = "hideandseek.command.debug";
    private final HideAndSeek plugin;
    private final Map<String, DebugSubcommand> subcommands;

    public DebugCommand(HideAndSeek plugin) {
        this.plugin = plugin;
        this.subcommands = new HashMap<>();
        registerSubcommands();
    }

    private void registerSubcommands() {
        subcommands.put("anticheat", new DebugAntiCheatCommand(plugin));
        subcommands.put("points", new DebugPointsCommand());
        subcommands.put("coins", new DebugCoinsCommand(plugin));
        subcommands.put("skins", new DebugSkinsCommand(plugin));
        subcommands.put("loadout", new DebugLoadoutCommand(plugin));
        subcommands.put("perks", new DebugPerksCommand(plugin));
        subcommands.put("config", new DebugConfigCommand(plugin));
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }

        if (args.length == 0) {
            return subcommands.keySet().stream().sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }

        if (args.length == 1) {
            return DebugSubcommand.filterByPrefix(subcommands.keySet(), args[0]);
        }

        DebugSubcommand subcommand = subcommands.get(args[0].toLowerCase());
        if (subcommand == null) {
            return List.of();
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subcommand.tabComplete(sender, subArgs);
    }

    @Override
    public @NotNull String getName() {
        return "debug";
    }


    @Override
    public @Nullable String getPermission() {
        return PERMISSION;
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subcommandName = args[0].toLowerCase();
        DebugSubcommand subcommand = subcommands.get(subcommandName);

        if (subcommand == null) {
            sender.sendMessage(Component.text("Unknown debug subcommand: " + subcommandName, NamedTextColor.RED));
            sendHelp(sender);
            return true;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        return subcommand.handle(sender, subArgs);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.text("\n" + "=== Debug Command Help ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("/has debug anticheat [reset|show] [player]", NamedTextColor.YELLOW)
                .append(Component.text(" - Reset anti-cheat visibility", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug points <player> [set|give|remove] <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage player points", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug coins <player> [set|give|remove] <amount>", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage player coins", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug skins <player> [list|unlock|lock|reset]", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage player skins", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug loadout <player> [reset|items|give]", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage player loadout", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug perks <player> [grant|revoke|list] [perkId] [-f]", NamedTextColor.YELLOW)
                .append(Component.text(" - Manage player perks", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("/has debug config [test|validate] [-a]", NamedTextColor.YELLOW)
                .append(Component.text(" - Test configuration and maps", NamedTextColor.GRAY)));
    }
}




