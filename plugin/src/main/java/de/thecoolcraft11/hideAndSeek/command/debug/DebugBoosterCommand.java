package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.progression.BoosterAPI;
import de.thecoolcraft11.minigameframework.progression.PlayerBooster;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DebugBoosterCommand implements DebugSubcommand {
    private final HideAndSeek plugin;
    private static final List<String> BOOSTER_TYPES = List.of("XP", "COIN");

    public DebugBoosterCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }


    @Override
    public boolean handle(@NotNull CommandSender sender, @NonNull @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.usage"));
            return true;
        }

        final OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.player_not_found", Map.of("player", args[0])));
            return true;
        }
        final java.util.UUID targetId = target.getUniqueId();
        final String targetName = target.getName();

        String boosterAction = args[1].toLowerCase();

        if (boosterAction.equals("list")) {
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.available_types",
                    Map.of("types", String.join(", ", BOOSTER_TYPES))));
            return true;
        }

        if (boosterAction.equals("info")) {
            List<PlayerBooster> boosters = BoosterAPI.getActiveBoosters(targetId);
            if (boosters.isEmpty()) {
                sender.sendMessage(plugin.tr(sender, "command.debug.booster.no_active", Map.of("player", targetName)));
            } else {
                sender.sendMessage(plugin.tr(sender, "command.debug.booster.header", Map.of("player", targetName)));
                for (PlayerBooster booster : boosters) {
                    long remaining = BoosterAPI.getBoosterTimeRemaining(targetId, booster.type());
                    String timeStr = formatDuration(remaining);
                    sender.sendMessage(plugin.tr(sender, "command.debug.booster.entry", Map.of(
                            "type", booster.type().name(),
                            "strength", String.format("%.2f", booster.strength()),
                            "remaining", timeStr
                    )));
                }
            }
            return true;
        }

        if (!(boosterAction.equals("add") || boosterAction.equals("remove"))) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.booster.unknown_action", Map.of("action", boosterAction)));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.booster.usage_action", Map.of("action", boosterAction)));
            return true;
        }

        String boosterTypeStr = args[2].toUpperCase();
        PlayerBooster.BoosterType boosterType;
        try {
            boosterType = PlayerBooster.BoosterType.valueOf(boosterTypeStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.invalid_type", Map.of("type", boosterTypeStr)));
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.available_types",
                    Map.of("types", String.join(", ", BOOSTER_TYPES))));
            return true;
        }

        if (boosterAction.equals("add")) {
            if (args.length < 5) {
                sender.sendMessage(plugin.tr(sender, "command.debug.booster.usage_add"));
                return true;
            }
            float strength;
            double duration;
            try {
                strength = Float.parseFloat(args[3]);
                duration = Double.parseDouble(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(plugin.tr(sender, "command.debug.booster.invalid_strength_duration",
                        Map.of("strength", args[3], "duration", args[4])));
                return true;
            }

            if (boosterType == PlayerBooster.BoosterType.XP) {
                BoosterAPI.applyXPBooster(targetId, strength, duration);
            } else {
                BoosterAPI.applyCoinBooster(targetId, strength, duration);
            }

            long durationMs = (long) (duration * 60 * 1000);
            String timeStr = formatDuration(durationMs);
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.applied", Map.of(
                    "type", boosterType.name(),
                    "player", targetName,
                    "strength", String.format("%.2f", strength),
                    "duration", timeStr
            )));
        } else {
            BoosterAPI.removeBooster(targetId, boosterType);
            sender.sendMessage(plugin.tr(sender, "command.debug.booster.removed", Map.of(
                    "type", boosterType.name(),
                    "player", targetName
            )));
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
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

    private String formatDuration(long milliseconds) {
        if (milliseconds <= 0) return "Infinite";

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

