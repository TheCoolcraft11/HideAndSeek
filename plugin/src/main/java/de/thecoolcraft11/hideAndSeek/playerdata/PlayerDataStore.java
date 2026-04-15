package de.thecoolcraft11.hideAndSeek.playerdata;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerDataStore {

    CompletableFuture<Long> getCoins(UUID uuid);

    CompletableFuture<Void> setCoins(UUID uuid, long value);

    CompletableFuture<Long> getXp(UUID uuid);

    CompletableFuture<Void> setXp(UUID uuid, long value);

    CompletableFuture<Long> getWins(UUID uuid);

    CompletableFuture<Long> getLosses(UUID uuid);

    CompletableFuture<Void> addWin(UUID uuid);

    CompletableFuture<Void> addLoss(UUID uuid);

    CompletableFuture<String> getSkins(UUID uuid);

    CompletableFuture<Void> setSkins(UUID uuid, String json);

    CompletableFuture<String> getLoadout(UUID uuid);

    CompletableFuture<Void> setLoadout(UUID uuid, String json);

    default CompletableFuture<Void> touchPlayer(UUID uuid, String name) {
        return CompletableFuture.completedFuture(null);
    }
}

