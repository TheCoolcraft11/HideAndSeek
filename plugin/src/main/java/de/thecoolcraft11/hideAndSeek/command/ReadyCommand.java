package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ReadyCommand implements MinigameSubcommand {
    private static final String PERMISSION = "hideandseek.command.ready";
    private static final String GUI_PERMISSION = "hideandseek.command.ready.gui";

    private final HideAndSeek plugin;

    public ReadyCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "ready";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("rdy");
    }

    @Override
    public @Nullable String getPermission() {
        return PERMISSION;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        VoteManager voteManager = plugin.getVoteManager();

        if (!voteManager.isReadinessEnabled()) {
            sender.sendMessage(plugin.tr(sender, "command.ready.disabled"));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
                return;
            }

            if (!sender.hasPermission(GUI_PERMISSION)) {
                sender.sendMessage(plugin.tr(sender, "command.ready.no_gui_permission"));
                return;
            }

            plugin.getReadyGUI().open(player);
            return;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.tr(sender, "common.command.no_permission"));
            return;
        }

        if (voteManager.isNotLobbyPhase()) {
            player.sendMessage(plugin.tr(player, "command.ready.only_lobby"));
            return;
        }

        boolean ready = voteManager.toggleReady(player.getUniqueId());

        player.sendMessage(plugin.tr(player, "command.ready.status", Map.of(
                "state", plugin.trText(player,
                        ready ? "command.ready.state.ready" : "command.ready.state.not_ready")
        )));

        player.playSound(player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                1.0f,
                ready ? 1.2f : 0.9f);

        if (voteManager.tryAutoStartIfEveryoneReady()) {
            plugin.broadcastTr("command.ready.all_ready_start");
        }
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1 && sender.hasPermission(GUI_PERMISSION)) {
            return Stream.of("gui")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return List.of();
    }
}

