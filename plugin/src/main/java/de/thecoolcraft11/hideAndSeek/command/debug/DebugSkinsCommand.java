package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DebugSkinsCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugSkinsCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("list", "unlock", "lock", "reset"), args[1]);
        }
        if (args.length == 3 && ("unlock".equalsIgnoreCase(args[1]) || "lock".equalsIgnoreCase(args[1]))) {
            List<String> options = plugin.getCustomItemManager().getVariantManager().getAllVariants().entrySet().stream()
                    .flatMap(entry -> {
                        String logical = ItemSkinSelectionService.normalizeLogicalItemId(entry.getKey());
                        return entry.getValue().stream().map(ItemVariant::getId).map(variantId -> logical + ":" + variantId);
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
            sender.sendMessage(Component.text("Usage: /has debug skins <player> [list|unlock|lock|reset] [itemId:variantId]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "list" -> handleList(sender, target);
            case "unlock" -> handleUnlock(sender, target, args);
            case "lock" -> handleLock(sender, args);
            case "reset" -> handleReset(sender);
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /has debug skins <player> [list|unlock|lock|reset] [itemId:variantId]", NamedTextColor.YELLOW));
            }
        }

        return true;
    }

    private void handleList(CommandSender sender, Player target) {
        int coins = ItemSkinSelectionService.getCoins(target.getUniqueId());
        sender.sendMessage(Component.text("\n=== Skins for " + target.getName() + " ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Coins: " + coins, NamedTextColor.YELLOW));
        sender.sendMessage(Component.text("(Detailed list not implemented - use /items list instead)", NamedTextColor.GRAY));
    }

    private void handleUnlock(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /has debug skins <player> unlock <itemId:variantId>", NamedTextColor.YELLOW));
            return;
        }

        String[] parts = args[2].split(":");
        if (parts.length != 2) {
            sender.sendMessage(Component.text("Invalid format. Use itemId:variantId", NamedTextColor.RED));
            return;
        }

        String itemId = parts[0];
        String variantId = parts[1];

        if (ItemSkinSelectionService.unlock(plugin, target.getUniqueId(), itemId, variantId)) {
            sender.sendMessage(Component.text("Unlocked ", NamedTextColor.GREEN)
                    .append(Component.text(itemId + ":" + variantId, NamedTextColor.AQUA))
                    .append(Component.text(" for ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
        } else {
            sender.sendMessage(Component.text("Failed to unlock skin (already unlocked?)", NamedTextColor.RED));
        }
    }

    private void handleLock(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /has debug skins <player> lock <itemId:variantId>", NamedTextColor.YELLOW));
            return;
        }


        sender.sendMessage(Component.text("Lock function not implemented", NamedTextColor.YELLOW));
    }

    private void handleReset(CommandSender sender) {

        sender.sendMessage(Component.text("Reset function not fully implemented", NamedTextColor.YELLOW));
    }
}


