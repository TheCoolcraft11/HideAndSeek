package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

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
            sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.tr(sender, "common.command.no_permission"));
            return;
        }

        if (args.length == 0) {
            plugin.getMapGUI().open(player);
            return;
        }

        String mapName = args[0];

        List<String> availableMaps = plugin.getMapManager().getAvailableMaps();

        if (!availableMaps.contains(mapName)) {
            player.sendMessage(plugin.tr(player, "command.map.not_found",
                    Map.of("map", mapName)));

            player.sendMessage(plugin.tr(player, "command.map.available",
                    Map.of("maps", String.join(", ", availableMaps))));

            return;
        }

        World sourceWorld = Bukkit.getWorld(mapName);

        if (sourceWorld == null) {
            player.sendMessage(plugin.tr(player, "command.map.not_loaded",
                    Map.of("map", mapName)));
            return;
        }

        HideAndSeek.getDataController().setCurrentMapName(mapName, true);

        player.sendMessage(plugin.tr(player, "command.map.selected",
                Map.of("map", mapName)));

        player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP,
                1.0f,
                1.0f);
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        List<String> maps = plugin.getMapManager().getAvailableMaps();

        if (args.length == 0) {
            return maps;
        }

        String prefix = args[0].toLowerCase();
        return maps.stream()
                .filter(map -> map.toLowerCase().startsWith(prefix))
                .toList();
    }
}
