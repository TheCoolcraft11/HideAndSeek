package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DebugAntiCheatCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugAntiCheatCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return DebugSubcommand.filterByPrefix(List.of("reset", "show"), args[0]);
        }
        if (args.length == 2) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[1]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.usage"));
            return true;
        }

        String action = args[0].toLowerCase();

        if ("reset".equals(action)) {
            return handleReset(sender, args);
        } else if ("show".equals(action)) {
            return handleShow(sender, args);
        } else {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.anticheat.unknown_action", java.util.Map.of("action", action)));
            return true;
        }
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        Player target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.specify_player"));
                return false;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.player_not_found",
                        java.util.Map.of("player", args[1])));
                return false;
            }
        }

        UUID targetId = target.getUniqueId();

        HideAndSeek.getDataController().removeGlowing(targetId);


        plugin.getAntiCheatVisibilityListener().refreshSoon();

        sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.success_reset",
                java.util.Map.of("player", target.getName())));

        return true;
    }

    private boolean handleShow(CommandSender sender, String[] args) {
        Player target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.specify_player"));
                return false;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.player_not_found",
                        java.util.Map.of("player", args[1])));
                return false;
            }
        }

        UUID targetId = target.getUniqueId();

        HideAndSeek.getDataController().setGlowing(targetId, true);


        plugin.getAntiCheatVisibilityListener().refreshSoon();

        sender.sendMessage(plugin.tr(sender, "command.debug.anticheat.success_show",
                java.util.Map.of("player", target.getName())));

        return true;
    }
}


