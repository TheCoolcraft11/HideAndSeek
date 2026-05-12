package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.block.BlockListParser;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DebugConfigCommand implements DebugSubcommand {
    private final HideAndSeek plugin;

    public DebugConfigCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            return DebugSubcommand.filterByPrefix(List.of("test", "validate"), args[0]);
        }
        if (args.length == 2 && ("test".equalsIgnoreCase(args[0]) || "validate".equalsIgnoreCase(args[0]))) {
            return DebugSubcommand.filterByPrefix(List.of("-a"), args[1]);
        }
        return List.of();
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage(plugin.tr(sender, "command.debug.config.usage"));
            return true;
        }

        String action = args[0].toLowerCase();
        boolean testAllBorders = args.length > 1 && "-a".equalsIgnoreCase(args[1]);
        switch (action) {
            case "test", "validate" -> {
                sender.sendMessage(plugin.tr(sender, "command.debug.config.validating"));

                ValidationResult configResult = validateConfig(sender);
                ValidationResult mapsResult = validateMaps(sender, testAllBorders);

                List<String> errors = new ArrayList<>();
                errors.addAll(configResult.errors());
                errors.addAll(mapsResult.errors());

                List<String> warnings = new ArrayList<>();
                warnings.addAll(configResult.warnings());
                warnings.addAll(mapsResult.warnings());

                if (errors.isEmpty() && warnings.isEmpty()) {
                    sender.sendMessage(plugin.tr(sender, "command.debug.config.valid"));
                    return true;
                }

                if (!errors.isEmpty()) {
                    sender.sendMessage(plugin.tr(sender, "command.debug.config.errors_found"));
                    for (String error : errors) {
                        sender.sendMessage(Component.text("  - ").append(MiniMessage.miniMessage().deserialize(error)));
                    }
                }

                if (!warnings.isEmpty()) {
                    sender.sendMessage(plugin.tr(sender, "command.debug.config.warnings_found"));
                    for (String warning : warnings) {
                        sender.sendMessage(
                                Component.text("  - ").append(MiniMessage.miniMessage().deserialize(warning)));
                    }
                }
            }
            default -> sender.sendMessage(
                    plugin.tr(sender, "command.debug.config.unknown_action", Map.of("action", action)));
        }

        return true;
    }

    private ValidationResult validateConfig(CommandSender sender) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.config_not_found"));
            return new ValidationResult(errors, warnings);
        }

        YamlConfiguration config = loadStrictYaml(configFile, errors, sender, "config.yml");
        if (config == null) {
            return new ValidationResult(errors, warnings);
        }

        if (config.getKeys(false).isEmpty()) {
            warnings.add(plugin.trText(sender, "command.debug.config.errors.config_empty"));
        }

        if (config.contains("maps") && !config.isList("maps")) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.maps_not_list"));
        } else {
            for (String mapName : config.getStringList("maps")) {
                if (mapName == null || mapName.isBlank()) {
                    errors.add(plugin.trText(sender, "command.debug.config.errors.maps_empty_value"));
                }
            }
        }

        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            validateMaterialRuleList(settings, "seeker-break-blocks", "config.yml settings", errors, sender);
            validateMaterialRuleList(settings, "block-interaction-exceptions", "config.yml settings", errors, sender);
            validateMaterialRuleList(settings, "block-physics-exceptions", "config.yml settings", errors, sender);

            if (settings.contains("disallowed-blockstates") && !settings.isList("disallowed-blockstates")) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.disallowed_blockstates_not_list"));
            }
            if (settings.contains("game.apply-player-direction") && !settings.isBoolean(
                    "game.apply-player-direction")) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.apply_player_direction_not_bool"));
            }
            if (settings.contains("game.max-air-above-liquid")) {
                if (!settings.isInt("game.max-air-above-liquid")) {
                    errors.add(plugin.trText(sender, "command.debug.config.errors.max_air_above_liquid_not_int"));
                } else if (settings.getInt("game.max-air-above-liquid") < 0) {
                    errors.add(plugin.trText(sender, "command.debug.config.errors.max_air_above_liquid_negative"));
                }
            }
        }

        if (config.contains("disallowed-blockstates") || config.contains("seeker-break-blocks") ||
                config.contains("block-interaction-exceptions") || config.contains("block-physics-exceptions") ||
                config.contains("game.apply-player-direction") || config.contains("game.max-air-above-liquid")) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.legacy_root_keys"));
        }

        return new ValidationResult(errors, warnings);
    }

    private ValidationResult validateMaps(CommandSender sender, boolean testAllBorders) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        File mapsFile = new File(plugin.getDataFolder(), "maps.yml");
        if (!mapsFile.exists()) {
            errors.add(plugin.trText(sender, "command.debug.config.warnings.maps_not_found"));
            return new ValidationResult(errors, warnings);
        }

        YamlConfiguration mapsConfig = loadStrictYaml(mapsFile, errors, sender, "maps.yml");
        if (mapsConfig == null) {
            return new ValidationResult(errors, warnings);
        }

        for (String mapName : mapsConfig.getKeys(false)) {
            ConfigurationSection section = mapsConfig.getConfigurationSection(mapName);
            if (section == null) {
                errors.add(
                        plugin.trText(sender, "command.debug.config.errors.map_not_section", Map.of("map", mapName)));
                continue;
            }

            validateRawMapFormat(mapName, section, errors, sender);

            try {
                MapData mapData = plugin.getMapManager().getMapData(mapName);
                if (mapData == null) {
                    warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_not_loaded",
                            Map.of("map", mapName)));
                    continue;
                }

                World world = Bukkit.getWorld(mapName);
                if (world == null) {
                    warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_world_not_loaded",
                            Map.of("map", mapName)));
                }

                List<MapData.SpawnPoint> spawns = mapData.getSpawnPoints();
                if (spawns.isEmpty()) {
                    warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_no_spawns",
                            Map.of("map", mapName)));
                } else if (world != null) {
                    for (int i = 0; i < spawns.size(); i++) {
                        validateSpawnPoint(mapName, mapData, world, spawns.get(i), i, testAllBorders, warnings, sender);
                    }
                }

                List<MapData.WorldBorderData> borders = mapData.getWorldBorders();
                for (int i = 0; i < borders.size(); i++) {
                    String borderWarning = validateWorldBorder(mapName, borders.get(i), i, sender);
                    if (borderWarning != null) {
                        warnings.add(borderWarning);
                    }
                }
            } catch (Exception e) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.validation_error",
                        Map.of("map", mapName, "error", e.getMessage())));
            }
        }

        return new ValidationResult(errors, warnings);
    }

    private void validateRawMapFormat(String mapName, ConfigurationSection section, List<String> errors, CommandSender sender) {
        validateSpawnPointListFormat(mapName, section, errors, sender);
        validateWorldBorderListFormat(mapName, section, errors, sender);
        validateModeListFormat(mapName, section, errors, sender);

        if (section.contains("icon")) {
            String icon = section.getString("icon");
            if (icon == null || icon.isBlank()) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.icon_empty", Map.of("map", mapName)));
            } else if (Material.matchMaterial(icon.trim()) == null) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.icon_invalid",
                        Map.of("map", mapName, "material", icon)));
            }
        }

        validateMaterialRuleList(section, "allowed-blocks", "map '" + mapName + "'", errors, sender);
        validateMaterialRuleList(section, "seeker-break-blocks", "map '" + mapName + "'", errors, sender);
        validateMaterialRuleList(section, "block-interaction-exceptions", "map '" + mapName + "'", errors, sender);
        validateMaterialRuleList(section, "block-physics-exceptions", "map '" + mapName + "'", errors, sender);
    }

    private void validateSpawnPointListFormat(String mapName, ConfigurationSection section, List<String> errors, CommandSender sender) {
        if (section.contains("spawn-points") && !section.isList("spawn-points")) {
            errors.add(
                    plugin.trText(sender, "command.debug.config.errors.spawn_points_not_list", Map.of("map", mapName)));
            return;
        }

        List<String> spawns = section.getStringList("spawn-points");
        if (spawns.isEmpty()) {
            String legacySpawn = section.getString("spawn-point");
            if (legacySpawn != null && !legacySpawn.isBlank()) {
                spawns = List.of(legacySpawn);
            }
        }

        for (int i = 0; i < spawns.size(); i++) {
            String raw = spawns.get(i);
            if (raw == null || raw.isBlank()) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.spawn_point_empty",
                        Map.of("map", mapName, "index", String.valueOf(i))));
                continue;
            }
            if (MapData.SpawnPoint.fromString(raw) == null) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.spawn_point_invalid",
                        Map.of("map", mapName, "index", String.valueOf(i), "value", raw)));
            }
        }
    }

    private void validateWorldBorderListFormat(String mapName, ConfigurationSection section, List<String> errors, CommandSender sender) {
        if (section.contains("world-borders") && !section.isList("world-borders")) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.world_borders_not_list",
                    Map.of("map", mapName)));
            return;
        }

        List<String> borders = section.getStringList("world-borders");
        for (int i = 0; i < borders.size(); i++) {
            String raw = borders.get(i);
            if (raw == null || raw.isBlank()) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.world_border_empty",
                        Map.of("map", mapName, "index", String.valueOf(i))));
                continue;
            }

            MapData.WorldBorderData border = MapData.WorldBorderData.fromString(raw);
            if (border == null) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.world_border_invalid",
                        Map.of("map", mapName, "index", String.valueOf(i), "value", raw)));
                continue;
            }

            if (border.size() <= 0) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.world_border_size",
                        Map.of("map", mapName, "index", String.valueOf(i))));
            }
        }
    }

    private void validateModeListFormat(String mapName, ConfigurationSection section, List<String> errors, CommandSender sender) {
        if (section.contains("preferred-modes") && !section.isList("preferred-modes")) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.preferred_modes_not_list",
                    Map.of("map", mapName)));
            return;
        }

        for (String mode : section.getStringList("preferred-modes")) {
            try {
                GameModeEnum.valueOf(mode.toUpperCase());
            } catch (Exception ex) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.preferred_mode_invalid",
                        Map.of("map", mapName, "mode", mode)));
            }
        }
    }

    private void validateMaterialRuleList(ConfigurationSection section, String key, String scope, List<String> errors, CommandSender sender) {
        if (section.contains(key) && !section.isList(key)) {
            errors.add(plugin.trText(sender, "command.debug.config.errors.material_rule_not_list",
                    Map.of("scope", scope, "key", key)));
            return;
        }

        for (String rule : section.getStringList(key)) {
            if (!isValidMaterialRule(rule)) {
                errors.add(plugin.trText(sender, "command.debug.config.errors.material_rule_invalid",
                        Map.of("scope", scope, "key", key, "rule", rule)));
            }
        }
    }

    private void validateSpawnPoint(String mapName,
                                    MapData mapData,
                                    World world,
                                    MapData.SpawnPoint spawn,
                                    int index,
                                    boolean testAllBorders,
                                    List<String> warnings,
                                    CommandSender sender) {
        try {
            int y = (int) spawn.y();
            if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_outside_height",
                        Map.of("map", mapName, "index", String.valueOf(index), "y", String.valueOf(y))));
                return;
            }

            Location loc = new Location(world, spawn.x(), spawn.y(), spawn.z());
            Block feet = loc.getBlock();
            Block head = loc.clone().add(0, 1, 0).getBlock();
            Block ground = loc.clone().add(0, -1, 0).getBlock();

            if (!feet.isPassable()) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_inside_block_feet",
                        Map.of("map", mapName, "index", String.valueOf(index), "material", feet.getType().name())));
            }

            if (!head.isPassable()) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_inside_block_head",
                        Map.of("map", mapName, "index", String.valueOf(index), "material", head.getType().name())));
            }

            if (ground.isPassable() || !ground.getType().isSolid()) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_no_floor",
                        Map.of("map", mapName, "index", String.valueOf(index), "material", ground.getType().name())));
            }

            int blockY = world.getHighestBlockYAt((int) spawn.x(), (int) spawn.z());
            if (y > blockY + 5) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_high_in_air",
                        Map.of("map", mapName, "index", String.valueOf(index), "y", String.valueOf(y), "ground",
                                String.valueOf(blockY))));
            }

            List<MapData.WorldBorderData> borders = mapData.getWorldBorders();
            if (borders.isEmpty()) {
                return;
            }

            if (testAllBorders) {
                for (int borderIndex = 0; borderIndex < borders.size(); borderIndex++) {
                    if (isOutsideConfiguredBorder(spawn, borders.get(borderIndex))) {
                        warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_outside_border",
                                Map.of("map", mapName, "index", String.valueOf(index), "border",
                                        String.valueOf(borderIndex))));
                    }
                }
                return;
            }

            int assignedBorderIndex = getAssignedBorderIndex(borders, index);
            if (assignedBorderIndex < 0 || assignedBorderIndex >= borders.size()) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_no_matching_border",
                        Map.of("map", mapName, "index", String.valueOf(index), "border", String.valueOf(index))));
                return;
            }

            MapData.WorldBorderData assigned = borders.get(assignedBorderIndex);
            if (isOutsideConfiguredBorder(spawn, assigned)) {
                warnings.add(plugin.trText(sender, "command.debug.config.warnings.map_spawn_outside_assigned_border",
                        Map.of("map", mapName, "index", String.valueOf(index), "border",
                                String.valueOf(assignedBorderIndex))));
            }
        } catch (Exception e) {
            warnings.add(plugin.trText(sender, "command.debug.config.warnings.spawn_validation_error",
                    Map.of("index", String.valueOf(index), "map", mapName, "error", e.getMessage())));
        }
    }

    private int getAssignedBorderIndex(List<MapData.WorldBorderData> borders, int spawnIndex) {
        if (spawnIndex < borders.size()) {
            return spawnIndex;
        }
        return -1;
    }

    private String validateWorldBorder(String mapName, MapData.WorldBorderData border, int index, CommandSender sender) {
        try {
            World world = Bukkit.getWorld(mapName);
            if (world == null) {
                return plugin.trText(sender, "command.debug.config.warnings.map_world_not_found",
                        Map.of("map", mapName, "index", String.valueOf(index)));
            }

            double radius = border.size() / 2.0;
            if (radius <= 0) {
                return plugin.trText(sender, "command.debug.config.warnings.map_border_invalid_radius",
                        Map.of("map", mapName, "index", String.valueOf(index), "radius", String.valueOf(radius)));
            }

            if (radius > 100000) {
                return plugin.trText(sender, "command.debug.config.warnings.map_border_radius_large",
                        Map.of("map", mapName, "index", String.valueOf(index), "radius", String.valueOf(radius)));
            }
        } catch (Exception e) {
            return plugin.trText(sender, "command.debug.config.warnings.border_validation_error",
                    Map.of("index", String.valueOf(index), "map", mapName, "error", e.getMessage()));
        }

        return null;
    }

    private boolean isValidMaterialRule(String rule) {
        if (rule == null || rule.isBlank()) {
            return false;
        }

        String trimmed = rule.trim().toUpperCase();


        if (BlockAppearanceConfig.parse(trimmed) != null) {
            return true;
        }


        if (trimmed.startsWith("#") || trimmed.startsWith("*")) {
            if (!BlockListParser.parseBlockList(trimmed).isEmpty()) {
                return true;
            }
        }


        if (trimmed.contains("*")) {
            return true;
        }

        String normalized = trimmed;
        int blockStateIndex = normalized.indexOf('[');
        if (blockStateIndex >= 0) {
            normalized = normalized.substring(0, blockStateIndex);
        }
        int replacementIndex = normalized.indexOf('{');
        if (replacementIndex >= 0) {
            normalized = normalized.substring(0, replacementIndex);
        }

        if (normalized.isBlank()) {
            return false;
        }

        return Material.matchMaterial(normalized) != null;
    }

    private boolean isOutsideConfiguredBorder(MapData.SpawnPoint spawn, MapData.WorldBorderData border) {
        double half = border.size() / 2.0;
        double minX = border.centerX() - half;
        double maxX = border.centerX() + half;
        double minZ = border.centerZ() - half;
        double maxZ = border.centerZ() + half;
        return !(spawn.x() >= minX) || !(spawn.x() <= maxX) || !(spawn.z() >= minZ) || !(spawn.z() <= maxZ);
    }

    private YamlConfiguration loadStrictYaml(File file, List<String> errors, CommandSender sender, String label) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            return yaml;
        } catch (InvalidConfigurationException e) {
            errors.add(plugin.trText(sender, "command.debug.config.warnings.yaml_syntax_error",
                    Map.of("file", label, "error", e.getMessage())));
        } catch (IOException e) {
            errors.add(plugin.trText(sender, "command.debug.config.warnings.yaml_read_error",
                    Map.of("file", label, "error", e.getMessage())));
        }
        return null;
    }

    private record ValidationResult(List<String> errors, List<String> warnings) {
    }
}
