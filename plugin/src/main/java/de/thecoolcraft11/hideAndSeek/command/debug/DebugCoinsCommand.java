package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DebugCoinsCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugCoinsCommand(HideAndSeek plugin) {
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
            sender.sendMessage(Component.text("Usage: /has debug coins <player> [set|give|remove] <amount>", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        UUID targetId = target.getUniqueId();
        String action = args[1].toLowerCase();
        int amount;

        try {
            amount = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Invalid amount: " + args[args.length - 1], NamedTextColor.RED));
            return true;
        }

        switch (action) {
            case "set" -> {


                int current = ItemSkinSelectionService.getCoins(targetId);
                int diff = amount - current;
                if (diff != 0) {
                    ItemSkinSelectionService.addCoins(plugin, targetId, diff, true);
                }
                sender.sendMessage(Component.text("Set coins for ", NamedTextColor.GREEN)
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" to ", NamedTextColor.GREEN))
                        .append(Component.text(String.valueOf(amount), NamedTextColor.GOLD)));
            }
            case "give" -> {
                ItemSkinSelectionService.addCoins(plugin, targetId, amount, true);
                int newCoins = ItemSkinSelectionService.getCoins(targetId);
                sender.sendMessage(Component.text("Gave ", NamedTextColor.GREEN)
                        .append(Component.text(String.valueOf(amount), NamedTextColor.GOLD))
                        .append(Component.text(" coins to ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" (now: " + newCoins + ")", NamedTextColor.GRAY)));
            }
            case "remove" -> {
                ItemSkinSelectionService.addCoins(plugin, targetId, -amount, true);
                int newCoins = ItemSkinSelectionService.getCoins(targetId);
                sender.sendMessage(Component.text("Removed ", NamedTextColor.GREEN)
                        .append(Component.text(String.valueOf(amount), NamedTextColor.GOLD))
                        .append(Component.text(" coins from ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.AQUA))
                        .append(Component.text(" (now: " + newCoins + ")", NamedTextColor.GRAY)));
            }
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /has debug coins <player> [set|give|remove] <amount>", NamedTextColor.YELLOW));
            }
        }

        return true;
    }
}


