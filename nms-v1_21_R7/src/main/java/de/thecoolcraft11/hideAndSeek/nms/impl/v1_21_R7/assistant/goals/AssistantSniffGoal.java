package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantBridge;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantSharedState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.EnumSet;
import java.util.UUID;

public class AssistantSniffGoal extends Goal {

    private final PathfinderMob nmsZombie;
    private final Plugin plugin;
    private final AssistantSharedState state;
    private int ticksUntilScan = 0;

    public AssistantSniffGoal(PathfinderMob nmsZombie, Plugin plugin, AssistantSharedState state) {
        this.nmsZombie = nmsZombie;
        this.plugin = plugin;
        this.state = state;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return true;
    }

    @Override
    public void tick() {
        int interval = Math.max(1, AssistantBridge.getIntSetting(plugin, "seeker-items.assistant.sniff-interval", 15));
        if (--ticksUntilScan > 0) {
            checkScentMemory();
            spawnTrackingParticles();
            return;
        }

        ticksUntilScan = interval;
        scanNow();
        spawnTrackingParticles();
    }

    private void scanNow() {
        if (AssistantBridge.getHiders().isEmpty()) {
            state.currentTargetId = null;
            state.lastKnownPosition = null;
            state.hiddenSinceMs = -1L;
            return;
        }

        double frontRange = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.sniff-range-front", 35.0);
        double rearRange = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.sniff-range-rear", 15.0);
        double hiddenMultiplier = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.sniff-hidden-multiplier", 0.5);

        org.bukkit.entity.Mob bukkitMob = (org.bukkit.entity.Mob) nmsZombie.getBukkitEntity();
        Location mobLoc = bukkitMob.getLocation();
        Vector facing = mobLoc.getDirection().normalize();

        Player bestTarget = null;
        double bestDist = Double.MAX_VALUE;

        for (UUID hiderId : AssistantBridge.getHiders()) {
            Player hider = AssistantBridge.getOnlinePlayer(hiderId);
            if (hider == null || !hider.isOnline()) continue;
            if (hider.getGameMode() == GameMode.SPECTATOR) continue;
            if (!hider.getWorld().equals(bukkitMob.getWorld())) continue;

            boolean hidden = AssistantBridge.isHidden(hiderId);
            double dist = hider.getLocation().distance(mobLoc);
            Vector toHider = hider.getLocation().toVector().subtract(mobLoc.toVector()).normalize();
            double dot = facing.dot(toHider);

            double range = dot > 0 ? frontRange : rearRange;
            if (hidden) {
                range *= hiddenMultiplier;
            }

            if (dist > range) continue;
            if (dist < bestDist) {
                bestDist = dist;
                bestTarget = hider;
            }
        }

        if (bestTarget != null) {
            state.currentTargetId = bestTarget.getUniqueId();
            state.lastKnownPosition = bestTarget.getLocation();

            if (AssistantBridge.isHidden(bestTarget.getUniqueId())) {
                if (state.hiddenSinceMs < 0L) {
                    state.hiddenSinceMs = System.currentTimeMillis();
                }
            } else {
                state.hiddenSinceMs = -1L;
            }
            return;
        }


        if (state.currentTargetId == null) {
            return;
        }

        Player tracked = AssistantBridge.getOnlinePlayer(state.currentTargetId);
        if (tracked == null || !tracked.isOnline() || !AssistantBridge.getHiders().contains(state.currentTargetId)) {
            state.currentTargetId = null;
            state.lastKnownPosition = null;
            state.hiddenSinceMs = -1L;
            return;
        }

        if (!AssistantBridge.isHidden(state.currentTargetId)) {

            state.currentTargetId = null;
            state.lastKnownPosition = null;
            state.hiddenSinceMs = -1L;
            return;
        }

        if (state.hiddenSinceMs < 0L) {
            state.hiddenSinceMs = System.currentTimeMillis();
        }
        checkScentMemory();
    }

    private void checkScentMemory() {
        int scentMemoryTicks = AssistantBridge.getIntSetting(plugin, "seeker-items.assistant.scent-memory-ticks", 100);
        long timeoutMs = Math.max(1, scentMemoryTicks) * 50L;

        if (state.currentTargetId != null && state.isScentMemoryExpired(timeoutMs)) {
            state.currentTargetId = null;
            state.lastKnownPosition = null;
            state.hiddenSinceMs = -1L;
        }
    }

    private void spawnTrackingParticles() {
        if (state.currentTargetId == null) {
            return;
        }

        org.bukkit.entity.Mob mob = (org.bukkit.entity.Mob) nmsZombie.getBukkitEntity();
        Location head = mob.getLocation().add(0.0, 1.8, 0.0);
        head.getWorld().spawnParticle(Particle.SOUL, head, 3, 0.15, 0.1, 0.15, 0.01);
    }
}
