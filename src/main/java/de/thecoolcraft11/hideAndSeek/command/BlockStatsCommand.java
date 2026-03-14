package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.BlockStatsGUI;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BlockStatsCommand implements MinigameSubcommand {
    private final BlockStatsGUI gui;
    private static final String PERMISSION = "hideandseek.command.blockstats";

    public BlockStatsCommand(HideAndSeek plugin) {
        this.gui = new BlockStatsGUI(plugin);
    }

    @Override
    public @NotNull String getName() {
        return "blockstats";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("bs", "blocks");
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return true;
        }

        if (!HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only seekers can use this command!", NamedTextColor.RED));
            return true;
        }

        gui.open(player);
        return true;
    }
}
