package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.PerkRegistry;
import de.thecoolcraft11.hideAndSeek.perk.PerkStateManager;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DebugPerksCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugPerksCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("grant", "revoke", "list"), args[1]);
        }
        if (args.length == 3 && ("grant".equalsIgnoreCase(args[1]) || "revoke".equalsIgnoreCase(args[1]))) {
            List<String> perkIds = plugin.getPerkService().getRegistry().getAllPerks().stream()
                    .map(PerkDefinition::getId)
                    .toList();
            return DebugSubcommand.filterByPrefix(perkIds, args[2]);
        }
        if (args.length == 4 && "grant".equalsIgnoreCase(args[1])) {
            return DebugSubcommand.filterByPrefix(List.of("-f"), args[3]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.player_not_found", Map.of("player", args[0])));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "grant" -> handleGrant(sender, target, args);
            case "revoke" -> handleRevoke(sender, target, args);
            case "list" -> handleList(sender, target);
            default -> {
                sender.sendMessage(plugin.tr(sender, "command.debug.perks.unknown_action", Map.of("action", action)));
                sender.sendMessage(plugin.tr(sender, "command.debug.perks.usage"));
            }
        }

        return true;
    }

    private void handleGrant(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.grant_usage"));
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.available_perks"));
            listAvailablePerks(sender);
            return;
        }

        String perkId = args[2];
        PerkRegistry registry = plugin.getPerkService().getRegistry();
        PerkStateManager stateManager = plugin.getPerkService().getStateManager();

        PerkDefinition perk = registry.getAllPerks().stream()
                .filter(p -> p.getId().equals(perkId))
                .findFirst()
                .orElse(null);

        if (perk == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.unknown_perk", Map.of("perk", perkId)));
            return;
        }

        boolean forceBypassCooldown = args.length >= 4 && "-f".equalsIgnoreCase(args[3]);
        boolean granted = stateManager.grantDebug(target, perk, forceBypassCooldown);
        if (!granted) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.grant_failed", Map.of(
                    "perk", perkId,
                    "player", target.getName()
            )));
            return;
        }

        sender.sendMessage(plugin.tr(sender, "command.debug.perks.grant_success", Map.of(
                "perk", perkId,
                "player", target.getName()
        )));
    }

    private void handleRevoke(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.revoke_usage"));
            return;
        }

        String perkId = args[2];
        UUID targetId = target.getUniqueId();
        PerkStateManager stateManager = plugin.getPerkService().getStateManager();

        if (stateManager.hasPurchased(targetId, perkId)) {
            stateManager.removePurchased(targetId, perkId);
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.revoke_success", Map.of(
                    "perk", perkId,
                    "player", target.getName()
            )));
        } else {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.not_owned", Map.of("perk", perkId)));
        }
    }

    private void handleList(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PerkStateManager stateManager = plugin.getPerkService().getStateManager();

        sender.sendMessage(plugin.tr(sender, "command.debug.perks.list_header", Map.of("player", target.getName())));

        java.util.Set<String> perks = stateManager.getPurchased().getOrDefault(targetId, java.util.Set.of());
        if (perks.isEmpty()) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.none"));
        } else {
            for (String perkId : perks) {
                sender.sendMessage(plugin.tr(sender, "command.debug.perks.list_entry", Map.of("perk", perkId)));
            }
        }
    }

    private void listAvailablePerks(CommandSender sender) {
        for (PerkDefinition perk : plugin.getPerkService().getRegistry().getAllPerks()) {
            sender.sendMessage(plugin.tr(sender, "command.debug.perks.list_entry", Map.of("perk", perk.getId())));
        }
    }
}




