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
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
            return;
        }

        if (args.length > 0 && "admin".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission(ADMIN_PERMISSION)) {
                sender.sendMessage(Component.text("You don't have permission to use admin loadout management!", NamedTextColor.RED));
                return;
            }
            plugin.getAdminLoadoutManagementGUI().open(player);
            return;
        }

        plugin.getLoadoutGUI().open(player);
    }
}
