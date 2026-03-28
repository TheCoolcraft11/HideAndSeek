package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant;

import org.bukkit.Location;

import java.util.UUID;

public class AssistantSharedState {
    public final Location wanderOrigin;
    public final long spawnTimeMs;
    public UUID currentTargetId;
    public Location lastKnownPosition;
    public long hiddenSinceMs = -1L;
    public long lastAlertTick = -999L;
    public long lastShootTick = -999L;

    public AssistantSharedState(Location origin) {
        this.wanderOrigin = origin.clone();
        this.spawnTimeMs = System.currentTimeMillis();
    }

    public boolean isScentMemoryExpired(long timeoutMs) {
        if (hiddenSinceMs < 0L) {
            return false;
        }
        return (System.currentTimeMillis() - hiddenSinceMs) > timeoutMs;
    }
}
