package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
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
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        VoteManager voteManager = plugin.getVoteManager();
        if (!voteManager.isReadinessEnabled()) {
            sender.sendMessage(Component.text("Readiness is disabled.", NamedTextColor.RED));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
                return true;
            }
            if (!sender.hasPermission(GUI_PERMISSION)) {
                sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                return true;
            }
            plugin.getReadyGUI().open(player);
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }
        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }
        if (voteManager.isNotLobbyPhase()) {
            player.sendMessage(Component.text("Ready status can only be changed in the lobby.", NamedTextColor.RED));
            return true;
        }

        boolean ready = voteManager.toggleReady(player.getUniqueId());
        player.sendMessage(Component.text("Ready status: ", NamedTextColor.GRAY)
                .append(Component.text(ready ? "READY" : "NOT READY", ready ? NamedTextColor.GREEN : NamedTextColor.RED)));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, ready ? 1.2f : 0.9f);

        if (voteManager.tryAutoStartIfEveryoneReady()) {
            Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
        }
        return true;
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

