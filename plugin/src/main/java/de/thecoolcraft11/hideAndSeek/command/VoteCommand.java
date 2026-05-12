package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class VoteCommand implements MinigameSubcommand {
    private static final String PERMISSION = "hideandseek.command.vote";
    private final HideAndSeek plugin;

    public VoteCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "vote";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("voting");
    }

    @Override
    public @Nullable String getPermission() {
        return PERMISSION;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.tr(sender, "common.command.no_permission"));
            return;
        }

        plugin.getVoteGUI().open(player);
    }
}

