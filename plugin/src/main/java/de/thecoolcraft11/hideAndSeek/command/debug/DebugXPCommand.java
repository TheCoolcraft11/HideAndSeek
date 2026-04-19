package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class DebugXPCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugXPCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NonNull @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(
                    Component.text("Usage: /has debug xp <player> [set|give|remove] <amount>", NamedTextColor.YELLOW));
            return true;
        }

        final Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }
        final java.util.UUID targetId = target.getUniqueId();
        final String targetName = target.getName();
        String action = args[1].toLowerCase();


        if (action.equals("set") || action.equals("give") || action.equals("remove")) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /has debug xp <player> [set|give|remove] <amount>",
                        NamedTextColor.YELLOW));
                return true;
            }
            long amount;
            try {
                amount = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid amount: " + args[2], NamedTextColor.RED));
                return true;
            }
            var dataStore = plugin.getPlayerDataStore();
            final long finalAmount = amount;
            dataStore.getXp(targetId).thenAccept(currentXp -> {
                long newXp = currentXp;
                switch (action) {
                    case "set" -> newXp = finalAmount;
                    case "give" -> newXp = currentXp + finalAmount;
                    case "remove" -> newXp = Math.max(0, currentXp - finalAmount);
                }
                final long resultXp = newXp;
                dataStore.setXp(targetId, newXp).thenRun(() ->
                        sender.sendMessage(
                                Component.text("XP for " + targetName + " is now " + resultXp, NamedTextColor.GREEN))
                );
            });
            return true;
        }

        sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
        return true;
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

}

