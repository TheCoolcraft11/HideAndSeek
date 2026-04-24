package de.thecoolcraft11.hideAndSeek.playerdata;

import com.google.gson.Gson;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStatsService {

    private static final Gson GSON = new Gson();
    private static volatile PlayerStatsService active;

    private final HideAndSeek plugin;
    private final Map<UUID, PlayerStatsRecord> cache = new ConcurrentHashMap<>();

    private final Set<UUID> countedSeekerRounds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> countedHiderRounds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Integer> currentRoundSeekerKills = new ConcurrentHashMap<>();
    private final Map<UUID, Long> currentRoundHiderSurvivalMs = new ConcurrentHashMap<>();

    private volatile boolean roundActive;
    private BukkitTask roundTickerTask;

    public PlayerStatsService(HideAndSeek plugin) {
        this.plugin = plugin;
        active = this;
    }

    public static PlayerStatsService getActive() {
        return active;
    }

    public void loadPlayer(UUID playerId, String playerName) {
        cache.computeIfAbsent(playerId, ignored -> new PlayerStatsRecord());
        plugin.getPlayerDataStore().getWins(playerId);
        plugin.getPlayerDataStore().getLosses(playerId);
        plugin.getPlayerDataStore().getStats(playerId)
                .thenAccept(json -> {
                    PlayerStatsRecord loaded = PlayerStatsRecord.fromJson(json);
                    PlayerStatsRecord record = cache.computeIfAbsent(playerId, ignored -> new PlayerStatsRecord());
                    synchronized (record) {
                        record.mergeFrom(loaded);
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning(
                            "Failed to load tracked stats for " + playerName + " (" + playerId + "): " + ex.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> savePlayer(UUID playerId) {
        PlayerStatsRecord record = cache.get(playerId);
        if (record == null) {
            return CompletableFuture.completedFuture(null);
        }

        String json;
        synchronized (record) {
            json = record.toJson();
        }
        return plugin.getPlayerDataStore().setStats(playerId, json)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to save tracked stats for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    public CompletableFuture<Void> savePlayers(Collection<UUID> playerIds) {
        Set<UUID> ids = new HashSet<>(playerIds);
        if (ids.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<?>[] futures = ids.stream()
                .map(this::savePlayer)
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    public void saveAllOnlinePlayers() {
        savePlayers(Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList()).join();
    }

    public void onRoundStarted(String mapName, String gameMode, Collection<UUID> hiders, Collection<UUID> seekers) {
        countedHiderRounds.clear();
        countedSeekerRounds.clear();
        currentRoundSeekerKills.clear();
        currentRoundHiderSurvivalMs.clear();

        Set<UUID> participants = new HashSet<>();
        participants.addAll(hiders);
        participants.addAll(seekers);

        for (UUID playerId : participants) {
            PlayerStatsRecord record = getRecord(playerId);
            synchronized (record) {
                record.recordMapPlayed(mapName);
                record.recordGameModePlayed(gameMode);
            }
        }

        for (UUID hiderId : hiders) {
            PlayerStatsRecord record = getRecord(hiderId);
            synchronized (record) {
                record.hiderRoundsPlayed++;
            }
            countedHiderRounds.add(hiderId);
        }

        for (UUID seekerId : seekers) {
            PlayerStatsRecord record = getRecord(seekerId);
            synchronized (record) {
                record.seekerRoundsPlayed++;
            }
            countedSeekerRounds.add(seekerId);
        }

        roundActive = true;
        startRoundTicker();
    }

    public void onRoundEnded(boolean hidersWin, Collection<UUID> survivingHiders) {
        roundActive = false;
        stopRoundTicker();

        Set<UUID> hiders = new HashSet<>(HideAndSeek.getDataController().getHiders());
        Set<UUID> seekers = new HashSet<>(HideAndSeek.getDataController().getSeekers());
        Set<UUID> participants = new HashSet<>(hiders);
        participants.addAll(seekers);

        for (UUID playerId : participants) {
            PlayerStatsRecord record = getRecord(playerId);
            synchronized (record) {
                int roundKills = currentRoundSeekerKills.getOrDefault(playerId, 0);
                if (roundKills > record.mostKillsInASeekerRound) {
                    record.mostKillsInASeekerRound = roundKills;
                }

                if (hidersWin) {
                    if (survivingHiders.contains(playerId) && hiders.contains(playerId)) {
                        record.hiderWins++;
                    }
                } else if (seekers.contains(playerId)) {
                    record.seekerWins++;
                }

                long roundSurvival = currentRoundHiderSurvivalMs.getOrDefault(playerId, 0L);
                if (roundSurvival > record.longestHiderSurvivalMs) {
                    record.longestHiderSurvivalMs = roundSurvival;
                }
            }
        }

        if (hidersWin && survivingHiders.size() == 1) {
            UUID lastHider = survivingHiders.iterator().next();
            PlayerStatsRecord record = getRecord(lastHider);
            synchronized (record) {
                record.totalRoundsAsLastHider++;
            }
        }

        currentRoundSeekerKills.clear();
        currentRoundHiderSurvivalMs.clear();
        countedHiderRounds.clear();
        countedSeekerRounds.clear();
    }

    public void recordItemEquipped(UUID playerId, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.recordItemEquipped(itemId);
        }
    }

    public void recordItemUsed(UUID playerId, String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.recordItemUsed(itemId);
        }
    }

    public void recordPerkUsed(UUID playerId, String perkId) {
        if (perkId == null || perkId.isBlank()) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.recordPerkUsed(perkId);
        }
    }

    public void recordSeekerKill(UUID playerId) {
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            currentRoundSeekerKills.merge(playerId, 1, Integer::sum);
            record.totalSeekerKills++;
        }
    }

    public void recordHiderDeath(UUID playerId) {
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.totalHiderDeaths++;
        }
    }

    public void recordDamageDealt(UUID playerId, double damage) {
        if (damage <= 0.0) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.totalDamageDealt += damage;
        }
    }

    public void recordTauntUsed(UUID playerId) {
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.totalTauntsUsed++;
        }
    }

    public void recordPointsEarned(UUID playerId, int points) {
        if (points <= 0) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.totalPointsEarned += points;
        }
    }

    public void recordCoinsEarned(UUID playerId, int coins) {
        if (coins <= 0) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.totalCoinsEarned += coins;
        }
    }

    public void recordSeekerRoundParticipation(UUID playerId) {
        if (!roundActive || !countedSeekerRounds.add(playerId)) {
            return;
        }
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            record.seekerRoundsPlayed++;
        }
    }

    public void recordBlockConversion(UUID playerId) {
        recordSeekerRoundParticipation(playerId);
    }

    public void tickRoundStats() {
        if (!roundActive) {
            return;
        }

        String phaseId = plugin.getStateManager().getCurrentPhaseId();
        if (!"hiding".equals(phaseId) && !"seeking".equals(phaseId)) {
            return;
        }

        GameModeEnum gameMode = plugin.getSettingRegistry().get("game.mode");
        boolean isBlockMode = gameMode == GameModeEnum.BLOCK;
        Set<UUID> hiders = new HashSet<>(HideAndSeek.getDataController().getHiders());
        Set<UUID> seekers = new HashSet<>(HideAndSeek.getDataController().getSeekers());
        Set<UUID> participants = new HashSet<>(hiders);
        participants.addAll(seekers);

        for (UUID playerId : participants) {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || !player.isOnline()) {
                continue;
            }

            PlayerStatsRecord record = getRecord(playerId);
            synchronized (record) {
                record.totalPlaytimeMs += 1000L;

                if (isBlockMode && hiders.contains(playerId)) {
                    if (HideAndSeek.getDataController().isHidden(playerId)) {
                        record.totalHiddenInBlockModeMs += 1000L;
                    } else {
                        record.totalUnhiddenInBlockModeMs += 1000L;
                    }
                }

                if ("seeking".equals(phaseId) && hiders.contains(playerId)) {
                    record.hiderSurvivalMs += 1000L;
                    currentRoundHiderSurvivalMs.merge(playerId, 1000L, Long::sum);
                }
            }
        }
    }

    public void startRoundTicker() {
        stopRoundTicker();
        roundTickerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRoundStats, 20L, 20L);
    }

    public void stopRoundTicker() {
        if (roundTickerTask != null) {
            roundTickerTask.cancel();
            roundTickerTask = null;
        }
    }

    public PlayerStatsRecord getSnapshot(UUID playerId) {
        PlayerStatsRecord record = getRecord(playerId);
        synchronized (record) {
            return record.copy();
        }
    }

    private PlayerStatsRecord getRecord(UUID playerId) {
        return cache.computeIfAbsent(playerId, ignored -> new PlayerStatsRecord());
    }

    public static final class PlayerStatsRecord {
        public long totalPointsEarned;
        public long totalCoinsEarned;
        public long totalTauntsUsed;
        public long totalRoundsAsLastHider;
        public long totalPlaytimeMs;
        public long totalHiderDeaths;
        public double totalDamageDealt;
        public long hiderWins;
        public long seekerWins;
        public long seekerRoundsPlayed;
        public long hiderRoundsPlayed;
        public long totalSeekerKills;
        public long mostKillsInASeekerRound;
        public long hiderSurvivalMs;
        public long longestHiderSurvivalMs;
        public long totalHiddenInBlockModeMs;
        public long totalUnhiddenInBlockModeMs;
        public Map<String, ItemUsage> items = new HashMap<>();
        public Map<String, Long> mapsPlayed = new HashMap<>();
        public Map<String, Long> gameModesPlayed = new HashMap<>();
        private int version = 1;
        public Map<String, Long> perksUsed = new HashMap<>();

        private static PlayerStatsRecord fromJson(String json) {
            PlayerStatsRecord record;
            try {
                record = (json == null || json.isBlank()) ? new PlayerStatsRecord() : GSON.fromJson(json,
                        PlayerStatsRecord.class);
            } catch (Exception ignored) {
                record = new PlayerStatsRecord();
            }
            if (record == null) {
                record = new PlayerStatsRecord();
            }
            record.normalize();
            return record;
        }

        private static void mergeLongMap(Map<String, Long> target, Map<String, Long> source) {
            if (source == null) {
                return;
            }
            for (Map.Entry<String, Long> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                target.merge(entry.getKey(), Math.max(0L, entry.getValue() == null ? 0L : entry.getValue()), Long::sum);
            }
        }

        private static void mergeUsageMap(Map<String, ItemUsage> target, Map<String, ItemUsage> source) {
            if (source == null) {
                return;
            }
            for (Map.Entry<String, ItemUsage> entry : source.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                ItemUsage usage = entry.getValue() == null ? new ItemUsage() : entry.getValue();
                target.merge(entry.getKey(), usage.copy(), ItemUsage::merge);
            }
        }

        private void normalize() {
            if (items == null) items = new HashMap<>();
            if (mapsPlayed == null) mapsPlayed = new HashMap<>();
            if (perksUsed == null) perksUsed = new HashMap<>();
            if (gameModesPlayed == null) gameModesPlayed = new HashMap<>();
        }

        private void mergeFrom(PlayerStatsRecord other) {
            if (other == null) {
                return;
            }
            version = Math.max(version, other.version);
            totalPointsEarned += other.totalPointsEarned;
            totalCoinsEarned += other.totalCoinsEarned;
            totalTauntsUsed += other.totalTauntsUsed;
            totalRoundsAsLastHider += other.totalRoundsAsLastHider;
            totalPlaytimeMs += other.totalPlaytimeMs;
            totalHiderDeaths += other.totalHiderDeaths;
            totalDamageDealt += other.totalDamageDealt;
            hiderWins += other.hiderWins;
            seekerWins += other.seekerWins;
            seekerRoundsPlayed += other.seekerRoundsPlayed;
            hiderRoundsPlayed += other.hiderRoundsPlayed;
            totalSeekerKills += other.totalSeekerKills;
            mostKillsInASeekerRound = Math.max(mostKillsInASeekerRound, other.mostKillsInASeekerRound);
            hiderSurvivalMs += other.hiderSurvivalMs;
            longestHiderSurvivalMs = Math.max(longestHiderSurvivalMs, other.longestHiderSurvivalMs);
            totalHiddenInBlockModeMs += other.totalHiddenInBlockModeMs;
            totalUnhiddenInBlockModeMs += other.totalUnhiddenInBlockModeMs;
            mergeUsageMap(items, other.items);
            mergeLongMap(mapsPlayed, other.mapsPlayed);
            mergeLongMap(perksUsed, other.perksUsed);
            mergeLongMap(gameModesPlayed, other.gameModesPlayed);
        }

        private void recordItemEquipped(String itemId) {
            items.computeIfAbsent(itemId, ignored -> new ItemUsage()).equipped++;
        }

        private void recordItemUsed(String itemId) {
            items.computeIfAbsent(itemId, ignored -> new ItemUsage()).used++;
        }

        private void recordPerkUsed(String perkId) {
            perksUsed.merge(perkId, 1L, Long::sum);
        }

        private void recordMapPlayed(String mapName) {
            if (mapName == null || mapName.isBlank()) {
                mapName = "unknown";
            }
            mapsPlayed.merge(mapName, 1L, Long::sum);
        }

        private void recordGameModePlayed(String gameMode) {
            if (gameMode == null || gameMode.isBlank()) {
                gameMode = "unknown";
            }
            gameModesPlayed.merge(gameMode, 1L, Long::sum);
        }

        private String toJson() {
            return GSON.toJson(this);
        }

        private PlayerStatsRecord copy() {
            PlayerStatsRecord copy = new PlayerStatsRecord();
            copy.version = version;
            copy.totalPointsEarned = totalPointsEarned;
            copy.totalCoinsEarned = totalCoinsEarned;
            copy.totalTauntsUsed = totalTauntsUsed;
            copy.totalRoundsAsLastHider = totalRoundsAsLastHider;
            copy.totalPlaytimeMs = totalPlaytimeMs;
            copy.totalHiderDeaths = totalHiderDeaths;
            copy.totalDamageDealt = totalDamageDealt;
            copy.hiderWins = hiderWins;
            copy.seekerWins = seekerWins;
            copy.seekerRoundsPlayed = seekerRoundsPlayed;
            copy.hiderRoundsPlayed = hiderRoundsPlayed;
            copy.totalSeekerKills = totalSeekerKills;
            copy.mostKillsInASeekerRound = mostKillsInASeekerRound;
            copy.hiderSurvivalMs = hiderSurvivalMs;
            copy.longestHiderSurvivalMs = longestHiderSurvivalMs;
            copy.totalHiddenInBlockModeMs = totalHiddenInBlockModeMs;
            copy.totalUnhiddenInBlockModeMs = totalUnhiddenInBlockModeMs;
            mergeUsageMap(copy.items, items);
            mergeLongMap(copy.mapsPlayed, mapsPlayed);
            mergeLongMap(copy.perksUsed, perksUsed);
            mergeLongMap(copy.gameModesPlayed, gameModesPlayed);
            return copy;
        }
    }

    public static final class ItemUsage {
        public long equipped;
        public long used;

        private ItemUsage merge(ItemUsage other) {
            if (other == null) {
                return this;
            }
            equipped += other.equipped;
            used += other.used;
            return this;
        }

        private ItemUsage copy() {
            ItemUsage copy = new ItemUsage();
            copy.equipped = equipped;
            copy.used = used;
            return copy;
        }
    }
}





