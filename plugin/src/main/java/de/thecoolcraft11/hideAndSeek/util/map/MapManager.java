package de.thecoolcraft11.hideAndSeek.util.map;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockStateFilter;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
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
import java.util.stream.Stream;

public class MapManager {
    private static final String WORKING_WORLD_PREFIX = "has_";
    private final HideAndSeek plugin;
    private final Map<String, MapData> mapDataCache;
    private final Map<String, Object> previousSettingValues;

    public MapManager(HideAndSeek plugin) {
        this.plugin = plugin;
        this.mapDataCache = new HashMap<>();
        this.previousSettingValues = new LinkedHashMap<>();
        loadMapConfigurations();
    }

    public List<String> getAvailableMaps() {
        return plugin.getConfig().getStringList("maps");
    }

    public List<String> getMapsForVoting() {
        List<String> maps = new ArrayList<>(mapDataCache.keySet());
        if (maps.isEmpty()) {
            maps = new ArrayList<>(getAvailableMaps());
        }
        maps.removeIf(name -> name == null || name.isBlank());
        maps.sort(String.CASE_INSENSITIVE_ORDER);
        return maps;
    }

    public List<String> getAvailableMapsForMode(GameModeEnum mode) {
        List<String> maps = getMapsForVoting();
        if (mode == null) {
            return maps;
        }

        List<String> filtered = new ArrayList<>();
        for (String mapName : maps) {
            MapData mapData = getMapData(mapName);
            if (mapData == null || mapData.getPreferredModes().isEmpty() || mapData.getPreferredModes().contains(mode)) {
                filtered.add(mapName);
            }
        }
        return filtered;
    }

    public List<String> getAvailableMapsByPreferredMode() {
        List<String> allMaps = getAvailableMaps();


        var usePreferredModesResult = plugin.getSettingService().getSetting("game.maps.use-preferred-modes");
        Object usePreferredModesObj = usePreferredModesResult.isSuccess() ? usePreferredModesResult.getValue() : true;
        boolean usePreferredModes = (usePreferredModesObj instanceof Boolean) ? (Boolean) usePreferredModesObj : true;

        if (!usePreferredModes) {
            return allMaps;
        }


        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
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

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Filtered " + filteredMaps.size() + " maps for mode " + currentMode + " out of " + allMaps.size() + " total maps");
        }
        return filteredMaps;
    }

    public MapData getMapData(String mapName) {
        return mapDataCache.get(mapName);
    }

    public Material getMapIconMaterial(String mapName, Material fallback) {
        Material safeFallback = (fallback == null || fallback.isAir()) ? Material.GRASS_BLOCK : fallback;
        if (mapName == null || mapName.isBlank()) {
            return safeFallback;
        }

        MapData mapData = getMapData(mapName);
        if (mapData == null || mapData.getIcon() == null || mapData.getIcon().isBlank()) {
            return safeFallback;
        }

        String iconName = mapData.getIcon().trim();
        Material iconMaterial = resolveIconMaterial(iconName);
        if (iconMaterial == null) {
            plugin.getLogger().warning("Invalid icon material for map '" + mapName + "': " + iconName + ". Falling back to " + safeFallback.name());
            return safeFallback;
        }
        if (iconMaterial.isAir()) {
            plugin.getLogger().warning("Map icon material cannot be air for map '" + mapName + "': " + iconName + ". Falling back to " + safeFallback.name());
            return safeFallback;
        }
        return iconMaterial;
    }

    private Material resolveIconMaterial(String rawIconName) {
        if (rawIconName == null || rawIconName.isBlank()) {
            return null;
        }


        String normalizedName = rawIconName.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase(Locale.ROOT);

        Material material = Material.getMaterial(normalizedName);
        if (material != null) {
            return material;
        }


        material = Material.matchMaterial(rawIconName);
        if (material != null) {
            return material;
        }


        return Material.matchMaterial(normalizedName);
    }

    public void applySettingOverridesForMap(String mapName) {
        clearAppliedSettingOverrides();

        var useOverridesResult = plugin.getSettingService().getSetting("game.maps.use-map-setting-overrides");
        Object useOverridesObj = useOverridesResult.isSuccess() ? useOverridesResult.getValue() : true;
        boolean useOverrides = (useOverridesObj instanceof Boolean) ? (Boolean) useOverridesObj : true;
        if (!useOverrides || mapName == null || mapName.isBlank()) {
            return;
        }

        MapData mapData = getMapData(mapName);
        if (mapData == null || mapData.getSettingOverrides().isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : mapData.getSettingOverrides().entrySet()) {
            String settingKey = entry.getKey();
            if (settingKey == null || settingKey.isBlank()) {
                continue;
            }

            var currentValueResult = plugin.getSettingService().getSetting(settingKey);
            if (!currentValueResult.isSuccess()) {
                plugin.getLogger().warning("Skipping unknown map setting override '" + settingKey + "' on map " + mapName);
                continue;
            }

            previousSettingValues.putIfAbsent(settingKey, currentValueResult.getValue());
            Object overrideValue = entry.getValue();
            var setResult = plugin.getSettingService().setSetting(settingKey, overrideValue == null ? "" : String.valueOf(overrideValue));
            if (!setResult.isSuccess()) {
                plugin.getLogger().warning("Failed to apply map setting override '" + settingKey + "' for map " + mapName + ": " + setResult.getErrorMessage());
            }
        }
    }

    public void clearAppliedSettingOverrides() {
        if (previousSettingValues.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : previousSettingValues.entrySet()) {
            Object value = entry.getValue();
            var setResult = plugin.getSettingService().setSetting(entry.getKey(), value == null ? "" : String.valueOf(value));
            if (!setResult.isSuccess()) {
                plugin.getLogger().warning("Failed to restore setting '" + entry.getKey() + "' after map overrides: " + setResult.getErrorMessage());
            }
        }

        previousSettingValues.clear();
    }


    private void loadMapConfigurations() {
        mapDataCache.clear();


        ConfigurationSection mapConfig = plugin.getConfig().getConfigurationSection("map-config");
        if (mapConfig != null) {
            for (String mapName : mapConfig.getKeys(false)) {
                MapData mapData = loadMapFromConfig(mapName, mapConfig.getConfigurationSection(mapName));
                if (mapData != null) {
                    mapDataCache.put(mapName, mapData);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Loaded map config for: " + mapName);
                    }
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
                        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info("Loaded map from maps.yml: " + mapName);
                        }
                    }
                }
            }
        }
    }


    private MapData loadMapFromConfig(String mapName, ConfigurationSection section) {
        if (section == null) return null;

        MapData mapData = new MapData(mapName);


        String prettyName = section.getString("pretty-name");
        if (prettyName == null || prettyName.isBlank()) {
            prettyName = section.getString("prettyName");
        }
        mapData.setPrettyName(prettyName);


        String description = section.getString("description", "");
        mapData.setDescription(description);


        String author = section.getString("author");
        if (author != null && !author.isEmpty()) {
            mapData.setAuthor(author);
        }


        String size = section.getString("size");
        if (size != null && !size.isEmpty()) {
            mapData.setSize(size);
        }

        String icon = section.getString("icon");
        mapData.setIcon(icon);


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
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Loaded " + allowedBlockStrings.size() + " allowed block patterns for map " + mapName);
            }
        }


        mapData.setSeekerBreakBlocks(section.getStringList("seeker-break-blocks"));
        mapData.setBlockInteractionExceptions(section.getStringList("block-interaction-exceptions"));
        mapData.setBlockPhysicsExceptions(section.getStringList("block-physics-exceptions"));

        ConfigurationSection settingOverridesSection = section.getConfigurationSection("setting-overrides");
        if (settingOverridesSection != null) {
            Map<String, Object> overrides = new LinkedHashMap<>();
            flattenSettingOverrides(settingOverridesSection, "", overrides);
            mapData.setSettingOverrides(overrides);
        }


        ConfigurationSection playersSection = section.getConfigurationSection("players");
        if (playersSection != null) {
            if (playersSection.contains("min")) {
                mapData.setMinPlayers(playersSection.getInt("min"));
            }
            if (playersSection.contains("recommended")) {
                mapData.setRecommendedPlayers(playersSection.getInt("recommended"));
            }
            if (playersSection.contains("max")) {
                mapData.setMaxPlayers(playersSection.getInt("max"));
            }
        }


        ConfigurationSection seekersSection = section.getConfigurationSection("seekers");
        if (seekersSection != null) {
            if (seekersSection.contains("min")) {
                mapData.setMinSeekers(seekersSection.getInt("min"));
            }
            if (seekersSection.contains("per-players")) {
                mapData.setSeekersPerPlayers(seekersSection.getInt("per-players"));
            }
            if (seekersSection.contains("max")) {
                mapData.setMaxSeekers(seekersSection.getInt("max"));
            }
        }


        ConfigurationSection timingsSection = section.getConfigurationSection("timings");
        if (timingsSection != null) {
            if (timingsSection.contains("hiding-time")) {
                mapData.setHidingTime(timingsSection.getInt("hiding-time"));
            }
            if (timingsSection.contains("seeking-time")) {
                mapData.setSeekingTime(timingsSection.getInt("seeking-time"));
            }
        }

        mapData.setVendingMachineLocations(section.getStringList("vending-machine-locations"));

        return mapData;
    }

    private void flattenSettingOverrides(ConfigurationSection section, String prefix, Map<String, Object> output) {
        for (String key : section.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }

            String fullKey = prefix.isEmpty() ? key : prefix + "." + key;
            Object value = section.get(key);
            if (value instanceof ConfigurationSection nestedSection) {
                flattenSettingOverrides(nestedSection, fullKey, output);
            } else {
                output.put(fullKey, value);
            }
        }
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

    public World selectAndCopyMap(int playerCount) {
        String selectedMap = selectRandomMapName(playerCount);
        if (selectedMap == null || selectedMap.isEmpty()) {
            return null;
        }

        return copyMapToWorkingWorld(selectedMap);
    }

    public String selectRandomMapName(int playerCount) {
        List<String> maps = getAvailableMapsByPreferredMode();
        if (maps.isEmpty()) {
            plugin.getLogger().warning("No maps configured in config.yml!");
            return null;
        }

        if (playerCount > 0) {
            maps = MapConfigHelper.filterMapsByPlayerCount(plugin, maps, playerCount);
        }

        String selectedMap = maps.get((int) (Math.random() * maps.size()));
        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Selected map: " + selectedMap);
        }
        return selectedMap;
    }

    private static void applySafeGameRules(World world) {
        boolean oldFieldsExist = fieldExists();

        if (oldFieldsExist) {
            setGamerulesForOlderVersion(world);
        } else {
            setGamerulesForNewVersion(world);
        }
    }

    private static boolean fieldExists() {
        try {
            GameRule.class.getDeclaredField("DO_IMMEDIATE_RESPAWN");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    @SuppressWarnings("removal")
    private static void setGamerulesForOlderVersion(World world) {
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.LOCATOR_BAR, false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
    }

    private static void setGamerulesForNewVersion(World world) {
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRules.LOCATOR_BAR, false);
        world.setGameRule(GameRules.SPAWN_MOBS, false);
        world.setGameRule(GameRules.NATURAL_HEALTH_REGENERATION, false);
        world.setGameRule(GameRules.BLOCK_DROPS, false);
        world.setGameRule(GameRules.ENTITY_DROPS, false);
        world.setGameRule(GameRules.MOB_DROPS, false);
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
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

                if (uidFile.delete() && plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Deleted uid.dat from working world to prevent duplicate error");
                }
            }


            WorldCreator worldCreator = new WorldCreator(workingWorldName);
            worldCreator.copy(sourceWorld);
            World workingWorld = worldCreator.createWorld();

            if (workingWorld != null) {
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Created working world: " + workingWorldName);
                }
                workingWorld.setAutoSave(false);

                applySafeGameRules(workingWorld);

                workingWorld.getWorldBorder().setDamageAmount(0);
                workingWorld.getWorldBorder().setDamageBuffer(0);
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
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Using configured spawn point #" + spawnWithBorder.spawnIndex() + " from MapData for map: " + mapName);
                }


                if (!mapData.getWorldBorders().isEmpty() && spawnWithBorder.borderIndex() >= 0) {
                    mapData.applyWorldBorder(workingWorld, spawnWithBorder.borderIndex());
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Applied world border #" + spawnWithBorder.borderIndex() + " for spawn #" + spawnWithBorder.spawnIndex());
                    }


                    HideAndSeek.getDataController().setCurrentBorderIndex(spawnWithBorder.borderIndex());
                } else {
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("No world borders configured for this map");
                    }
                    HideAndSeek.getDataController().setCurrentBorderIndex(-1);
                }
            } else {

                spawnLocation = workingWorld.getSpawnLocation();
                spawnLocation.setY(spawnLocation.getY() + 1);
                plugin.getLogger().warning("Failed to get spawn point for map " + mapName + ", using world spawn");
                HideAndSeek.getDataController().setCurrentBorderIndex(-1);
            }
        } else {

            spawnLocation = workingWorld.getSpawnLocation();
            spawnLocation.setY(spawnLocation.getY() + 1);
            plugin.getLogger().warning("No spawn points configured for map " + mapName + ", using world spawn");
            HideAndSeek.getDataController().setCurrentBorderIndex(-1);
        }


        HideAndSeek.getDataController().setRoundSpawnPoint(spawnLocation);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);
        }

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Teleported all players to map world: " + workingWorld.getName());
        }
    }


    public void deleteWorkingWorld(String mapName) {
        try {
            String workingWorldName = WORKING_WORLD_PREFIX + mapName;
            World world = Bukkit.getWorld(workingWorldName);

            if (world != null) {

                Bukkit.unloadWorld(world, false);
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Unloaded world: " + workingWorldName);
                }
            }


            deleteWorldIfExists(workingWorldName);
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Deleted working world: " + workingWorldName);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to delete working world: " + e.getMessage());
        }
    }


    private void deleteWorldIfExists(String worldName) {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            boolean success = Bukkit.unloadWorld(world, false);
            if (!success) {
                plugin.getLogger().warning("Failed to unload world: " + worldName);
                return;
            }
        }

        File worldDir = new File(Bukkit.getWorldContainer(), worldName);

        if (!worldDir.exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(worldDir.toPath())) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            plugin.getLogger().warning(
                                    "Failed to delete: " + path + " (" + e.getMessage() + ")"
                            );
                        }
                    });
        } catch (IOException e) {
            plugin.getLogger().warning(
                    "Failed to delete world directory: " + worldName + " (" + e.getMessage() + ")"
            );
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
        var disallowedStatesResult = plugin.getSettingService().getSetting("disallowed-blockstates");
        Object rawStates = disallowedStatesResult.isSuccess() ? disallowedStatesResult.getValue() : List.of();
        if (!(rawStates instanceof List<?> disallowedStates)) {
            return;
        }

        for (Object stateObj : disallowedStates) {
            if (!(stateObj instanceof String state) || state.isBlank()) {
                continue;
            }
            String property = state.trim();
            BlockStateFilter.addDisallowedProperty(property);
            plugin.getLogger().info("Disallowed blockstate property: " + property);
        }
    }
}
