package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return;
        }

        plugin.getVoteGUI().open(player);
    }
}

