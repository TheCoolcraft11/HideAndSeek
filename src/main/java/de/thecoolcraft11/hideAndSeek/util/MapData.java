package de.thecoolcraft11.hideAndSeek.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;


public class MapData {
    private final String name;
    private String description;
    private final List<SpawnPoint> spawnPoints;
    private final List<WorldBorderData> worldBorders;
    private final List<GameModeEnum> preferredModes;
    private final List<String> allowedBlocks;

    public MapData(String name) {
        this.name = name;
        this.description = "";
        this.spawnPoints = new ArrayList<>();
        this.worldBorders = new ArrayList<>();
        this.preferredModes = new ArrayList<>();
        this.allowedBlocks = new ArrayList<>();
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

    public void addAllowedBlock(String block) {
        if (!this.allowedBlocks.contains(block)) {
            this.allowedBlocks.add(block);
        }
    }

    public void setAllowedBlocks(List<String> blocks) {
        this.allowedBlocks.clear();
        if (blocks != null) {
            this.allowedBlocks.addAll(blocks);
        }
    }

    public SpawnPoint getRandomSpawnPoint() {
        if (spawnPoints.isEmpty()) {
            return null;
        }
        return spawnPoints.get((int) (Math.random() * spawnPoints.size()));
    }

    public SpawnPoint getSpawnPoint(int index) {
        if (spawnPoints.isEmpty() || index < 0 || index >= spawnPoints.size()) {
            return null;
        }
        return spawnPoints.get(index);
    }

    public WorldBorderData getWorldBorder(int index) {
        if (worldBorders.isEmpty() || index < 0 || index >= worldBorders.size()) {
            return null;
        }
        return worldBorders.get(index);
    }

    public void applyWorldBorders(World world) {
        if (world == null) return;


        if (worldBorders.isEmpty()) {
            return;
        }


        WorldBorderData borderData = worldBorders.getFirst();
        WorldBorder border = world.getWorldBorder();
        border.setCenter(borderData.centerX(), borderData.centerZ());
        border.setSize(borderData.radius() * 2);
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


        int borderIndex = (spawnPoints.size() == worldBorders.size()) ? spawnIndex : 0;

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

