package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantBridge;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantSharedState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;

public class AssistantPathfindGoal extends Goal {

    private static final int PATH_RECALC_INTERVAL = 10;

    private final PathfinderMob nmsZombie;
    private final Plugin plugin;
    private final AssistantSharedState state;
    private int ticksUntilRecalc = 0;

    public AssistantPathfindGoal(PathfinderMob nmsZombie, Plugin plugin, AssistantSharedState state) {
        this.nmsZombie = nmsZombie;
        this.plugin = plugin;
        this.state = state;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return state.currentTargetId != null && resolveDestination() != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (state.currentTargetId == null) {
            return false;
        }

        if (!AssistantBridge.getHiders().contains(state.currentTargetId)) {
            return false;
        }

        Player target = AssistantBridge.getOnlinePlayer(state.currentTargetId);
        if (target != null && target.isOnline()) {
            return true;
        }

        return state.lastKnownPosition != null;
    }

    @Override
    public void start() {
        ticksUntilRecalc = 0;
    }

    @Override
    public void stop() {
        nmsZombie.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (--ticksUntilRecalc > 0) {
            return;
        }

        ticksUntilRecalc = PATH_RECALC_INTERVAL;
        Location destination = resolveDestination();
        if (destination == null) {
            return;
        }


        double speedMultiplier = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.pathfind-speed-multiplier", 1.0);
        double standoff = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.standoff-range", 6.0);
        double tolerance = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.standoff-tolerance", 1.5);

        Location mobLoc = nmsZombie.getBukkitEntity().getLocation();

        double dx = mobLoc.getX() - destination.getX();
        double dz = mobLoc.getZ() - destination.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist >= (standoff - tolerance) && dist <= (standoff + tolerance)) {
            nmsZombie.getNavigation().stop();
            tryShootFromCurrentPosition();
            return;
        }

        if (dist < (standoff - tolerance)) {
            var toMob = mobLoc.toVector().subtract(destination.toVector());
            if (toMob.lengthSquared() > 0.001) {
                var retreat = mobLoc.clone().add(toMob.normalize().multiply(Math.max(1.5, standoff - dist + 1.0)));
                int surfaceY = retreat.getWorld().getHighestBlockYAt(retreat.getBlockX(), retreat.getBlockZ());
                retreat.setY(surfaceY);
                nmsZombie.getNavigation().moveTo(retreat.getX(), retreat.getY(), retreat.getZ(), speedMultiplier);
                return;
            }
        }

        nmsZombie.getNavigation().moveTo(destination.getX(), destination.getY(), destination.getZ(), speedMultiplier);
        tryShootFromCurrentPosition();
    }

    @SuppressWarnings("resource")
    private void tryShootFromCurrentPosition() {
        if (state.currentTargetId == null) {
            return;
        }

        Player target = AssistantBridge.getOnlinePlayer(state.currentTargetId);
        if (target == null || !target.isOnline() || AssistantBridge.isHidden(state.currentTargetId)) {
            return;
        }

        long cooldown = AssistantBridge.getIntSetting(plugin, "seeker-items.assistant.shoot-cooldown", 70);
        long nowTick = nmsZombie.level().getGameTime();
        if (nowTick - state.lastShootTick < cooldown) {
            return;
        }

        double range = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.shoot-range", 18.0);
        Location mobLoc = nmsZombie.getBukkitEntity().getLocation();
        Location targetLoc = target.getLocation();
        double dx = mobLoc.getX() - targetLoc.getX();
        double dz = mobLoc.getZ() - targetLoc.getZ();
        if (Math.sqrt(dx * dx + dz * dz) > range) {
            return;
        }

        String owner = nmsZombie.getBukkitEntity().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "assistant_owner"), PersistentDataType.STRING);
        if (owner == null) {
            return;
        }

        java.util.UUID seekerId;
        try {
            seekerId = java.util.UUID.fromString(owner);
        } catch (IllegalArgumentException ex) {
            return;
        }

        if (!(nmsZombie.getBukkitEntity() instanceof LivingEntity living)) {
            return;
        }

        state.lastShootTick = nowTick;
        AssistantBridge.shootProjectile(plugin, living, living.getUniqueId(), target.getUniqueId(), seekerId);
    }

    private Location resolveDestination() {
        if (state.currentTargetId == null) {
            return null;
        }

        Player target = AssistantBridge.getOnlinePlayer(state.currentTargetId);
        if (target != null && target.isOnline() && !AssistantBridge.isHidden(state.currentTargetId)) {
            state.lastKnownPosition = target.getLocation();
            return state.lastKnownPosition;
        }

        if (state.lastKnownPosition == null) {
            state.currentTargetId = null;
            state.hiddenSinceMs = -1L;
        }
        return state.lastKnownPosition;
    }
}
