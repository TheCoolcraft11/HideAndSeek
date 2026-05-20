package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DebugSkinsCommand implements DebugSubcommand {

    private final HideAndSeek plugin;

    public DebugSkinsCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .toList();

            return DebugSubcommand.filterByPrefix(players, args[0]);
        }

        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(
                    List.of("list", "unlock", "lock", "reset"),
                    args[1]
            );
        }

        if (args.length == 3 &&
                ("unlock".equalsIgnoreCase(args[1]) ||
                        "lock".equalsIgnoreCase(args[1]))) {

            List<String> options = plugin.getCustomItemManager()
                    .getVariantManager()
                    .getAllVariants()
                    .entrySet()
                    .stream()
                    .flatMap(entry -> {
                        String logical =
                                ItemSkinSelectionService.normalizeLogicalItemId(entry.getKey());

                        return entry.getValue()
                                .stream()
                                .map(ItemVariant::getId)
                                .map(variantId -> logical + ":" + variantId);
                    })
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();

            return DebugSubcommand.filterByPrefix(options, args[2]);
        }

        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {

        if (args.length < 2) {
            sender.sendMessage(plugin.tr(sender, "command.debug.skins.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);

        if (target == null) {
            sender.sendMessage(
                    plugin.tr(
                            sender,
                            "command.debug.skins.player_not_found",
                            Map.of("player", args[0])
                    )
            );
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {

            case "list" -> handleList(sender, target);

            case "unlock" -> handleUnlock(sender, target, args);

            case "lock" -> handleLock(sender, args);

            case "reset" -> handleReset(sender);

            default -> {
                sender.sendMessage(
                        plugin.tr(
                                sender,
                                "command.debug.skins.unknown_action",
                                Map.of("action", action)
                        )
                );

                sender.sendMessage(
                        plugin.tr(sender, "command.debug.skins.usage")
                );
            }
        }

        return true;
    }

    private void handleList(CommandSender sender, Player target) {

        UUID playerId = target.getUniqueId();

        ItemSkinSelectionService.getCoins(playerId)
                .thenAccept(coins -> Bukkit.getScheduler().runTask(plugin, () -> {

                    sender.sendMessage(
                            plugin.tr(
                                    sender,
                                    "command.debug.skins.list_header",
                                    Map.of("player", target.getName())
                            )
                    );

                    sender.sendMessage(
                            plugin.tr(
                                    sender,
                                    "command.debug.skins.list_coins",
                                    Map.of("coins", coins)
                            )
                    );

                    sender.sendMessage(
                            plugin.tr(
                                    sender,
                                    "command.debug.skins.list_hint"
                            )
                    );
                }))
                .exceptionally(ex -> {

                    Bukkit.getScheduler().runTask(plugin, () -> {

                        sender.sendMessage(
                                "§cFailed to load player skin data."
                        );

                        plugin.getLogger().warning(
                                "Failed to load skin debug info for "
                                        + playerId
                                        + ": "
                                        + ex.getMessage()
                        );
                    });

                    return null;
                });
    }

    private void handleUnlock(CommandSender sender, Player target, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.skins.unlock_usage")
            );
            return;
        }

        String[] parts = args[2].split(":");

        if (parts.length != 2) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.skins.invalid_format")
            );
            return;
        }

        String itemId = parts[0];
        String variantId = parts[1];

        UUID playerId = target.getUniqueId();

        ItemSkinSelectionService.unlock(
                        plugin,
                        playerId,
                        itemId,
                        variantId
                )
                .thenAccept(unlocked -> Bukkit.getScheduler().runTask(plugin, () -> {

                    if (unlocked) {

                        sender.sendMessage(
                                plugin.tr(
                                        sender,
                                        "command.debug.skins.unlocked_success",
                                        Map.of(
                                                "skin", itemId + ":" + variantId,
                                                "player", target.getName()
                                        )
                                )
                        );

                    } else {

                        sender.sendMessage(
                                plugin.tr(
                                        sender,
                                        "command.debug.skins.unlock_failed"
                                )
                        );
                    }
                }))
                .exceptionally(ex -> {

                    Bukkit.getScheduler().runTask(plugin, () -> {

                        sender.sendMessage(
                                "§cFailed to unlock skin."
                        );

                        plugin.getLogger().warning(
                                "Failed to unlock skin for "
                                        + playerId
                                        + ": "
                                        + ex.getMessage()
                        );
                    });

                    return null;
                });
    }

    private void handleLock(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.skins.lock_usage")
            );
            return;
        }

        sender.sendMessage(
                plugin.tr(sender, "command.debug.skins.lock_not_implemented")
        );
    }

    private void handleReset(CommandSender sender) {

        sender.sendMessage(
                plugin.tr(sender, "command.debug.skins.reset_not_implemented")
        );
    }
}