package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.command.debug.*;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
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
        subcommands.put("points", new DebugPointsCommand(plugin));
        subcommands.put("coins", new DebugCoinsCommand(plugin));
        subcommands.put("skins", new DebugSkinsCommand(plugin));
        subcommands.put("loadout", new DebugLoadoutCommand(plugin));
        subcommands.put("perks", new DebugPerksCommand(plugin));
        subcommands.put("config", new DebugConfigCommand(plugin));
        subcommands.put("unstuck", new DebugUnstuckCommand(plugin));
        subcommands.put("migrateyaml", new DebugMigrateYamlCommand(plugin));
        subcommands.put("xp", new DebugXPCommand(plugin));
        subcommands.put("booster", new DebugBoosterCommand(plugin));
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
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.tr(sender, "common.command.no_permission"));
            return;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subcommandName = args[0].toLowerCase();
        DebugSubcommand subcommand = subcommands.get(subcommandName);

        if (subcommand == null) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.unknown_subcommand", Map.of("subcommand", subcommandName)));
            sendHelp(sender);
            return;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        subcommand.handle(sender, subArgs);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.tr(sender, "command.debug.help.header"));

        sender.sendMessage(plugin.tr(sender, "command.debug.help.anticheat"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.points"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.coins"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.skins"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.loadout"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.perks"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.config"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.unstuck"));
        sender.sendMessage(plugin.tr(sender, "command.debug.help.migrateyaml"));
    }
}




