package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.PerkRegistry;
import de.thecoolcraft11.hideAndSeek.perk.PerkStateManager;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
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
            sender.sendMessage(Component.text("Usage: /has debug perks <player> [grant|revoke|list] [perkId]", NamedTextColor.YELLOW));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found: " + args[0], NamedTextColor.RED));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "grant" -> handleGrant(sender, target, args);
            case "revoke" -> handleRevoke(sender, target, args);
            case "list" -> handleList(sender, target);
            default -> {
                sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /has debug perks <player> [grant|revoke|list] [perkId]", NamedTextColor.YELLOW));
            }
        }

        return true;
    }

    private void handleGrant(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /has debug perks <player> grant <perkId> [-f]", NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Available perks:", NamedTextColor.GRAY));
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
            sender.sendMessage(Component.text("Unknown perk: " + perkId, NamedTextColor.RED));
            return;
        }

        boolean forceBypassCooldown = args.length >= 4 && "-f".equalsIgnoreCase(args[3]);
        boolean granted = stateManager.grantDebug(target, perk, forceBypassCooldown);
        if (!granted) {
            sender.sendMessage(Component.text("Failed to grant perk ", NamedTextColor.RED)
                    .append(Component.text(perkId, NamedTextColor.AQUA))
                    .append(Component.text(" to ", NamedTextColor.RED))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
            return;
        }

        sender.sendMessage(Component.text("Granted and activated perk ", NamedTextColor.GREEN)
                .append(Component.text(perkId, NamedTextColor.AQUA))
                .append(Component.text(" for ", NamedTextColor.GREEN))
                .append(Component.text(target.getName(), NamedTextColor.AQUA)));
    }

    private void handleRevoke(CommandSender sender, Player target, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(Component.text("Usage: /has debug perks <player> revoke <perkId>", NamedTextColor.YELLOW));
            return;
        }

        String perkId = args[2];
        UUID targetId = target.getUniqueId();
        PerkStateManager stateManager = plugin.getPerkService().getStateManager();

        if (stateManager.hasPurchased(targetId, perkId)) {
            stateManager.removePurchased(targetId, perkId);
            sender.sendMessage(Component.text("Revoked perk ", NamedTextColor.GREEN)
                    .append(Component.text(perkId, NamedTextColor.AQUA))
                    .append(Component.text(" from ", NamedTextColor.GREEN))
                    .append(Component.text(target.getName(), NamedTextColor.AQUA)));
        } else {
            sender.sendMessage(Component.text("Player doesn't have perk: " + perkId, NamedTextColor.RED));
        }
    }

    private void handleList(CommandSender sender, Player target) {
        UUID targetId = target.getUniqueId();
        PerkStateManager stateManager = plugin.getPerkService().getStateManager();

        sender.sendMessage(Component.text("\n=== Perks for " + target.getName() + " ===", NamedTextColor.GOLD));

        java.util.Set<String> perks = stateManager.getPurchased().getOrDefault(targetId, java.util.Set.of());
        if (perks.isEmpty()) {
            sender.sendMessage(Component.text("No perks", NamedTextColor.YELLOW));
        } else {
            for (String perkId : perks) {
                sender.sendMessage(Component.text("  - " + perkId, NamedTextColor.GRAY));
            }
        }
    }

    private void listAvailablePerks(CommandSender sender) {
        for (PerkDefinition perk : plugin.getPerkService().getRegistry().getAllPerks()) {
            sender.sendMessage(Component.text("  - " + perk.getId(), NamedTextColor.GRAY));
        }
    }
}




