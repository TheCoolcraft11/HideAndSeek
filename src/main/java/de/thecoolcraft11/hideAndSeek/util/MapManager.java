package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockStateFilter;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class MapManager {
    private static final String WORKING_WORLD_PREFIX = "hid_";
    private final HideAndSeek plugin;
    private final Map<String, MapData> mapDataCache;

    public MapManager(HideAndSeek plugin) {
        this.plugin = plugin;
        this.mapDataCache = new HashMap<>();
        loadMapConfigurations();
    }

    public List<String> getAvailableMaps() {
        return plugin.getConfig().getStringList("maps");
    }

    public List<String> getAvailableMapsByPreferredMode() {
        List<String> allMaps = getAvailableMaps();


        var usePreferredModesResult = plugin.getSettingService().getSetting("game.use_preferred_modes");
        Object usePreferredModesObj = usePreferredModesResult.isSuccess() ? usePreferredModesResult.getValue() : true;
        boolean usePreferredModes = (usePreferredModesObj instanceof Boolean) ? (Boolean) usePreferredModesObj : true;

        if (!usePreferredModes) {
            return allMaps;
        }


        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum currentMode = (gameModeObj instanceof GameModeEnum) ? (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;


        List<String> filteredMaps = new ArrayList<>();
        for (String mapName : allMaps) {
            MapData mapData = getMapData(mapName);
            if (mapData != null) {
                List<GameModeEnum> preferredModes = mapData.getPreferredModes();


                if (preferredModes.isEmpty() || preferredModes.contains(currentMode)) {
                    filteredMaps.add(mapName);
                }
            } else {

                filteredMaps.add(mapName);
            }
        }


        if (filteredMaps.isEmpty()) {
            plugin.getLogger().warning("No maps found with preferred mode " + currentMode + ", using all maps");
            return allMaps;
        }

        plugin.getLogger().info("Filtered " + filteredMaps.size() + " maps for mode " + currentMode + " out of " + allMaps.size() + " total maps");
        return filteredMaps;
    }

    public MapData getMapData(String mapName) {
        return mapDataCache.get(mapName);
    }


    private void loadMapConfigurations() {
        mapDataCache.clear();


        ConfigurationSection mapConfig = plugin.getConfig().getConfigurationSection("map-config");
        if (mapConfig != null) {
            for (String mapName : mapConfig.getKeys(false)) {
                MapData mapData = loadMapFromConfig(mapName, mapConfig.getConfigurationSection(mapName));
                if (mapData != null) {
                    mapDataCache.put(mapName, mapData);
                    plugin.getLogger().info("Loaded map config for: " + mapName);
                }
            }
        }


        File worldsFile = new File(plugin.getDataFolder(), "maps.yml");
        if (worldsFile.exists()) {
            FileConfiguration worldsConfig = YamlConfiguration.loadConfiguration(worldsFile);
            for (String mapName : worldsConfig.getKeys(false)) {
                ConfigurationSection section = worldsConfig.getConfigurationSection(mapName);
                if (section != null) {
                    MapData mapData = loadMapFromConfig(mapName, section);
                    if (mapData != null) {
                        mapDataCache.put(mapName, mapData);
                        plugin.getLogger().info("Loaded map from maps.yml: " + mapName);
                    }
                }
            }
        }
    }


    private MapData loadMapFromConfig(String mapName, ConfigurationSection section) {
        if (section == null) return null;

        MapData mapData = new MapData(mapName);


        String description = section.getString("description", "");
        mapData.setDescription(description);


        List<String> spawnPointStrings = section.getStringList("spawn-points");
        if (spawnPointStrings.isEmpty()) {

            String legacySpawn = section.getString("spawn-point");
            if (legacySpawn != null && !legacySpawn.isEmpty()) {
                spawnPointStrings = Collections.singletonList(legacySpawn);
            }
        }

        for (String spawnStr : spawnPointStrings) {
            MapData.SpawnPoint spawn = MapData.SpawnPoint.fromString(spawnStr);
            if (spawn != null) {
                mapData.addSpawnPoint(spawn);
            } else {
                plugin.getLogger().warning("Invalid spawn point for " + mapName + ": " + spawnStr);
            }
        }


        List<String> borderStrings = section.getStringList("world-borders");
        for (String borderStr : borderStrings) {
            MapData.WorldBorderData border = MapData.WorldBorderData.fromString(borderStr);
            if (border != null) {
                mapData.addWorldBorder(border);
            } else {
                plugin.getLogger().warning("Invalid world border for " + mapName + ": " + borderStr);
            }
        }


        List<String> modeStrings = section.getStringList("preferred-modes");
        for (String modeStr : modeStrings) {
            try {
                GameModeEnum mode = GameModeEnum.valueOf(modeStr.toUpperCase());
                mapData.addPreferredMode(mode);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid game mode for " + mapName + ": " + modeStr);
            }
        }


        List<String> allowedBlockStrings = section.getStringList("allowed-blocks");
        mapData.setAllowedBlocks(allowedBlockStrings);
        if (!allowedBlockStrings.isEmpty()) {
            plugin.getLogger().info("Loaded " + allowedBlockStrings.size() + " allowed block patterns for map " + mapName);
        }

        return mapData;
    }

    public List<String> getAllowedBlocksForMap(String mapName) {
        MapData mapData = getMapData(mapName);
        if (mapData != null) {
            return mapData.getAllowedBlocks();
        }

        plugin.getLogger().warning("No map data found for " + mapName + ", returning empty allowed blocks list");
        return new ArrayList<>();
    }

    public Material getDefaultAllowedBlock(String mapName) {
        List<String> allowedBlocks = getAllowedBlocksForMap(mapName);

        if (allowedBlocks.isEmpty()) {
            plugin.getLogger().warning("No allowed blocks configured for map: " + mapName + ", defaulting to STONE");
            return Material.STONE;
        }


        for (String pattern : allowedBlocks) {
            try {

                de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig config =
                        de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig.parse(pattern);

                if (config != null) {

                    if (config.getDefaultVariant() != null) {
                        Material material = Material.valueOf(config.getDefaultVariant());
                        if (material.isBlock() && !material.isAir()) {
                            return material;
                        }
                    }


                    String baseMaterial = config.getBaseBlockType();


                    if (config.isAllowAllVariants()) {

                        for (Material mat : Material.values()) {
                            if (mat.name().endsWith(baseMaterial) && mat.isBlock() && !mat.isAir()) {
                                return mat;
                            }
                        }
                    } else {

                        Material material = Material.valueOf(baseMaterial);
                        if (material.isBlock() && !material.isAir()) {
                            return material;
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to parse block pattern: " + pattern);
            }
        }


        plugin.getLogger().warning("Could not determine default block for map: " + mapName + ", using STONE");
        return Material.STONE;
    }

    public World selectAndCopyMap() {
        List<String> maps = getAvailableMapsByPreferredMode();
        if (maps.isEmpty()) {
            plugin.getLogger().warning("No maps configured in config.yml!");
            return null;
        }

        String selectedMap = maps.get((int) (Math.random() * maps.size()));
        plugin.getLogger().info("Selected map: " + selectedMap);

        return copyMapToWorkingWorld(selectedMap);
    }

    public World copyMapToWorkingWorld(String mapName) {
        try {
            String workingWorldName = WORKING_WORLD_PREFIX + mapName;
            World sourceWorld = Bukkit.getWorld(mapName);

            if (sourceWorld == null) {
                plugin.getLogger().warning("Source world '" + mapName + "' not found!");
                return null;
            }


            deleteWorldIfExists(workingWorldName);


            File sourceDir = sourceWorld.getWorldFolder();
            File destDir = new File(Bukkit.getWorldContainer(), workingWorldName);

            copyDirectory(sourceDir.toPath(), destDir.toPath());


            File uidFile = new File(destDir, "uid.dat");
            if (uidFile.exists()) {
                uidFile.delete();
                plugin.getLogger().info("Deleted uid.dat from working world to prevent duplicate error");
            }


            WorldCreator worldCreator = new WorldCreator(workingWorldName);
            worldCreator.copy(sourceWorld);
            World workingWorld = worldCreator.createWorld();

            if (workingWorld != null) {
                plugin.getLogger().info("Created working world: " + workingWorldName);
                workingWorld.setAutoSave(false);
                workingWorld.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
                workingWorld.setGameRule(GameRule.LOCATOR_BAR, false);
                workingWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false);
                workingWorld.setGameRule(GameRule.NATURAL_REGENERATION, false);
                workingWorld.setGameRule(GameRule.DO_TILE_DROPS, false);
                workingWorld.setGameRule(GameRule.DO_ENTITY_DROPS, false);
                workingWorld.setGameRule(GameRule.DO_MOB_LOOT, false);
            }

            return workingWorld;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to copy map: " + e.getMessage());
            return null;
        }
    }

    public void teleportPlayersToMap(World workingWorld) {
        if (workingWorld == null) {
            plugin.getLogger().warning("Cannot teleport players: working world is null!");
            return;
        }

        String mapName = HideAndSeek.getDataController().getCurrentMapName();
        MapData mapData = mapName != null ? getMapData(mapName) : null;

        Location spawnLocation;

        if (mapData != null && !mapData.getSpawnPoints().isEmpty()) {

            MapData.SpawnWithBorder spawnWithBorder = mapData.getSpawnPointWithBorder();

            if (spawnWithBorder != null) {
                spawnLocation = spawnWithBorder.spawnPoint().toLocation(workingWorld);
                plugin.getLogger().info("Using configured spawn point #" + spawnWithBorder.spawnIndex() + " from MapData for map: " + mapName);


                if (!mapData.getWorldBorders().isEmpty()) {
                    mapData.applyWorldBorder(workingWorld, spawnWithBorder.borderIndex());
                    plugin.getLogger().info("Applied world border #" + spawnWithBorder.borderIndex() + " for spawn #" + spawnWithBorder.spawnIndex());
                }
            } else {

                spawnLocation = workingWorld.getSpawnLocation();
                spawnLocation.setY(spawnLocation.getY() + 1);
                plugin.getLogger().warning("Failed to get spawn point for map " + mapName + ", using world spawn");
            }
        } else {

            spawnLocation = workingWorld.getSpawnLocation();
            spawnLocation.setY(spawnLocation.getY() + 1);
            plugin.getLogger().warning("No spawn points configured for map " + mapName + ", using world spawn");
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);
        }

        plugin.getLogger().info("Teleported all players to map world: " + workingWorld.getName());
    }


    public void deleteWorkingWorld(String mapName) {
        try {
            String workingWorldName = WORKING_WORLD_PREFIX + mapName;
            World world = Bukkit.getWorld(workingWorldName);

            if (world != null) {

                Bukkit.unloadWorld(world, false);
                plugin.getLogger().info("Unloaded world: " + workingWorldName);
            }


            deleteWorldIfExists(workingWorldName);
            plugin.getLogger().info("Deleted working world: " + workingWorldName);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete working world: " + e.getMessage());
        }
    }


    private void deleteWorldIfExists(String worldName) {
        try {
            File worldDir = new File(Bukkit.getWorldContainer(), worldName);
            if (worldDir.exists()) {
                Files.walk(worldDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                plugin.getLogger().warning("Failed to delete: " + path);
                            }
                        });
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to delete world directory: " + e.getMessage());
        }
    }

    private void copyDirectory(Path source, Path dest) throws IOException {
        if (!Files.exists(dest)) {
            Files.createDirectories(dest);
        }

        try (var stream = Files.list(source)) {
            stream.forEach(sourcePath -> {
                Path destPath = dest.resolve(sourcePath.getFileName());
                try {
                    if (Files.isDirectory(sourcePath)) {
                        copyDirectory(sourcePath, destPath);
                    } else {
                        Files.copy(sourcePath, destPath);
                    }
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to copy file: " + e.getMessage());
                }
            });
        }
    }


    public String getLobbyWorld() {

        for (World world : Bukkit.getWorlds()) {
            if (!world.getName().startsWith(WORKING_WORLD_PREFIX)) {
                return world.getName();
            }
        }

        return "world";
    }

    public void loadDisallowedBlockStates() {
        BlockStateFilter.clear();
        List<String> disallowedStates = plugin.getConfig().getStringList("disallowed-blockstates");
        for (String state : disallowedStates) {
            if (!state.isEmpty()) {
                BlockStateFilter.addDisallowedProperty(state.trim());
                plugin.getLogger().info("Disallowed blockstate property: " + state.trim());
            }
        }
    }
}
