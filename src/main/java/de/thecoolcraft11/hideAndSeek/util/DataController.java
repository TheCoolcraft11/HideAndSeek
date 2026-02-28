package de.thecoolcraft11.hideAndSeek.util;

import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;

import java.util.*;

public class DataController {

    private static DataController instance;
    private List<UUID> hiders;
    private List<UUID> seekers;
    private final Map<UUID, Block> hiddenBlocks;
    private final Map<UUID, org.bukkit.Material> chosenBlocks;
    private final Map<UUID, BlockData> chosenBlockData;
    private final Map<UUID, org.bukkit.entity.BlockDisplay> blockDisplays;
    private final Map<UUID, Boolean> hiddenState;
    private final Map<UUID, Long> sneakStartTimes;
    private final Map<UUID, org.bukkit.Location> lastLocations;
    private final Map<String, UUID> placedBlockKeys;
    private final Map<UUID, org.bukkit.entity.Entity> sittingEntities;
    private final Map<UUID, org.bukkit.entity.Entity> interactionEntities;
    private final Map<UUID, Integer> playerPoints;
    private String currentMapName;
    private final Map<UUID, Long> blockDamageOverrideUntil;
    private final Map<UUID, Boolean> glowingState;


    public DataController() {
        this.hiders = new ArrayList<>();
        this.seekers = new ArrayList<>();
        this.hiddenBlocks = new HashMap<>();
        this.chosenBlocks = new HashMap<>();
        this.chosenBlockData = new HashMap<>();
        this.blockDisplays = new HashMap<>();
        this.hiddenState = new HashMap<>();
        this.sneakStartTimes = new HashMap<>();
        this.lastLocations = new HashMap<>();
        this.placedBlockKeys = new HashMap<>();
        this.sittingEntities = new HashMap<>();
        this.interactionEntities = new HashMap<>();
        this.playerPoints = new HashMap<>();
        this.currentMapName = null;
        this.blockDamageOverrideUntil = new HashMap<>();
        this.glowingState = new HashMap<>();
    }

    public void setup() {
        if (instance == null) {
            instance = this;
        }
    }

    public static DataController getInstance() {
        if (instance == null) {
            instance = new DataController();
        }
        return instance;
    }

    public void changeHiddenBlock(UUID uuid, Block block) {
        hiddenBlocks.put(uuid, block);
    }

    public Block getHiddenBlock(UUID uuid) {
        return hiddenBlocks.get(uuid);
    }

    public Map<UUID, Block> getHiddenBlocks() {
        return hiddenBlocks;
    }

    public List<UUID> getSeekers() {
        return seekers;
    }

    public List<UUID> getHiders() {
        return hiders;
    }

    public void setSeekers(List<UUID> seekers) {
        this.seekers = seekers;
    }

    public void setHiders(List<UUID> hiders) {
        this.hiders = hiders;
    }

    public void removeHiddenBlock(UUID uuid) {
        hiddenBlocks.remove(uuid);
    }

    public void removeSeeker(UUID uuid) {
        seekers.remove(uuid);
    }

    public void removeHider(UUID uuid) {
        hiders.remove(uuid);
    }

    public void reset() {
        if (hiders != null) hiders.clear();
        if (seekers != null) seekers.clear();
        hiddenBlocks.clear();
        chosenBlocks.clear();
        chosenBlockData.clear();
        blockDisplays.clear();
        hiddenState.clear();
        sneakStartTimes.clear();
        lastLocations.clear();
        placedBlockKeys.clear();
        playerPoints.clear();
        blockDamageOverrideUntil.clear();
        glowingState.clear();
        for (org.bukkit.entity.Entity entity : sittingEntities.values()) {
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
        }
        sittingEntities.clear();
    }

    public void addHider(UUID uuid) {
        if (!hiders.contains(uuid)) {
            hiders.add(uuid);
        }
    }

    public void addSeeker(UUID uuid) {
        if (!seekers.contains(uuid)) {
            seekers.add(uuid);
        }
    }

    public void setChosenBlock(UUID uuid, org.bukkit.Material material) {
        if (material != null) {
            chosenBlocks.put(uuid, material);
        }
    }

    public org.bukkit.Material getChosenBlock(UUID uuid) {
        return chosenBlocks.get(uuid);
    }

    public Map<UUID, org.bukkit.Material> getChosenBlocks() {
        return chosenBlocks;
    }

    public void setChosenBlockData(UUID uuid, BlockData blockData) {
        if (blockData != null) {
            chosenBlockData.put(uuid, blockData);
        }
    }

    public BlockData getChosenBlockData(UUID uuid) {
        return chosenBlockData.get(uuid);
    }

    public Map<UUID, BlockData> getChosenBlockDatas() {
        return chosenBlockData;
    }

    public void setBlockDisplay(UUID uuid, org.bukkit.entity.BlockDisplay display) {
        if (display == null) {
            blockDisplays.remove(uuid);
            return;
        }
        blockDisplays.put(uuid, display);
    }

    public org.bukkit.entity.BlockDisplay getBlockDisplay(UUID uuid) {
        return blockDisplays.get(uuid);
    }

    public void setHidden(UUID uuid, boolean hidden) {
        hiddenState.put(uuid, hidden);
    }

    public boolean isHidden(UUID uuid) {
        return hiddenState.getOrDefault(uuid, false);
    }

    public void setSneakStart(UUID uuid, long startMs) {
        sneakStartTimes.put(uuid, startMs);
    }

    public Long getSneakStart(UUID uuid) {
        return sneakStartTimes.get(uuid);
    }

    public void clearSneakStart(UUID uuid) {
        sneakStartTimes.remove(uuid);
    }

    public void setLastLocation(UUID uuid, org.bukkit.Location location) {
        lastLocations.put(uuid, location);
    }

    public org.bukkit.Location getLastLocation(UUID uuid) {
        return lastLocations.get(uuid);
    }

    public void removePlacedBlockKey(org.bukkit.Location location) {
        placedBlockKeys.remove(blockKey(location));
    }

    public void addPlacedBlockKey(org.bukkit.Location location, UUID uuid) {
        placedBlockKeys.put(blockKey(location), uuid);
    }

    public UUID getHiderByBlock(org.bukkit.Location location) {
        return placedBlockKeys.get(blockKey(location));
    }

    private String blockKey(org.bukkit.Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    public void setSittingEntity(UUID uuid, org.bukkit.entity.Entity entity) {
        if (entity == null) {
            sittingEntities.remove(uuid);
            return;
        }
        sittingEntities.put(uuid, entity);
    }

    public org.bukkit.entity.Entity getSittingEntity(UUID uuid) {
        return sittingEntities.get(uuid);
    }

    public void setInteractionEntity(UUID uuid, org.bukkit.entity.Entity entity) {
        if (entity == null) {
            interactionEntities.remove(uuid);
            return;
        }
        interactionEntities.put(uuid, entity);
    }

    public org.bukkit.entity.Entity getInteractionEntity(UUID uuid) {
        return interactionEntities.get(uuid);
    }

    public void addPoints(UUID uuid, int points) {
        playerPoints.put(uuid, playerPoints.getOrDefault(uuid, 0) + points);
    }

    public int getPoints(UUID uuid) {
        return playerPoints.getOrDefault(uuid, 0);
    }

    public void setPoints(UUID uuid, int points) {
        playerPoints.put(uuid, points);
    }

    public Map<UUID, Integer> getAllPoints() {
        return new HashMap<>(playerPoints);
    }

    public void setCurrentMapName(String mapName) {
        this.currentMapName = mapName;
    }

    public String getCurrentMapName() {
        return this.currentMapName;
    }

    public void setBlockDamageOverride(UUID uuid, long untilEpochMs) {
        blockDamageOverrideUntil.put(uuid, untilEpochMs);
    }

    public boolean isBlockDamageOverrideActive(UUID uuid) {
        Long until = blockDamageOverrideUntil.get(uuid);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            blockDamageOverrideUntil.remove(uuid);
            return false;
        }
        return true;
    }

    public void setGlowing(UUID uuid, boolean glowing) {
        glowingState.put(uuid, glowing);
    }

    public boolean isGlowing(UUID uuid) {
        return glowingState.getOrDefault(uuid, false);
    }

    public void removeGlowing(UUID uuid) {
        glowingState.remove(uuid);
    }
}
