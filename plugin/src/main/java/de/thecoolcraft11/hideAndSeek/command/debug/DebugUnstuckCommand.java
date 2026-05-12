package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.UnstuckManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class DebugUnstuckCommand implements DebugSubcommand {
    private final HideAndSeek plugin;
    private final UnstuckManager unstuckManager;

    public DebugUnstuckCommand(HideAndSeek plugin) {
        this.plugin = plugin;
        this.unstuckManager = plugin.getUnstuckManager();
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[0]);
        }
        if (args.length == 2) {
            return DebugSubcommand.filterByPrefix(List.of("history", "nearby", "spawn"), args[1]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.player_not_found", Map.of("player", args[0])));
            return true;
        }

        String method = args[1].toLowerCase();
        Location targetLocation;
        UnstuckManager.UnstuckMethod unstuckMethod = null;

        switch (method) {
            case "history" -> {

                targetLocation = findSafePositionFromHistory(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.HISTORY;
                }
            }
            case "nearby" -> {

                targetLocation = findNearbySafePosition(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.NEARBY;
                }
            }
            case "spawn" -> {

                targetLocation = unstuckManager.resolveWorldSpawnTarget(target);
                if (targetLocation != null) {
                    unstuckMethod = UnstuckManager.UnstuckMethod.SPAWN;
                }
            }
            default -> {
                sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.unknown_method", Map.of("method", method)));
                sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.valid_methods"));
                return true;
            }
        }

        if (targetLocation == null) {
            sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.no_safe_position", Map.of("method", method)));
            return true;
        }


        boolean teleported = target.teleport(targetLocation);
        if (!teleported) {
            sender.sendMessage(
                    plugin.tr(sender, "command.debug.unstuck.teleport_failed", Map.of("player", target.getName())));
            return true;
        }


        unstuckManager.recordSuccessfulUnstuck(target.getUniqueId(), targetLocation, unstuckMethod);

        unstuckManager.applyCooldown(target.getUniqueId(), 0);


        targetLocation.getWorld().spawnParticle(Particle.PORTAL, targetLocation, 24, 0.3, 0.5, 0.3, 0.1);
        target.playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);


        sender.sendMessage(plugin.tr(sender, "command.debug.unstuck.force_sender", Map.of(
                "player", target.getName(),
                "method", method
        )));

        target.sendMessage(plugin.tr(target, "command.debug.unstuck.force_target", Map.of(
                "sender", sender.getName(),
                "method", method
        )));

        return true;
    }

    private Location findSafePositionFromHistory(Player player) {


        UnstuckManager.UnstuckResult result = unstuckManager.tryFindSafePosition(player, HideAndSeek.getDataController().getRoundSpawnPoint());

        if (result.success() && result.location() != null && result.method() == UnstuckManager.UnstuckMethod.HISTORY) {
            return result.location();
        }


        return null;
    }

    private Location findNearbySafePosition(Player player) {

        UnstuckManager.UnstuckResult result = unstuckManager.tryFindSafePosition(player, HideAndSeek.getDataController().getRoundSpawnPoint());

        if (result.success() && result.location() != null && result.method() == UnstuckManager.UnstuckMethod.NEARBY) {
            return result.location();
        }


        return null;
    }
}



