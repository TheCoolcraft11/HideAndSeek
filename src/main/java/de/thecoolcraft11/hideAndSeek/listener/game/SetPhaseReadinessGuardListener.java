package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.*;
import java.util.stream.Collectors;

public class SetPhaseReadinessGuardListener implements Listener {
    private final HideAndSeek plugin;

    public SetPhaseReadinessGuardListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        VoteManager voteManager = plugin.getVoteManager();
        if (!voteManager.isReadinessEnabled() || voteManager.isNotLobbyPhase()) {
            return;
        }

        String raw = event.getMessage();
        if (raw.isBlank()) {
            return;
        }

        List<String> args = splitCommand(raw.startsWith("/") ? raw.substring(1) : raw);
        if (isNotSetPhaseToHiding(args)) {
            return;
        }
        if (hasForceArg(args)) {
            return;
        }
        if (voteManager.areAllEligiblePlayersReady()) {
            return;
        }

        event.setCancelled(true);
        sendNotReadyMessage(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onServerCommand(ServerCommandEvent event) {
        VoteManager voteManager = plugin.getVoteManager();
        if (!voteManager.isReadinessEnabled() || voteManager.isNotLobbyPhase()) {
            return;
        }

        String raw = event.getCommand();
        if (raw.isBlank()) {
            return;
        }

        List<String> args = splitCommand(raw);
        if (isNotSetPhaseToHiding(args)) {
            return;
        }
        if (hasForceArg(args)) {
            return;
        }
        if (voteManager.areAllEligiblePlayersReady()) {
            return;
        }

        event.setCancelled(true);
        sendNotReadyMessage(event.getSender());
    }

    private boolean isNotSetPhaseToHiding(List<String> args) {
        if (args.size() < 3) {
            return true;
        }

        String root = args.get(0).toLowerCase(Locale.ROOT);
        if (!root.equals("mg") && !root.equals("minigame")) {
            return true;
        }

        return !args.get(1).equalsIgnoreCase("setphase") || !args.get(2).equalsIgnoreCase("hiding");
    }

    private boolean hasForceArg(List<String> args) {
        return args.stream().anyMatch(arg -> arg.equalsIgnoreCase("force") || arg.equalsIgnoreCase("--force"));
    }

    private List<String> splitCommand(String rawCommand) {
        String[] split = rawCommand.trim().split("\\s+");
        List<String> result = new ArrayList<>();
        for (String part : split) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }

    private void sendNotReadyMessage(CommandSender sender) {
        Set<UUID> notReady = plugin.getVoteManager().getNotReadyOnlinePlayerIds();
        String names = notReady.stream()
                .map(id -> plugin.getServer().getPlayer(id))
                .filter(Objects::nonNull)
                .map(Player::getName)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));

        sender.sendMessage(Component.text("Cannot switch to hiding: not everyone is ready.", NamedTextColor.RED));
        if (!names.isEmpty()) {
            sender.sendMessage(Component.text("Not ready: " + names, NamedTextColor.YELLOW));
        }
        sender.sendMessage(Component.text("Use /mg setphase hiding force to override.", NamedTextColor.GRAY));
    }
}
