package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class UnstuckManager {
    private static final long RECORD_INTERVAL_TICKS = 10L;
    private static final double SAME_SNAPSHOT_DISTANCE_SQUARED = 0.01;
    private static final double MIN_TELEPORT_DISTANCE_SQUARED = 0.25;
    private static final int RECENT_DESTINATION_LIMIT = 6;
    private static final long ATTEMPT_RESET_WINDOW_MS = 120_000L;

    private final HideAndSeek plugin;
    private final DataController dataController;

    private final Map<UUID, LinkedList<Location>> historyByPlayer = new HashMap<>();
    private final Map<UUID, Long> cooldownUntilMs = new HashMap<>();
    private final Map<UUID, Integer> consecutiveAttemptsByPlayer = new HashMap<>();
    private final Map<UUID, Long> lastAttemptAtMs = new HashMap<>();
    private final Map<UUID, LinkedList<String>> recentDestinationKeysByPlayer = new HashMap<>();

    private int trackingTaskId = -1;

    public UnstuckManager(HideAndSeek plugin) {
        this.plugin = plugin;
        this.dataController = HideAndSeek.getDataController();
    }

    public void startTrackingTask() {
        stopTrackingTask();
        trackingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::recordAllPlayersInSeeking, 0L, RECORD_INTERVAL_TICKS);
    }

    public void stopTrackingTask() {
        if (trackingTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(trackingTaskId);
            trackingTaskId = -1;
        }
    }

    public void shutdown() {
        stopTrackingTask();
        clearAll();
    }

    public void clearAll() {
        historyByPlayer.clear();
        cooldownUntilMs.clear();
        consecutiveAttemptsByPlayer.clear();
        lastAttemptAtMs.clear();
        recentDestinationKeysByPlayer.clear();
    }

    public void clearPlayerData(UUID playerId) {
        historyByPlayer.remove(playerId);
        cooldownUntilMs.remove(playerId);
        consecutiveAttemptsByPlayer.remove(playerId);
        lastAttemptAtMs.remove(playerId);
        recentDestinationKeysByPlayer.remove(playerId);
    }

    public long getRemainingCooldownMs(UUID playerId) {
        Long until = cooldownUntilMs.get(playerId);
        if (until == null) {
            return 0L;
        }
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            cooldownUntilMs.remove(playerId);
            return 0L;
        }
        return remaining;
    }

    public boolean hasNearbyOpponents(Player player, double range) {
        if (isSeeker(player.getUniqueId())) {
            return false;
        }
        double rangeSquared = range * range;
        Location source = player.getLocation();
        List<UUID> opponents = getOpponentIds(player.getUniqueId());

        for (UUID opponentId : opponents) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent == null || !opponent.isOnline()) {
                continue;
            }
            if (!opponent.getWorld().equals(source.getWorld())) {
                continue;
            }
            if (opponent.getLocation().distanceSquared(source) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNearbyOpponentsAt(Player player, @Nullable Location location, double range) {
        if (isSeeker(player.getUniqueId())) {
            return false;
        }
        if (location == null || location.getWorld() == null) {
            return false;
        }
        double rangeSquared = range * range;
        List<UUID> opponents = getOpponentIds(player.getUniqueId());
        for (UUID opponentId : opponents) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent == null || !opponent.isOnline()) {
                continue;
            }
            if (!opponent.getWorld().equals(location.getWorld())) {
                continue;
            }
            if (opponent.getLocation().distanceSquared(location) <= rangeSquared) {
                return true;
            }
        }
        return false;
    }

    public void recordPosition(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        Location location = player.getLocation().clone();
        if (!isOnSolidGround(location) && !isInLiquid(location)) {
            return;
        }

        LinkedList<Location> history = historyByPlayer.computeIfAbsent(player.getUniqueId(), k -> new LinkedList<>());
        if (!history.isEmpty()) {
            Location last = history.getLast();
            if (sameWorld(last, location) && last.distanceSquared(location) < SAME_SNAPSHOT_DISTANCE_SQUARED) {
                return;
            }
        }

        history.addLast(location);
        while (history.size() > getMaxHistoryEntries()) {
            history.removeFirst();
        }
    }

    public boolean isPlayerStationary(Player player) {
        LinkedList<Location> history = historyByPlayer.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            return true;
        }

        int snapshotsToCheck = Math.max(2, getStationarySnapshotCount());
        if (history.size() < snapshotsToCheck) {
            return false;
        }

        List<Location> recent = history.subList(history.size() - snapshotsToCheck, history.size());
        Location first = recent.getFirst();
        double radius = getDoubleSetting("game.unstuck.stationary-radius", 0.75);

        for (Location location : recent) {
            if (!sameWorld(first, location)) {
                return false;
            }
            if (horizontalDistance(location, first) > radius) {
                return false;
            }
            if (Math.abs(location.getY() - first.getY()) > radius) {
                return false;
            }
        }
        return true;
    }

    public int getDefaultCooldownSeconds() {
        return getIntSetting("game.unstuck.cooldown", 30);
    }

    public int getSpawnFallbackCooldownSeconds() {
        return getIntSetting("game.unstuck.spawn-cooldown", 90);
    }

    public int getForceSpawnAfterAttempts() {
        return Math.max(1, getIntSetting("game.unstuck.force-spawn-after-attempts", 3));
    }

    public void applyCooldown(UUID playerId, int seconds) {
        long durationMs = Math.max(0L, seconds) * 1000L;
        cooldownUntilMs.put(playerId, System.currentTimeMillis() + durationMs);
    }

    public void recordSuccessfulUnstuck(UUID playerId, Location location, UnstuckMethod method) {
        if (method == UnstuckMethod.SPAWN) {
            consecutiveAttemptsByPlayer.put(playerId, 0);
        } else {
            int previous = getConsecutiveAttempts(playerId);
            consecutiveAttemptsByPlayer.put(playerId, previous + 1);
        }
        lastAttemptAtMs.put(playerId, System.currentTimeMillis());

        LinkedList<String> recent = recentDestinationKeysByPlayer.computeIfAbsent(playerId, ignored -> new LinkedList<>());
        recent.addLast(blockKey(location));
        while (recent.size() > RECENT_DESTINATION_LIMIT) {
            recent.removeFirst();
        }
    }

    public @Nullable Location resolveWorldSpawnTarget(Player player) {
        Location spawn = player.getWorld().getSpawnLocation();
        Location safeSpawn = sanitizeSafeLocation(player, spawn);
        if (safeSpawn != null) {
            return safeSpawn;
        }

        int spawnSearchRadius = Math.max(1, getIntSetting("game.unstuck.spawn-search-radius", 4));
        return findNearbySafeGround(player, spawn, spawnSearchRadius, player.getLocation());
    }

    public UnstuckResult tryFindSafePosition(Player player, @Nullable Location spawn) {
        long remaining = getRemainingCooldownMs(player.getUniqueId());
        if (remaining > 0L) {
            long secondsLeft = (long) Math.ceil(remaining / 1000.0);
            return UnstuckResult.fail("Unstuck is on cooldown for " + secondsLeft + "s.");
        }

        Location current = player.getLocation();

        if (getConsecutiveAttempts(player.getUniqueId()) >= getForceSpawnAfterAttempts()) {
            Location forcedSpawn = findSpawnFallbackLocation(player, spawn, current);
            if (forcedSpawn != null) {
                return UnstuckResult.success(
                        forcedSpawn,
                        UnstuckMethod.SPAWN,
                        getSpawnFallbackCooldownSeconds(),
                        "Too many consecutive unstuck attempts. Sending you to spawn."
                );
            }
            return UnstuckResult.fail("Forced spawn fallback is not safe right now.");
        }

        Location fromHistory = findHistoryRollback(player, current);
        if (fromHistory != null) {
            return UnstuckResult.success(fromHistory, UnstuckMethod.HISTORY, getDefaultCooldownSeconds(), "Teleported to your last safe position.");
        }

        int nearbyScanRadius = Math.max(1, getIntSetting("game.unstuck.scan-radius", 3));
        Location nearby = findNearbySafeGround(player, current, nearbyScanRadius, current);
        if (nearby != null) {
            return UnstuckResult.success(nearby, UnstuckMethod.NEARBY, getDefaultCooldownSeconds(), "Teleported to a nearby safe block.");
        }

        if (!isPlayerStationary(player)) {
            return UnstuckResult.fail("No safe position found yet. Stay still for a few seconds and retry.");
        }

        double seekerRange = getDoubleSetting("game.unstuck.seeker-range", 15.0);
        if (hasNearbyOpponentsAt(player, spawn, seekerRange)) {
            return UnstuckResult.fail("Spawn is not safe right now.");
        }

        Location safeSpawn = findSpawnFallbackLocation(player, spawn, current);
        if (safeSpawn == null) {
            return UnstuckResult.fail("No safe unstuck location was found.");
        }

        return UnstuckResult.success(safeSpawn, UnstuckMethod.SPAWN, getSpawnFallbackCooldownSeconds(), "Teleported to spawn as a last resort.");
    }

    private void recordAllPlayersInSeeking() {
        String phase = plugin.getStateManager().getCurrentPhaseId();
        if (!"seeking".equals(phase)) {
            return;
        }

        Set<UUID> activePlayers = Set.copyOf(getActivePlayerIds());

        historyByPlayer.keySet().removeIf(uuid -> !activePlayers.contains(uuid));
        cooldownUntilMs.keySet().removeIf(uuid -> !activePlayers.contains(uuid));

        for (UUID playerId : activePlayers) {
            Player tracked = Bukkit.getPlayer(playerId);
            if (tracked != null && tracked.isOnline()) {
                recordPosition(tracked);
            }
        }
    }

    private @Nullable Location findHistoryRollback(Player player, Location current) {
        LinkedList<Location> history = historyByPlayer.get(player.getUniqueId());
        if (history == null || history.isEmpty()) {
            return null;
        }

        double maxUpwardGain = getDoubleSetting("game.unstuck.max-upward-gain", 1.0);
        double maxHorizontalRollback = getDoubleSetting("game.unstuck.max-horizontal-rollback", 4.0);

        List<ScoredLocation> candidates = new ArrayList<>();
        for (int index = history.size() - 1; index >= 0; index--) {
            Location snapshot = history.get(index);
            Location safeLocation = sanitizeSafeLocation(player, snapshot);
            if (safeLocation == null) {
                continue;
            }
            if (!sameWorld(current, safeLocation)) {
                continue;
            }
            if (!isMeaningfullyDifferent(current, safeLocation)) {
                continue;
            }
            if (isRecentlyUsedDestination(player.getUniqueId(), safeLocation)) {
                continue;
            }

            double heightGain = safeLocation.getY() - current.getY();
            double horizontalDistance = horizontalDistance(safeLocation, current);
            if (heightGain > maxUpwardGain && horizontalDistance > maxHorizontalRollback) {
                continue;
            }

            int recencyBonus = Math.max(0, history.size() - index);
            double score = scoreLocation(safeLocation, current) + recencyBonus;
            candidates.add(new ScoredLocation(safeLocation, score));
        }

        return selectBestCandidate(candidates, current);
    }

    private @Nullable Location findNearbySafeGround(Player player, @Nullable Location origin, int radius, @Nullable Location avoidLocation) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }

        int effectiveRadius = Math.max(1, radius);

        List<Integer> yOffsets = List.of(-5, -4, -3, -2, -1, 0, 1, 2);
        List<int[]> horizontalOffsets = createHorizontalOffsets(effectiveRadius);

        World world = origin.getWorld();
        int baseX = origin.getBlockX();
        int baseY = origin.getBlockY();
        int baseZ = origin.getBlockZ();

        List<ScoredLocation> candidates = new ArrayList<>();
        for (int yOffset : yOffsets) {
            int y = baseY + yOffset;
            for (int[] offset : horizontalOffsets) {
                int x = baseX + offset[0];
                int z = baseZ + offset[1];
                Location candidate = new Location(world, x + 0.5, y, z + 0.5, origin.getYaw(), origin.getPitch());
                Location safe = sanitizeSafeLocation(player, candidate);
                if (safe != null && isMeaningfullyDifferent(avoidLocation, safe) && !isRecentlyUsedDestination(player.getUniqueId(), safe)) {
                    double depthPenalty = Math.abs(yOffset) * 0.6;
                    double score = scoreLocation(safe, avoidLocation) - depthPenalty;
                    candidates.add(new ScoredLocation(safe, score));
                }
            }
        }

        return selectBestCandidate(candidates, avoidLocation);
    }

    private @Nullable Location findSpawnFallbackLocation(Player player, @Nullable Location spawn, Location current) {
        Location spawnBase;
        if (spawn != null && spawn.getWorld() != null) {
            spawnBase = spawn.clone();
        } else {
            spawnBase = player.getWorld().getSpawnLocation();
        }

        double seekerRange = getDoubleSetting("game.unstuck.seeker-range", 15.0);
        if (hasNearbyOpponentsAt(player, spawnBase, seekerRange)) {
            return null;
        }

        Location direct = sanitizeSafeLocation(player, spawnBase);
        if (direct != null && isMeaningfullyDifferent(current, direct) && !isRecentlyUsedDestination(player.getUniqueId(), direct)) {
            return direct;
        }

        int spawnSearchRadius = Math.max(1, getIntSetting("game.unstuck.spawn-search-radius", 4));
        return findNearbySafeGround(player, spawnBase, spawnSearchRadius, current);
    }

    private @Nullable Location sanitizeSafeLocation(Player player, Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) {
            return null;
        }

        int x = baseLocation.getBlockX();
        int y = baseLocation.getBlockY();
        int z = baseLocation.getBlockZ();

        if (!isSafeStandingSpot(world, x, y, z, player)) {
            return null;
        }

        return new Location(world, x + 0.5, y, z + 0.5, baseLocation.getYaw(), baseLocation.getPitch());
    }

    private boolean isSafeStandingSpot(World world, int x, int y, int z, Player player) {
        Block below = world.getBlockAt(x, y - 1, z);
        if (!isSolidFloor(below)) {
            return false;
        }

        int clearanceBlocks = getClearanceBlocks(player);
        for (int i = 0; i < clearanceBlocks; i++) {
            Block check = world.getBlockAt(x, y + i, z);
            if (!check.isPassable() || check.isLiquid()) {
                return false;
            }
        }

        return true;
    }

    private boolean isSolidFloor(Block block) {
        return block.getType().isSolid() && !block.isPassable() && !block.isLiquid();
    }

    private int getClearanceBlocks(Player player) {
        return player.getHeight() <= 1.2 ? 1 : 2;
    }

    private boolean isOnSolidGround(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        Block below = world.getBlockAt(location.getBlockX(), location.getBlockY() - 1, location.getBlockZ());
        return isSolidFloor(below);
    }

    private boolean isInLiquid(Location location) {
        World world = location.getWorld();
        if (world == null) {
            return false;
        }

        Block feet = world.getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        Block head = world.getBlockAt(location.getBlockX(), location.getBlockY() + 1, location.getBlockZ());
        return feet.isLiquid() || head.isLiquid();
    }

    private int getMaxHistoryEntries() {
        int seconds = Math.max(6, getIntSetting("game.unstuck.history-seconds", 12));
        return seconds * 2;
    }

    private int getStationarySnapshotCount() {
        int seconds = Math.max(2, getIntSetting("game.unstuck.stationary-seconds", 4));
        return seconds * 2;
    }

    private List<int[]> createHorizontalOffsets(int radius) {
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                offsets.add(new int[]{dx, dz});
            }
        }

        offsets.sort(Comparator
                .comparingInt((int[] a) -> Math.abs(a[0]) + Math.abs(a[1]))
                .thenComparingInt(a -> a[0] * a[0] + a[1] * a[1]));

        return Collections.unmodifiableList(offsets);
    }

    private int getIntSetting(String key, int fallback) {
        Object value = plugin.getSettingRegistry().get(key, fallback);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (key.startsWith("game.unstuck.")) {
            String legacyKey = key.replace("game.unstuck.", "hider-items.unstuck.");
            Object legacyValue = plugin.getSettingRegistry().get(legacyKey, fallback);
            if (legacyValue instanceof Number number) {
                return number.intValue();
            }
        }
        return fallback;
    }

    private double getDoubleSetting(String key, double fallback) {
        Object value = plugin.getSettingRegistry().get(key, fallback);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (key.startsWith("game.unstuck.")) {
            String legacyKey = key.replace("game.unstuck.", "hider-items.unstuck.");
            Object legacyValue = plugin.getSettingRegistry().get(legacyKey, fallback);
            if (legacyValue instanceof Number number) {
                return number.doubleValue();
            }
        }
        return fallback;
    }

    private boolean sameWorld(Location first, Location second) {
        return first.getWorld() != null && first.getWorld().equals(second.getWorld());
    }

    private double scoreLocation(Location candidate, @Nullable Location reference) {
        Block feet = candidate.getWorld().getBlockAt(candidate.getBlockX(), candidate.getBlockY(), candidate.getBlockZ());
        int skyLight = feet.getLightFromSky();
        boolean canSeeSky = candidate.getWorld().getHighestBlockYAt(candidate.getBlockX(), candidate.getBlockZ()) <= candidate.getBlockY();

        int openness = 0;
        int[][] offsets = new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] offset : offsets) {
            Block sideFeet = candidate.getWorld().getBlockAt(candidate.getBlockX() + offset[0], candidate.getBlockY(), candidate.getBlockZ() + offset[1]);
            Block sideHead = candidate.getWorld().getBlockAt(candidate.getBlockX() + offset[0], candidate.getBlockY() + 1, candidate.getBlockZ() + offset[1]);
            if (sideFeet.isPassable() && !sideFeet.isLiquid() && sideHead.isPassable() && !sideHead.isLiquid()) {
                openness++;
            }
        }

        double score = 0.0;
        score += canSeeSky ? 20.0 : 0.0;
        score += skyLight * 2.0;
        score += openness * 4.0;

        if (reference != null && sameWorld(reference, candidate)) {
            double yGain = candidate.getY() - reference.getY();
            score += yGain * 3.0;
            score -= horizontalDistance(candidate, reference) * 0.4;
        }

        return score;
    }

    private @Nullable Location selectBestCandidate(List<ScoredLocation> candidates, @Nullable Location reference) {
        if (candidates.isEmpty()) {
            return null;
        }
        candidates.sort((a, b) -> {
            int scoreCompare = Double.compare(b.score, a.score);
            if (scoreCompare != 0) {
                return scoreCompare;
            }
            double aDistance = reference == null ? 0.0 : horizontalDistance(a.location, reference);
            double bDistance = reference == null ? 0.0 : horizontalDistance(b.location, reference);
            return Double.compare(aDistance, bDistance);
        });
        return candidates.getFirst().location;
    }

    private boolean isRecentlyUsedDestination(UUID playerId, Location location) {
        LinkedList<String> recent = recentDestinationKeysByPlayer.get(playerId);
        if (recent == null || recent.isEmpty()) {
            return false;
        }
        return recent.contains(blockKey(location));
    }

    private int getConsecutiveAttempts(UUID playerId) {
        long now = System.currentTimeMillis();
        Long last = lastAttemptAtMs.get(playerId);
        if (last == null || (now - last) > ATTEMPT_RESET_WINDOW_MS) {
            consecutiveAttemptsByPlayer.put(playerId, 0);
            return 0;
        }
        return consecutiveAttemptsByPlayer.getOrDefault(playerId, 0);
    }

    private String blockKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private boolean isMeaningfullyDifferent(@Nullable Location first, Location second) {
        if (first == null) {
            return true;
        }
        if (!sameWorld(first, second)) {
            return true;
        }
        if (first.getBlockX() == second.getBlockX()
                && first.getBlockY() == second.getBlockY()
                && first.getBlockZ() == second.getBlockZ()) {
            return false;
        }
        return first.distanceSquared(second) >= MIN_TELEPORT_DISTANCE_SQUARED;
    }

    private List<UUID> getActivePlayerIds() {
        List<UUID> active = new ArrayList<>(dataController.getHiders());
        active.addAll(dataController.getSeekers());
        return active;
    }

    private List<UUID> getOpponentIds(UUID playerId) {
        if (dataController.getHiders().contains(playerId)) {
            return dataController.getSeekers();
        }
        if (dataController.getSeekers().contains(playerId)) {
            return dataController.getHiders();
        }
        return List.of();
    }

    private boolean isSeeker(UUID playerId) {
        return dataController.getSeekers().contains(playerId);
    }

    private double horizontalDistance(Location first, Location second) {
        if (!sameWorld(first, second)) {
            return Double.MAX_VALUE;
        }
        double dx = first.getX() - second.getX();
        double dz = first.getZ() - second.getZ();
        return Math.sqrt((dx * dx) + (dz * dz));
    }

    public enum UnstuckMethod {
        HISTORY,
        NEARBY,
        SPAWN
    }

    public record UnstuckResult(boolean success, @Nullable Location location, @Nullable UnstuckMethod method,
                                int cooldownSeconds, @Nullable String message) {
        public static UnstuckResult fail(String message) {
            return new UnstuckResult(false, null, null, 0, message);
        }

        public static UnstuckResult success(Location location, UnstuckMethod method, int cooldownSeconds, String message) {
            return new UnstuckResult(true, location, method, cooldownSeconds, message);
        }
    }

    private record ScoredLocation(Location location, double score) {
    }
}





