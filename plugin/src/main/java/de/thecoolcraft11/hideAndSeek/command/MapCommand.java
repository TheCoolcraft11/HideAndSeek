package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MapCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;
    private static final String PERMISSION = "hideandseek.command.map";

    public MapCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "map";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("selectmap", "choosemap");
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


        if (args.length == 0) {
            plugin.getMapGUI().open(player);
            return;
        }


        String mapName = args[0];


        List<String> availableMaps = plugin.getMapManager().getAvailableMaps();
        if (!availableMaps.contains(mapName)) {
            player.sendMessage(Component.text("Map '" + mapName + "' not found!", NamedTextColor.RED));
            player.sendMessage(Component.text("Available maps: " + String.join(", ", availableMaps), NamedTextColor.GRAY));
            return;
        }


        World sourceWorld = Bukkit.getWorld(mapName);
        if (sourceWorld == null) {
            player.sendMessage(Component.text("Map world '" + mapName + "' is not loaded!", NamedTextColor.RED));
            return;
        }


        HideAndSeek.getDataController().setCurrentMapName(mapName, true);
        player.sendMessage(Component.text("Map selected: ", NamedTextColor.GREEN)
                .append(Component.text(mapName, NamedTextColor.GOLD)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {

            return plugin.getMapManager().getAvailableMaps();
        }


        return plugin.getMapManager().getAvailableMaps().stream()
                .filter(map -> map.toLowerCase().startsWith(args[0].toLowerCase()))
                .toList();
    }
}
