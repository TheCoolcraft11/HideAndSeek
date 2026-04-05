package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class DebugAntiCheatCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugAntiCheatCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return DebugSubcommand.filterByPrefix(List.of("reset", "show"), args[0]);
        }
        if (args.length == 2) {
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return DebugSubcommand.filterByPrefix(players, args[1]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /has debug anticheat <reset|show> [player]", NamedTextColor.YELLOW));
            return true;
        }

        String action = args[0].toLowerCase();

        if ("reset".equals(action)) {
            return handleReset(sender, args);
        } else if ("show".equals(action)) {
            return handleShow(sender, args);
        } else {
            sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
            return true;
        }
    }

    private boolean handleReset(CommandSender sender, String[] args) {
        Player target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Please specify a player", NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        UUID targetId = target.getUniqueId();

        HideAndSeek.getDataController().removeGlowing(targetId);


        plugin.getAntiCheatVisibilityListener().refreshSoon();

        sender.sendMessage(Component.text("Anti-cheat visibility reset for ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA)));

        return true;
    }

    private boolean handleShow(CommandSender sender, String[] args) {
        Player target;

        if (args.length < 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Component.text("Please specify a player", NamedTextColor.RED));
                return true;
            }
            target = (Player) sender;
        } else {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found: " + args[1], NamedTextColor.RED));
                return true;
            }
        }

        UUID targetId = target.getUniqueId();

        HideAndSeek.getDataController().setGlowing(targetId, true);


        plugin.getAntiCheatVisibilityListener().refreshSoon();

        sender.sendMessage(Component.text("Forced visibility for ", NamedTextColor.GREEN)
                .append(Component.text(target.getName(), NamedTextColor.AQUA)));

        return true;
    }
}


