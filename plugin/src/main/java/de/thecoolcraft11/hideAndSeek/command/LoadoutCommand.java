package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LoadoutCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;
    private static final String PERMISSION = "hideandseek.command.loadout";
    private static final String ADMIN_PERMISSION = "hideandseek.command.loadout.admin";

    public LoadoutCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "loadout";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("kit", "items");
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

        if (args.length > 0 && "admin".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(plugin.tr(sender, "command.loadout.no_admin_permission"));
                return;
            }

            plugin.getAdminLoadoutManagementGUI().open(player);
            return;
        }

        plugin.getLoadoutGUI().open(player);
    }
}
