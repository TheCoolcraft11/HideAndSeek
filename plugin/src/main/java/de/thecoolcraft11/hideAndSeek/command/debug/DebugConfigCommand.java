package de.thecoolcraft11.hideAndSeek.command.debug;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.block.BlockListParser;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("Usage: /has debug config [test|validate] [-a]", NamedTextColor.YELLOW));
            return true;
        }

        String action = args[0].toLowerCase();
        boolean testAllBorders = args.length > 1 && "-a".equalsIgnoreCase(args[1]);
        switch (action) {
            case "test", "validate" -> {
                sender.sendMessage(Component.text("Validating configuration...", NamedTextColor.YELLOW));

                ValidationResult configResult = validateConfig();
                ValidationResult mapsResult = validateMaps(testAllBorders);

                List<String> errors = new ArrayList<>();
                errors.addAll(configResult.errors());
                errors.addAll(mapsResult.errors());

                List<String> warnings = new ArrayList<>();
                warnings.addAll(configResult.warnings());
                warnings.addAll(mapsResult.warnings());

                if (errors.isEmpty() && warnings.isEmpty()) {
                    sender.sendMessage(Component.text("\nConfiguration is valid.", NamedTextColor.GREEN));
                    return true;
                }

                if (!errors.isEmpty()) {
                    sender.sendMessage(Component.text("\nErrors found:", NamedTextColor.RED));
                    for (String error : errors) {
                        sender.sendMessage(Component.text("  - " + error, NamedTextColor.RED));
                    }
                }

                if (!warnings.isEmpty()) {
                    sender.sendMessage(Component.text("\nWarnings:", NamedTextColor.YELLOW));
                    for (String warning : warnings) {
                        sender.sendMessage(Component.text("  - " + warning, NamedTextColor.YELLOW));
                    }
                }
            }
            default -> sender.sendMessage(Component.text("Unknown action: " + action, NamedTextColor.RED));
        }

        return true;
    }

    private ValidationResult validateConfig() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            errors.add("config.yml not found");
            return new ValidationResult(errors, warnings);
        }

        YamlConfiguration config = loadStrictYaml(configFile, errors, "config.yml");
        if (config == null) {
            return new ValidationResult(errors, warnings);
        }

        if (config.getKeys(false).isEmpty()) {
            warnings.add("config.yml appears to be empty");
        }

        if (config.contains("maps") && !config.isList("maps")) {
            errors.add("config.yml key 'maps' must be a list of world names");
        } else {
            for (String mapName : config.getStringList("maps")) {
                if (mapName == null || mapName.isBlank()) {
                    errors.add("config.yml contains an empty value in 'maps'");
                }
            }
        }

        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings != null) {
            validateMaterialRuleList(settings, "seeker-break-blocks", "config.yml settings", errors);
            validateMaterialRuleList(settings, "block-interaction-exceptions", "config.yml settings", errors);
            validateMaterialRuleList(settings, "block-physics-exceptions", "config.yml settings", errors);

            if (settings.contains("disallowed-blockstates") && !settings.isList("disallowed-blockstates")) {
                errors.add("config.yml settings key 'disallowed-blockstates' must be a list");
            }
            if (settings.contains("game.apply-player-direction") && !settings.isBoolean(
                    "game.apply-player-direction")) {
                errors.add("config.yml settings key 'game.apply-player-direction' must be true/false");
            }
            if (settings.contains("game.max-air-above-liquid")) {
                if (!settings.isInt("game.max-air-above-liquid")) {
                    errors.add("config.yml settings key 'game.max-air-above-liquid' must be an integer");
                } else if (settings.getInt("game.max-air-above-liquid") < 0) {
                    errors.add("config.yml settings key 'game.max-air-above-liquid' must be >= 0");
                }
            }
        }

        if (config.contains("disallowed-blockstates") || config.contains("seeker-break-blocks") ||
                config.contains("block-interaction-exceptions") || config.contains("block-physics-exceptions") ||
                config.contains("game.apply-player-direction") || config.contains("game.max-air-above-liquid")) {
            warnings.add(
                    "Legacy root config keys detected for block rules. Move them under settings.* (for example settings.seeker-break-blocks)");
        }

        return new ValidationResult(errors, warnings);
    }

    private ValidationResult validateMaps(boolean testAllBorders) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        File mapsFile = new File(plugin.getDataFolder(), "maps.yml");
        if (!mapsFile.exists()) {
            errors.add("maps.yml not found");
            return new ValidationResult(errors, warnings);
        }

        YamlConfiguration mapsConfig = loadStrictYaml(mapsFile, errors, "maps.yml");
        if (mapsConfig == null) {
            return new ValidationResult(errors, warnings);
        }

        for (String mapName : mapsConfig.getKeys(false)) {
            ConfigurationSection section = mapsConfig.getConfigurationSection(mapName);
            if (section == null) {
                errors.add("maps.yml map '" + mapName + "' must be a section/object");
                continue;
            }

            validateRawMapFormat(mapName, section, errors);

            try {
                MapData mapData = plugin.getMapManager().getMapData(mapName);
                if (mapData == null) {
                    warnings.add("Map '" + mapName + "' is defined but could not be loaded");
                    continue;
                }

                World world = Bukkit.getWorld(mapName);
                if (world == null) {
                    warnings.add("Map '" + mapName + "' world is not loaded (spawn checks limited)");
                }

                List<MapData.SpawnPoint> spawns = mapData.getSpawnPoints();
                if (spawns.isEmpty()) {
                    warnings.add("Map '" + mapName + "' has no spawn points");
                } else if (world != null) {
                    for (int i = 0; i < spawns.size(); i++) {
                        validateSpawnPoint(mapName, mapData, world, spawns.get(i), i, testAllBorders, warnings);
                    }
                }

                List<MapData.WorldBorderData> borders = mapData.getWorldBorders();
                for (int i = 0; i < borders.size(); i++) {
                    String borderWarning = validateWorldBorder(mapName, borders.get(i), i);
                    if (borderWarning != null) {
                        warnings.add(borderWarning);
                    }
                }
            } catch (Exception e) {
                warnings.add("Error validating map '" + mapName + "': " + e.getMessage());
            }
        }

        return new ValidationResult(errors, warnings);
    }

    private void validateRawMapFormat(String mapName, ConfigurationSection section, List<String> errors) {
        validateSpawnPointListFormat(mapName, section, errors);
        validateWorldBorderListFormat(mapName, section, errors);
        validateModeListFormat(mapName, section, errors);

        if (section.contains("icon")) {
            String icon = section.getString("icon");
            if (icon == null || icon.isBlank()) {
                errors.add("Map '" + mapName + "' has empty 'icon' value");
            } else if (Material.matchMaterial(icon.trim()) == null) {
                errors.add("Map '" + mapName + "' has invalid icon material: " + icon);
            }
        }

        validateMaterialRuleList(section, "allowed-blocks", "map '" + mapName + "'", errors);
        validateMaterialRuleList(section, "seeker-break-blocks", "map '" + mapName + "'", errors);
        validateMaterialRuleList(section, "block-interaction-exceptions", "map '" + mapName + "'", errors);
        validateMaterialRuleList(section, "block-physics-exceptions", "map '" + mapName + "'", errors);
    }

    private void validateSpawnPointListFormat(String mapName, ConfigurationSection section, List<String> errors) {
        if (section.contains("spawn-points") && !section.isList("spawn-points")) {
            errors.add("Map '" + mapName + "' key 'spawn-points' must be a list");
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
                errors.add("Map '" + mapName + "' spawn point #" + i + " is empty");
                continue;
            }
            if (MapData.SpawnPoint.fromString(raw) == null) {
                errors.add("Map '" + mapName + "' spawn point #" + i + " has invalid format: " + raw);
            }
        }
    }

    private void validateWorldBorderListFormat(String mapName, ConfigurationSection section, List<String> errors) {
        if (section.contains("world-borders") && !section.isList("world-borders")) {
            errors.add("Map '" + mapName + "' key 'world-borders' must be a list");
            return;
        }

        List<String> borders = section.getStringList("world-borders");
        for (int i = 0; i < borders.size(); i++) {
            String raw = borders.get(i);
            if (raw == null || raw.isBlank()) {
                errors.add("Map '" + mapName + "' world border #" + i + " is empty");
                continue;
            }

            MapData.WorldBorderData border = MapData.WorldBorderData.fromString(raw);
            if (border == null) {
                errors.add("Map '" + mapName + "' world border #" + i + " has invalid format: " + raw);
                continue;
            }

            if (border.size() <= 0) {
                errors.add("Map '" + mapName + "' world border #" + i + " must be > 0");
            }
        }
    }

    private void validateModeListFormat(String mapName, ConfigurationSection section, List<String> errors) {
        if (section.contains("preferred-modes") && !section.isList("preferred-modes")) {
            errors.add("Map '" + mapName + "' key 'preferred-modes' must be a list");
            return;
        }

        for (String mode : section.getStringList("preferred-modes")) {
            try {
                GameModeEnum.valueOf(mode.toUpperCase());
            } catch (Exception ex) {
                errors.add("Map '" + mapName + "' has invalid preferred mode: " + mode);
            }
        }
    }

    private void validateMaterialRuleList(ConfigurationSection section, String key, String scope, List<String> errors) {
        if (section.contains(key) && !section.isList(key)) {
            errors.add(scope + " key '" + key + "' must be a list");
            return;
        }

        for (String rule : section.getStringList(key)) {
            if (!isValidMaterialRule(rule)) {
                errors.add(scope + " has invalid material rule in '" + key + "': " + rule);
            }
        }
    }

    private void validateSpawnPoint(String mapName,
                                    MapData mapData,
                                    World world,
                                    MapData.SpawnPoint spawn,
                                    int index,
                                    boolean testAllBorders,
                                    List<String> warnings) {
        try {
            int y = (int) spawn.y();
            if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " is outside world height bounds (Y: " + y + ")");
                return;
            }

            Location loc = new Location(world, spawn.x(), spawn.y(), spawn.z());
            Block feet = loc.getBlock();
            Block head = loc.clone().add(0, 1, 0).getBlock();
            Block ground = loc.clone().add(0, -1, 0).getBlock();

            if (!feet.isPassable()) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " is inside a solid block at feet level (" + feet.getType() + ")");
            }

            if (!head.isPassable()) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " is inside a solid block at head level (" + head.getType() + ")");
            }

            if (ground.isPassable() || !ground.getType().isSolid()) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " has no solid floor below (" + ground.getType() + ")");
            }

            int blockY = world.getHighestBlockYAt((int) spawn.x(), (int) spawn.z());
            if (y > blockY + 5) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " is high in air (Y: " + y + ", ground: " + blockY + ")");
            }

            List<MapData.WorldBorderData> borders = mapData.getWorldBorders();
            if (borders.isEmpty()) {
                return;
            }

            if (testAllBorders) {
                for (int borderIndex = 0; borderIndex < borders.size(); borderIndex++) {
                    if (isOutsideConfiguredBorder(spawn, borders.get(borderIndex))) {
                        warnings.add("Map '" + mapName + "' spawn point #" + index + " is outside world border #" + borderIndex + " (-a check)");
                    }
                }
                return;
            }

            int assignedBorderIndex = getAssignedBorderIndex(borders, index);
            if (assignedBorderIndex < 0 || assignedBorderIndex >= borders.size()) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " has no matching world border #" + index);
                return;
            }

            MapData.WorldBorderData assigned = borders.get(assignedBorderIndex);
            if (isOutsideConfiguredBorder(spawn, assigned)) {
                warnings.add("Map '" + mapName + "' spawn point #" + index + " is outside its assigned world border #" + assignedBorderIndex);
            }
        } catch (Exception e) {
            warnings.add("Error validating spawn point #" + index + " in map '" + mapName + "': " + e.getMessage());
        }
    }

    private int getAssignedBorderIndex(List<MapData.WorldBorderData> borders, int spawnIndex) {
        if (spawnIndex < borders.size()) {
            return spawnIndex;
        }
        return -1;
    }

    private String validateWorldBorder(String mapName, MapData.WorldBorderData border, int index) {
        try {
            World world = Bukkit.getWorld(mapName);
            if (world == null) {
                return "Map '" + mapName + "' world not found (border #" + index + ")";
            }

            double radius = border.size() / 2.0;
            if (radius <= 0) {
                return "Map '" + mapName + "' border #" + index + " has invalid radius: " + radius;
            }

            if (radius > 100000) {
                return "Map '" + mapName + "' border #" + index + " radius is very large: " + radius;
            }
        } catch (Exception e) {
            return "Error validating world border #" + index + " in map '" + mapName + "': " + e.getMessage();
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

    private YamlConfiguration loadStrictYaml(File file, List<String> errors, String label) {
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
            return yaml;
        } catch (InvalidConfigurationException e) {
            errors.add("Invalid YAML syntax in " + label + ": " + e.getMessage());
        } catch (IOException e) {
            errors.add("Failed to read " + label + ": " + e.getMessage());
        }
        return null;
    }

    private record ValidationResult(List<String> errors, List<String> warnings) {
    }
}
