package de.thecoolcraft11.hideAndSeek.util.map;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MapConfigHelper {

    public static int calculateSeekerCount(MinigameFramework plugin, int playerCount, MapData mapData) {

        var useMapSpecificResult = plugin.getSettingService().getSetting("game.use_map_specific_seeker_count");
        Object useMapSpecificObj = useMapSpecificResult.isSuccess() ? useMapSpecificResult.getValue() : true;
        boolean useMapSpecific = (useMapSpecificObj instanceof Boolean) ? (Boolean) useMapSpecificObj : true;


        if (!useMapSpecific || mapData == null) {
            return getGlobalSeekerCount(plugin);
        }


        Integer mapMinSeekers = mapData.getMinSeekers();
        Integer mapSeekersPerPlayers = mapData.getSeekersPerPlayers();
        Integer mapMaxSeekers = mapData.getMaxSeekers();


        if (mapMinSeekers == null) {
            return getGlobalSeekerCount(plugin);
        }


        int seekers = mapMinSeekers;

        if (mapSeekersPerPlayers != null && mapSeekersPerPlayers > 0) {
            seekers += (playerCount / mapSeekersPerPlayers);
        }

        if (mapMaxSeekers != null) {
            seekers = Math.min(seekers, mapMaxSeekers);
        }

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Calculated seekers from map config: " + seekers +
                    " (min=" + mapMinSeekers + ", perX=" + mapSeekersPerPlayers +
                    ", max=" + mapMaxSeekers + ", players=" + playerCount + ")");
        }

        return seekers;
    }

    public static int getGlobalSeekerCount(MinigameFramework plugin) {
        var seekerCountResult = plugin.getSettingService().getSetting("game.seeker_count");
        Object seekerCountObj = seekerCountResult.isSuccess() ? seekerCountResult.getValue() : 1;
        int seekerCount = (seekerCountObj instanceof Integer) ? (Integer) seekerCountObj : 1;

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Using global seeker count: " + seekerCount);
        }
        return seekerCount;
    }


    public static int getHidingTime(MinigameFramework plugin, MapData mapData) {

        var useMapSpecificResult = plugin.getSettingService().getSetting("game.use_map_specific_timings");
        Object useMapSpecificObj = useMapSpecificResult.isSuccess() ? useMapSpecificResult.getValue() : true;
        boolean useMapSpecific = (useMapSpecificObj instanceof Boolean) ? (Boolean) useMapSpecificObj : true;


        if (useMapSpecific && mapData != null && mapData.getHidingTime() != null) {
            int hidingTime = mapData.getHidingTime();
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Using map-specific hiding time: " + hidingTime);
            }
            return hidingTime;
        }


        var hidingTimeResult = plugin.getSettingService().getSetting("game.hiding_time");
        Object hidingTimeObj = hidingTimeResult.isSuccess() ? hidingTimeResult.getValue() : 60;
        int hidingTime = (hidingTimeObj instanceof Integer) ? (Integer) hidingTimeObj : 60;

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Using global hiding time: " + hidingTime);
        }
        return hidingTime;
    }

    public static int getSeekingTime(MinigameFramework plugin, MapData mapData) {

        var useMapSpecificResult = plugin.getSettingService().getSetting("game.use_map_specific_timings");
        Object useMapSpecificObj = useMapSpecificResult.isSuccess() ? useMapSpecificResult.getValue() : true;
        boolean useMapSpecific = (useMapSpecificObj instanceof Boolean) ? (Boolean) useMapSpecificObj : true;


        if (useMapSpecific && mapData != null && mapData.getSeekingTime() != null) {
            int seekingTime = mapData.getSeekingTime();
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Using map-specific seeking time: " + seekingTime);
            }
            return seekingTime;
        }


        var seekingTimeResult = plugin.getSettingService().getSetting("game.seeking_time");
        Object seekingTimeObj = seekingTimeResult.isSuccess() ? seekingTimeResult.getValue() : 300;
        int seekingTime = (seekingTimeObj instanceof Integer) ? (Integer) seekingTimeObj : 300;

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Using global seeking time: " + seekingTime);
        }
        return seekingTime;
    }

    public static java.util.List<String> filterMapsByPlayerCount(
            MinigameFramework plugin,
            java.util.List<String> availableMaps,
            int playerCount) {


        var useMapSpecificResult = plugin.getSettingService().getSetting("game.use_map_specific_player_limits");
        Object useMapSpecificObj = useMapSpecificResult.isSuccess() ? useMapSpecificResult.getValue() : true;
        boolean useMapSpecific = (useMapSpecificObj instanceof Boolean) ? (Boolean) useMapSpecificObj : true;
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;

        if (!useMapSpecific) {
            if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Map-specific player limits disabled, using all available maps");
            }
            return availableMaps;
        }

        java.util.List<String> filteredMaps = new java.util.ArrayList<>();

        for (String mapName : availableMaps) {
            MapData mapData = hideAndSeekPlugin.getMapManager().getMapData(mapName);


            if (mapData == null) {
                filteredMaps.add(mapName);
                continue;
            }

            Integer minPlayers = mapData.getMinPlayers();
            Integer maxPlayers = mapData.getMaxPlayers();


            if (minPlayers != null && playerCount < minPlayers) {
                if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Map '" + mapName + "' filtered out: playerCount (" + playerCount + ") < minPlayers (" + minPlayers + ")");
                }
                continue;
            }


            if (maxPlayers != null && playerCount > maxPlayers) {
                if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Map '" + mapName + "' filtered out: playerCount (" + playerCount + ") > maxPlayers (" + maxPlayers + ")");
                }
                continue;
            }

            filteredMaps.add(mapName);
        }


        if (filteredMaps.isEmpty()) {
            plugin.getLogger().warning("No maps matched player count " + playerCount + ", using all available maps");
            return availableMaps;
        }

        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Filtered maps by player count: " + filteredMaps.size() + " out of " + availableMaps.size());
        }
        return filteredMaps;
    }

    public static List<String> getSeekerBreakBlockPatterns(HideAndSeek plugin, String mapName) {
        MapData mapData = mapName == null ? null : plugin.getMapManager().getMapData(mapName);
        if (mapData != null && !mapData.getSeekerBreakBlocks().isEmpty()) {
            return new ArrayList<>(mapData.getSeekerBreakBlocks());
        }
        return plugin.getConfig().getStringList("seeker-break-blocks");
    }

    public static List<Material> getBlockInteractionExceptions(HideAndSeek plugin, String mapName) {
        return resolveMaterialRules(plugin, mapName, "block-interaction-exceptions", true);
    }

    public static List<Material> getBlockPhysicsExceptions(HideAndSeek plugin, String mapName) {
        return resolveMaterialRules(plugin, mapName, "block-physics-exceptions", false);
    }

    private static List<Material> resolveMaterialRules(HideAndSeek plugin, String mapName, String globalPath, boolean interactionRules) {
        List<String> configuredRules;
        MapData mapData = mapName == null ? null : plugin.getMapManager().getMapData(mapName);

        if (mapData != null) {
            configuredRules = interactionRules
                    ? mapData.getBlockInteractionExceptions()
                    : mapData.getBlockPhysicsExceptions();
            if (!configuredRules.isEmpty()) {
                return parseMaterialRules(configuredRules);
            }
        }

        return parseMaterialRules(plugin.getConfig().getStringList(globalPath));
    }

    private static List<Material> parseMaterialRules(List<String> rules) {
        Set<Material> resolved = new LinkedHashSet<>();
        for (String rule : rules) {
            if (rule == null || rule.isBlank()) {
                continue;
            }

            String trimmed = rule.trim().toUpperCase();
            if (trimmed.contains("*")) {
                Pattern pattern = Pattern.compile(trimmed.replace("*", ".*"));
                for (Material material : Material.values()) {
                    if (pattern.matcher(material.name()).matches()) {
                        resolved.add(material);
                    }
                }
                continue;
            }

            Material material = Material.matchMaterial(trimmed);
            if (material != null) {
                resolved.add(material);
            }
        }

        return new ArrayList<>(resolved);
    }
}

