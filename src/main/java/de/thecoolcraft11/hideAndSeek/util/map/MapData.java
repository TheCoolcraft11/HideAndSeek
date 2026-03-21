package de.thecoolcraft11.hideAndSeek.util.map;

import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MapData {
    private final String name;
    private String description;
    private String author;
    private String size;
    private final List<SpawnPoint> spawnPoints;
    private final List<WorldBorderData> worldBorders;
    private final List<GameModeEnum> preferredModes;
    private final List<String> allowedBlocks;
    private final List<String> seekerBreakBlocks;
    private final List<String> blockInteractionExceptions;
    private final List<String> blockPhysicsExceptions;
    private final Map<String, Object> settingOverrides;


    private Integer minPlayers;
    private Integer recommendedPlayers;
    private Integer maxPlayers;


    private Integer minSeekers;
    private Integer seekersPerPlayers;
    private Integer maxSeekers;


    private Integer hidingTime;
    private Integer seekingTime;

    public MapData(String name) {
        this.name = name;
        this.description = "";
        this.author = null;
        this.size = null;
        this.spawnPoints = new ArrayList<>();
        this.worldBorders = new ArrayList<>();
        this.preferredModes = new ArrayList<>();
        this.allowedBlocks = new ArrayList<>();
        this.seekerBreakBlocks = new ArrayList<>();
        this.blockInteractionExceptions = new ArrayList<>();
        this.blockPhysicsExceptions = new ArrayList<>();
        this.settingOverrides = new LinkedHashMap<>();
        this.minPlayers = null;
        this.recommendedPlayers = null;
        this.maxPlayers = null;
        this.minSeekers = null;
        this.seekersPerPlayers = null;
        this.maxSeekers = null;
        this.hidingTime = null;
        this.seekingTime = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public List<SpawnPoint> getSpawnPoints() {
        return spawnPoints;
    }

    public void addSpawnPoint(SpawnPoint spawn) {
        this.spawnPoints.add(spawn);
    }

    public List<WorldBorderData> getWorldBorders() {
        return worldBorders;
    }

    public void addWorldBorder(WorldBorderData border) {
        this.worldBorders.add(border);
    }

    public List<GameModeEnum> getPreferredModes() {
        return preferredModes;
    }

    public void addPreferredMode(GameModeEnum mode) {
        if (!this.preferredModes.contains(mode)) {
            this.preferredModes.add(mode);
        }
    }

    public List<String> getAllowedBlocks() {
        return allowedBlocks;
    }

    public void setAllowedBlocks(List<String> blocks) {
        this.allowedBlocks.clear();
        if (blocks != null) {
            this.allowedBlocks.addAll(blocks);
        }
    }

    public List<String> getSeekerBreakBlocks() {
        return seekerBreakBlocks;
    }

    public void setSeekerBreakBlocks(List<String> blocks) {
        this.seekerBreakBlocks.clear();
        if (blocks != null) {
            this.seekerBreakBlocks.addAll(blocks);
        }
    }

    public List<String> getBlockInteractionExceptions() {
        return blockInteractionExceptions;
    }

    public void setBlockInteractionExceptions(List<String> blockInteractionExceptions) {
        this.blockInteractionExceptions.clear();
        if (blockInteractionExceptions != null) {
            this.blockInteractionExceptions.addAll(blockInteractionExceptions);
        }
    }

    public List<String> getBlockPhysicsExceptions() {
        return blockPhysicsExceptions;
    }

    public void setBlockPhysicsExceptions(List<String> blockPhysicsExceptions) {
        this.blockPhysicsExceptions.clear();
        if (blockPhysicsExceptions != null) {
            this.blockPhysicsExceptions.addAll(blockPhysicsExceptions);
        }
    }

    public Map<String, Object> getSettingOverrides() {
        return settingOverrides;
    }

    public void setSettingOverrides(Map<String, Object> settingOverrides) {
        this.settingOverrides.clear();
        if (settingOverrides != null) {
            this.settingOverrides.putAll(settingOverrides);
        }
    }


    public Integer getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(Integer minPlayers) {
        this.minPlayers = minPlayers;
    }

    public Integer getRecommendedPlayers() {
        return recommendedPlayers;
    }

    public void setRecommendedPlayers(Integer recommendedPlayers) {
        this.recommendedPlayers = recommendedPlayers;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }


    public Integer getMinSeekers() {
        return minSeekers;
    }

    public void setMinSeekers(Integer minSeekers) {
        this.minSeekers = minSeekers;
    }

    public Integer getSeekersPerPlayers() {
        return seekersPerPlayers;
    }

    public void setSeekersPerPlayers(Integer seekersPerPlayers) {
        this.seekersPerPlayers = seekersPerPlayers;
    }

    public Integer getMaxSeekers() {
        return maxSeekers;
    }

    public void setMaxSeekers(Integer maxSeekers) {
        this.maxSeekers = maxSeekers;
    }


    public Integer getHidingTime() {
        return hidingTime;
    }

    public void setHidingTime(Integer hidingTime) {
        this.hidingTime = hidingTime;
    }

    public Integer getSeekingTime() {
        return seekingTime;
    }

    public void setSeekingTime(Integer seekingTime) {
        this.seekingTime = seekingTime;
    }

    public WorldBorderData getWorldBorder(int index) {
        if (worldBorders.isEmpty() || index < 0 || index >= worldBorders.size()) {
            return null;
        }
        return worldBorders.get(index);
    }

    public void applyWorldBorder(World world, int index) {
        if (world == null) return;

        WorldBorderData borderData = getWorldBorder(index);
        if (borderData == null) {

            if (!worldBorders.isEmpty()) {
                borderData = worldBorders.getFirst();
            } else {
                return;
            }
        }

        WorldBorder border = world.getWorldBorder();
        border.setCenter(borderData.centerX(), borderData.centerZ());
        border.setSize(borderData.radius() * 2);
    }

    public SpawnWithBorder getSpawnPointWithBorder() {
        if (spawnPoints.isEmpty()) {
            return null;
        }

        int spawnIndex = (int) (Math.random() * spawnPoints.size());
        SpawnPoint spawn = spawnPoints.get(spawnIndex);

        int borderIndex;


        if (spawnPoints.size() == worldBorders.size()) {

            borderIndex = spawnIndex;
        } else if (!worldBorders.isEmpty()) {

            borderIndex = (int) (Math.random() * worldBorders.size());
        } else {

            borderIndex = -1;
        }

        return new SpawnWithBorder(spawnIndex, spawn, borderIndex);
    }

    public record SpawnWithBorder(int spawnIndex, SpawnPoint spawnPoint, int borderIndex) {
    }

    public record SpawnPoint(double x, double y, double z, float yaw, float pitch) {

        public Location toLocation(World world) {
            return new Location(world, x, y, z, yaw, pitch);
        }

        public static SpawnPoint fromString(String str) {
            try {
                String[] parts = str.split(",");
                if (parts.length < 3) return null;

                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                float yaw = parts.length > 3 ? Float.parseFloat(parts[3].trim()) : 0;
                float pitch = parts.length > 4 ? Float.parseFloat(parts[4].trim()) : 0;

                return new SpawnPoint(x, y, z, yaw, pitch);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public @NonNull String toString() {
            return x + "," + y + "," + z + "," + yaw + "," + pitch;
        }
    }

    public record WorldBorderData(double centerX, double centerZ, double radius) {

        public static WorldBorderData fromString(String str) {
            try {
                String[] parts = str.split(",");
                if (parts.length < 3) return null;

                double x = Double.parseDouble(parts[0].trim());
                double z = Double.parseDouble(parts[1].trim());
                double radius = Double.parseDouble(parts[2].trim());

                return new WorldBorderData(x, z, radius);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public @NonNull String toString() {
            return centerX + "," + centerZ + "," + radius;
        }
    }
}

