package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.minigameframework.progression.BoosterAPI;
import de.thecoolcraft11.minigameframework.progression.PlayerBooster;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class DebugBoosterCommand implements DebugSubcommand {
    private static final List<String> BOOSTER_TYPES = List.of("XP", "COIN");


    @Override
    public boolean handle(@NotNull CommandSender sender, @NonNull @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text(
                    "Usage: /has debug booster <player> [add|remove|list|info] [type] [strength] [duration]",
                    NamedTextColor.YELLOW));
            return true;
        }

        final Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }
        final java.util.UUID targetId = target.getUniqueId();
        final String targetName = target.getName();

        String boosterAction = args[1].toLowerCase();

        if (boosterAction.equals("list")) {
            sender.sendMessage(Component.text("Available booster types: " + String.join(", ", BOOSTER_TYPES),
                    NamedTextColor.YELLOW));
            return true;
        }

        if (boosterAction.equals("info")) {
            List<PlayerBooster> boosters = BoosterAPI.getActiveBoosters(targetId);
            if (boosters.isEmpty()) {
                sender.sendMessage(Component.text("No active boosters for " + targetName, NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("Active boosters for " + targetName + ":", NamedTextColor.GOLD));
                for (PlayerBooster booster : boosters) {
                    long remaining = BoosterAPI.getBoosterTimeRemaining(targetId, booster.type());
                    String timeStr = formatDuration(remaining);
                    sender.sendMessage(Component.text(
                            "  - " + booster.type().name() + ": " + String.format("%.2f",
                                    booster.strength()) + "x (remaining: " + timeStr + ")",
                            NamedTextColor.AQUA
                    ));
                }
            }
            return true;
        }

        if (!(boosterAction.equals("add") || boosterAction.equals("remove"))) {
            sender.sendMessage(Component.text("Unknown booster action: " + boosterAction, NamedTextColor.RED));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Usage: /has debug booster <player> " + boosterAction + " <type> [strength] [duration]",
                    NamedTextColor.YELLOW));
            return true;
        }

        String boosterTypeStr = args[2].toUpperCase();
        PlayerBooster.BoosterType boosterType;
        try {
            boosterType = PlayerBooster.BoosterType.valueOf(boosterTypeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid booster type: " + boosterTypeStr, NamedTextColor.RED));
            sender.sendMessage(
                    Component.text("Available types: " + String.join(", ", BOOSTER_TYPES), NamedTextColor.YELLOW));
            return true;
        }

        if (boosterAction.equals("add")) {
            if (args.length < 5) {
                sender.sendMessage(Component.text("Usage: /has debug booster <player> add <type> <strength> <duration>",
                        NamedTextColor.YELLOW));
                return true;
            }
            float strength;
            double duration;
            try {
                strength = Float.parseFloat(args[3]);
                duration = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid strength or duration: " + args[3] + ", " + args[4],
                        NamedTextColor.RED));
                return true;
            }

            if (boosterType == PlayerBooster.BoosterType.XP) {
                BoosterAPI.applyXPBooster(targetId, strength, duration);
            } else {
                BoosterAPI.applyCoinBooster(targetId, strength, duration);
            }

            long durationMs = (long) (duration * 60 * 1000);
            String timeStr = formatDuration(durationMs);
            sender.sendMessage(Component.text("Applied " + boosterType.name() + " booster to " + targetName +
                    " (" + String.format("%.2f", strength) + "x for " + timeStr + ")", NamedTextColor.GREEN));
        } else {
            BoosterAPI.removeBooster(targetId, boosterType);
            sender.sendMessage(Component.text("Removed " + boosterType.name() + " booster from " + targetName,
                    NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("add", "remove", "list", "info"), args[1]);
        }
        if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))) {
            return DebugSubcommand.filterByPrefix(BOOSTER_TYPES, args[2]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("add")) {
            return DebugSubcommand.filterByPrefix(List.of("1.0", "1.25", "1.5", "2.0", "3.0"), args[3]);
        }
        if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            return DebugSubcommand.filterByPrefix(List.of("10", "30", "60", "120", "300"), args[4]);
        }
        return List.of();
    }

    /**
     * Format milliseconds into human-readable duration.
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) return "0m";

        long hours = TimeUnit.MILLISECONDS.toHours(milliseconds);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds) % 60;

        if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}

