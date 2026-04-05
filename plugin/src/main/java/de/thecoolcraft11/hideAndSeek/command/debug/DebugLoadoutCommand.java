package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutDataService;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DebugLoadoutCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugLoadoutCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("reset", "items", "give"), args[1]);
        }
        if (args.length == 3 && "give".equalsIgnoreCase(args[1])) {
            List<String> items = java.util.Arrays.stream(LoadoutItemType.values()).map(Enum::name).toList();
            return DebugSubcommand.filterByPrefix(items, args[2]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /has debug loadout <player> [reset|items|give] [itemName]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "reset" -> handleReset(sender, target);
            case "items" -> handleItems(sender, target);
            case "give" -> handleGive(sender, target, args);
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /has debug loadout <player> [reset|items|give] [itemName]", NamedTextColor.YELLOW));
            }
        }

        return true;
    }

    private void handleReset(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(Component.text("Player has no loadout", NamedTextColor.YELLOW));
            return;
        }

        for (LoadoutItemType item : List.copyOf(loadout.getHiderItems())) {
            loadout.removeHiderItem(item);
        }

        for (LoadoutItemType item : List.copyOf(loadout.getSeekerItems())) {
            loadout.removeSeekerItem(item);
        }

        LoadoutDataService.savePlayer(plugin, targetId);

        sender.sendMessage(Component.text("Loadout reset for ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA)));
    }

    private void handleItems(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(Component.text("Player has no loadout", NamedTextColor.YELLOW));
            return;
        }

        sender.sendMessage(Component.text("\n=== Loadout for " + target.getName() + " ===", NamedTextColor.GOLD));

        if (loadout.getHiderItems().isEmpty()) {
            sender.sendMessage(Component.text("Hider Items: None", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Hider Items:", NamedTextColor.YELLOW));
            for (LoadoutItemType item : loadout.getHiderItems()) {
                sender.sendMessage(Component.text("  - " + item.name(), NamedTextColor.GRAY));
            }
        }

        if (loadout.getSeekerItems().isEmpty()) {
            sender.sendMessage(Component.text("Seeker Items: None", NamedTextColor.YELLOW));
        } else {
            sender.sendMessage(Component.text("Seeker Items:", NamedTextColor.YELLOW));
            for (LoadoutItemType item : loadout.getSeekerItems()) {
                sender.sendMessage(Component.text("  - " + item.name(), NamedTextColor.GRAY));
            }
        }
    }

    private void handleGive(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /has debug loadout <player> give <itemName>", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Available items:", NamedTextColor.YELLOW));
            for (LoadoutItemType item : LoadoutItemType.values()) {
                String type = item.isForHiders() && item.isForSeekers() ? "BOTH" :
                        item.isForHiders() ? "HIDER" : "SEEKER";
                sender.sendMessage(Component.text("  - " + item.name() + " (" + type + ")", NamedTextColor.GRAY));
            }
            return;
        }

        String itemName = args[2].toUpperCase();
        LoadoutItemType itemType;

        try {
            itemType = LoadoutItemType.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Unknown item: " + itemName, NamedTextColor.RED));
            return;
        }

        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(Component.text("Player has no loadout", NamedTextColor.RED));
            return;
        }

        boolean added = false;
        if (itemType.isForHiders()) {
            added = loadout.addHiderItem(itemType, 5, 1000, itemType.getRarity().getDefaultCost());
        } else if (itemType.isForSeekers()) {
            added = loadout.addSeekerItem(itemType, 5, 1000, itemType.getRarity().getDefaultCost());
        }

        if (added) {
            LoadoutDataService.savePlayer(plugin, targetId);
            sender.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                    .append(Component.text(itemName, NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA))
                    .append(Component.text("'s loadout", NamedTextColor.GREEN)));
        } else {
            sender.sendMessage(Component.text("Failed to add item (loadout full or invalid item)", NamedTextColor.RED));
        }
    }
}


