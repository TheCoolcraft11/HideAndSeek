package de.thecoolcraft11.hideAndSeek.playerdata;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public interface PlayerDataStore {

    CompletableFuture<Long> getCoins(UUID uuid);

    CompletableFuture<Void> setCoins(UUID uuid, long value);

    CompletableFuture<Long> getXp(UUID uuid);

    CompletableFuture<Void> setXp(UUID uuid, long value);

    default CompletableFuture<Long> getWins(UUID uuid) {
        return CompletableFuture.completedFuture(0L);
    }

    default CompletableFuture<Long> getLosses(UUID uuid) {
        return CompletableFuture.completedFuture(0L);
    }

    CompletableFuture<Void> addWin(UUID uuid);

    CompletableFuture<Void> addLoss(UUID uuid);

    CompletableFuture<String> getSkins(UUID uuid);

    CompletableFuture<Void> setSkins(UUID uuid, String json);

    CompletableFuture<String> getLoadout(UUID uuid);

    CompletableFuture<Void> setLoadout(UUID uuid, String json);

    CompletableFuture<String> getStats(UUID uuid);

    CompletableFuture<Void> setStats(UUID uuid, String json);

    default CompletableFuture<Void> touchPlayer(UUID uuid, String name) {
        return CompletableFuture.completedFuture(null);
    }
}

