package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.ReloadAction;
import de.thecoolcraft11.minigameframework.commands.ReloadActionRegistry;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class ReloadActionRegistrar {

    public static void registerAll(HideAndSeek plugin) {
        ReloadActionRegistry.register(new ReloadAction() {
            @Override
            public @NotNull String getName() {
                return "maps";
            }

            @Override
            public void handle(@NotNull CommandSender sender) {
                plugin.getMapManager().reloadMaps();
                plugin.updateWorldIconsForAllMaps();
                sender.sendMessage(plugin.tr(sender, "command.reload.maps"));
            }
        });

        ReloadActionRegistry.register(new ReloadAction() {
            @Override
            public @NotNull String getName() {
                return "skins";
            }

            @Override
            public void handle(@NotNull CommandSender sender) {
                plugin.getSkinManager().loadSkins();
                sender.sendMessage(plugin.tr(sender, "command.reload.skins"));
            }
        });
    }
}
