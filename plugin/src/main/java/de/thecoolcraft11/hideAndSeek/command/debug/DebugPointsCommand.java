package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DebugPointsCommand implements DebugSubcommand {

    private final HideAndSeek plugin;

    public DebugPointsCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("set", "give", "remove"), args[1]);
        }
        if (args.length == 3) {
            return DebugSubcommand.filterByPrefix(List.of("1", "10", "100", "500", "1000"), args[2]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.tr(sender, "command.debug.points.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.points.player_not_found", java.util.Map.of("player", args[0])));
            return true;
        }

        UUID targetId = target.getUniqueId();
        String action = args[1].toLowerCase();
        int amount;

        try {
            amount = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.tr(sender, "command.debug.points.invalid_amount",
                    java.util.Map.of("value", args[args.length - 1])));
            return true;
        }

        switch (action) {
            case "set" -> {
                HideAndSeek.getDataController().addPoints(targetId, amount - HideAndSeek.getDataController().getPoints(targetId));
                sender.sendMessage(plugin.tr(sender, "command.debug.points.success_set",
                        java.util.Map.of("player", target.getName(), "amount", amount)));
            }
            case "give" -> {
                HideAndSeek.getDataController().addPoints(targetId, amount);
                int newPoints = HideAndSeek.getDataController().getPoints(targetId);
                sender.sendMessage(plugin.tr(sender, "command.debug.points.success_give",
                        java.util.Map.of("amount", amount, "player", target.getName(), "new_total", newPoints)));
            }
            case "remove" -> {
                HideAndSeek.getDataController().addPoints(targetId, -amount);
                int newPoints = HideAndSeek.getDataController().getPoints(targetId);
                sender.sendMessage(plugin.tr(sender, "command.debug.points.success_remove",
                        java.util.Map.of("amount", amount, "player", target.getName(), "new_total", newPoints)));
            }
            default -> {
                sender.sendMessage(
                        plugin.tr(sender, "command.debug.points.unknown_action", java.util.Map.of("action", action)));
                sender.sendMessage(plugin.tr(sender, "command.debug.points.usage"));
            }
        }

        return true;
    }
}



