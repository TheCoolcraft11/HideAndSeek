package de.thecoolcraft11.hideAndSeek.playerdata;

import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.storage.sql.framework.GlobalStatType;
import de.thecoolcraft11.minigameframework.storage.sql.stats.GlobalStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class DatabasePlayerDataStore implements PlayerDataStore {

    public static final String MINIGAME_ID = "hideandseek";

    private static final String WINS_KEY = "wins";
    private static final String LOSSES_KEY = "losses";

    private static final String SKINS_KEY = "skins";
    private static final String LOADOUT_KEY = "loadout";

    private final MinigameFramework framework;

    public DatabasePlayerDataStore(MinigameFramework framework) {
        this.framework = framework;
    }

    @Override
    public CompletableFuture<Long> getCoins(UUID uuid) {
        return GlobalStatsAPI.getStat(uuid, GlobalStatType.COINS).thenApply(v -> v == null ? 0L : Math.max(0L, v));
    }

    @Override
    public CompletableFuture<Void> setCoins(UUID uuid, long value) {
        return GlobalStatsAPI.setStat(uuid, GlobalStatType.COINS, Math.max(0L, value));
    }

    @Override
    public CompletableFuture<Long> getXp(UUID uuid) {
        return GlobalStatsAPI.getStat(uuid, GlobalStatType.XP).thenApply(v -> v == null ? 0L : Math.max(0L, v));
    }

    @Override
    public CompletableFuture<Void> setXp(UUID uuid, long value) {
        return GlobalStatsAPI.setStat(uuid, GlobalStatType.XP, Math.max(0L, value));
    }

    @Override
    public CompletableFuture<Long> getWins(UUID uuid) {
        return GlobalStatsAPI.getStat(uuid, GlobalStatType.WINS).thenApply(v -> v == null ? 0L : Math.max(0L, v));
    }

    @Override
    public CompletableFuture<Long> getLosses(UUID uuid) {
        return GlobalStatsAPI.getStat(uuid, GlobalStatType.LOSSES).thenApply(v -> v == null ? 0L : Math.max(0L, v));
    }

    @Override
    public CompletableFuture<Void> addWin(UUID uuid) {
        return GlobalStatsAPI.addStat(uuid, GlobalStatType.WINS, 1L)
                .thenCompose(ignored ->
                        MinigameStatsAPI.addStat(uuid, MINIGAME_ID, WINS_KEY, 1L))
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Void> addLoss(UUID uuid) {
        return GlobalStatsAPI.addStat(uuid, GlobalStatType.LOSSES, 1L)
                .thenCompose(ignored ->
                        MinigameStatsAPI.addStat(uuid, MINIGAME_ID, LOSSES_KEY, 1L))
                .thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<String> getSkins(UUID uuid) {
        return MinigameStatsAPI
                .getTypedStat(uuid, MINIGAME_ID, SKINS_KEY)
                .thenApply(stat -> {
                    if (stat == null || stat.value() == null || stat.value().isBlank()) {
                        return "{}";
                    }
                    return stat.value();
                })
                .exceptionally(ex -> {
                    framework.getLogger().warning(
                            "Failed to read skins from database for " + uuid + ": " + ex.getMessage()
                    );
                    return "{}";
                });
    }

    @Override
    public CompletableFuture<Void> setSkins(UUID uuid, String json) {
        return MinigameStatsAPI.setTypedStatString(uuid, MINIGAME_ID, SKINS_KEY, sanitizeJson(json));
    }

    @Override
    public CompletableFuture<String> getLoadout(UUID uuid) {
        return MinigameStatsAPI
                .getTypedStat(uuid, MINIGAME_ID, LOADOUT_KEY)
                .thenApply(stat -> {
                    if (stat == null || stat.value() == null || stat.value().isBlank()) {
                        return "{}";
                    }
                    return stat.value();
                })
                .exceptionally(ex -> {
                    framework.getLogger().warning(
                            "Failed to read loadout from database for " + uuid + ": " + ex.getMessage()
                    );
                    return "{}";
                });
    }

    @Override
    public CompletableFuture<Void> setLoadout(UUID uuid, String json) {
        return MinigameStatsAPI.setTypedStatString(uuid, MINIGAME_ID, LOADOUT_KEY, sanitizeJson(json));
    }

    @Override
    public CompletableFuture<Void> touchPlayer(UUID uuid, String name) {
        return GlobalStatsAPI.touchPlayer(uuid, name == null || name.isBlank() ? uuid.toString() : name);
    }

    private String sanitizeJson(String json) {
        return (json == null || json.isBlank()) ? "{}" : json;
    }
}





