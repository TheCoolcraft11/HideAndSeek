package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
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
            List<String> players = Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getName).toList();
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
            sender.sendMessage(plugin.tr(sender, "command.debug.coins.usage"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(args[0]);
        if (target == null || target.getName() == null) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.coins.player_not_found", java.util.Map.of("player", args[0])));
            return true;
        }

        UUID targetId = target.getUniqueId();
        String action = args[1].toLowerCase();
        int amount;

        try {
            amount = Integer.parseInt(args[args.length - 1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(plugin.tr(sender, "command.debug.coins.invalid_amount",
                    java.util.Map.of("value", args[args.length - 1])));
            return true;
        }

        switch (action) {
            case "set" -> ItemSkinSelectionService.getCoins(targetId)
                    .thenCompose(current -> {
                        int diff = amount - current;
                        if (diff != 0) {
                            return ItemSkinSelectionService.addCoins(plugin, targetId, diff, true);
                        }
                        return java.util.concurrent.CompletableFuture.completedFuture(null);
                    })
                    .thenCompose(v -> ItemSkinSelectionService.getCoins(targetId))
                    .thenAccept(newCoins ->
                            sender.sendMessage(plugin.tr(sender,
                                    "command.debug.coins.success_set",
                                    java.util.Map.of(
                                            "player", target.getName(),
                                            "amount", amount,
                                            "new_total", newCoins
                                    )))
                    );
            case "give" -> ItemSkinSelectionService.addCoins(plugin, targetId, amount, true)
                    .thenCompose(v -> ItemSkinSelectionService.getCoins(targetId))
                    .thenAccept(newCoins ->
                            sender.sendMessage(plugin.tr(sender,
                                    "command.debug.coins.success_give",
                                    java.util.Map.of(
                                            "amount", amount,
                                            "player", target.getName(),
                                            "new_total", newCoins
                                    )))
                    );
            case "remove" -> ItemSkinSelectionService.addCoins(plugin, targetId, -amount, true)
                    .thenCompose(v -> ItemSkinSelectionService.getCoins(targetId))
                    .thenAccept(newCoins ->
                            sender.sendMessage(plugin.tr(sender,
                                    "command.debug.coins.success_remove",
                                    java.util.Map.of(
                                            "amount", amount,
                                            "player", target.getName(),
                                            "new_total", newCoins
                                    )))
                    );
            default -> {
                sender.sendMessage(
                        plugin.tr(sender, "command.debug.coins.unknown_action", java.util.Map.of("action", action)));
                sender.sendMessage(plugin.tr(sender, "command.debug.coins.usage"));
            }
        }

        return true;
    }
}


