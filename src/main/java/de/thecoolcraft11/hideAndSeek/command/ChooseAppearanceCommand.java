package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.AppearanceGUI;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ChooseAppearanceCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;
    private final AppearanceGUI gui;
    private static final String PERMISSION = "hideandseek.command.chooseappearance";

    public ChooseAppearanceCommand(HideAndSeek plugin) {
        this.plugin = plugin;
        this.gui = new AppearanceGUI(plugin, plugin.getBlockSelectorGUI());
    }

    @Override
    public @NotNull String getName() {
        return "chooseappearance";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("appearance", "ca", "capp");
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

        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can customize appearance!", NamedTextColor.RED));
            return true;
        }

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;

        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            player.sendMessage(Component.text("Block mode is not enabled!", NamedTextColor.RED));
            return true;
        }

        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        if (!currentPhase.equals("hiding") && !currentPhase.equals("seeking")) {
            player.sendMessage(Component.text("You can only customize appearance during Hiding or Seeking phases!", NamedTextColor.RED));
            return true;
        }

        gui.open(player);
        return true;
    }
}

