package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutDataService;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
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
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.player_not_found", Map.of("player", args[0])));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "reset" -> handleReset(sender, target);
            case "items" -> handleItems(sender, target);
            case "give" -> handleGive(sender, target, args);
            default -> {
                sender.sendMessage(plugin.tr(sender, "command.debug.loadout.unknown_action", Map.of("action", action)));
                sender.sendMessage(plugin.tr(sender, "command.debug.loadout.usage"));
            }
        }

        return true;
    }

    private void handleReset(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.no_loadout"));
            return;
        }

        for (LoadoutItemType item : List.copyOf(loadout.getHiderItems())) {
            loadout.removeHiderItem(item);
        }

        for (LoadoutItemType item : List.copyOf(loadout.getSeekerItems())) {
            loadout.removeSeekerItem(item);
        }

        LoadoutDataService.savePlayer(plugin, targetId);

        sender.sendMessage(plugin.tr(sender, "command.debug.loadout.reset", Map.of("player", target.getName())));
    }

    private void handleItems(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.no_loadout"));
            return;
        }

        sender.sendMessage(plugin.tr(sender, "command.debug.loadout.header", Map.of("player", target.getName())));

        if (loadout.getHiderItems().isEmpty()) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.no_hider_items"));
        } else {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.hider_items"));
            for (LoadoutItemType item : loadout.getHiderItems()) {
                sender.sendMessage(plugin.tr(sender, "command.debug.loadout.item_entry", Map.of("item", item.name())));
            }
        }

        if (loadout.getSeekerItems().isEmpty()) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.no_seeker_items"));
        } else {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.seeker_items"));
            for (LoadoutItemType item : loadout.getSeekerItems()) {
                sender.sendMessage(plugin.tr(sender, "command.debug.loadout.item_entry", Map.of("item", item.name())));
            }
        }
    }

    private void handleGive(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.usage_give"));
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.available_items"));
            for (LoadoutItemType item : LoadoutItemType.values()) {
                String type = item.isForHiders() && item.isForSeekers() ? "BOTH" :
                        item.isForHiders() ? "HIDER" : "SEEKER";
                sender.sendMessage(plugin.tr(sender, "command.debug.loadout.available_item", Map.of(
                        "item", item.name(),
                        "type", type
                )));
            }
            return;
        }

        String itemName = args[2].toUpperCase();
        LoadoutItemType itemType;

        try {
            itemType = LoadoutItemType.valueOf(itemName);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.unknown_item", Map.of("item", itemName)));
            return;
        }

        UUID targetId = target.getUniqueId();
        PlayerLoadout loadout = LoadoutDataService.getLoadout(targetId);

        if (loadout == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.no_loadout"));
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
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.added", Map.of(
                    "item", itemName,
                    "player", target.getName()
            )));
        } else {
            sender.sendMessage(plugin.tr(sender, "command.debug.loadout.add_failed"));
        }
    }
}


