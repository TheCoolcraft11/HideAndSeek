package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantBridge;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantSharedState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Random;

public class AssistantWanderGoal extends Goal {

    private static final int MAX_PICK_ATTEMPTS = 5;
    private static final Random RNG = new Random();

    private final PathfinderMob nmsZombie;
    private final Plugin plugin;
    private final AssistantSharedState state;
    private int ticksUntilNextDest = 0;
    private int idleTicks = 0;

    public AssistantWanderGoal(PathfinderMob nmsZombie, Plugin plugin, AssistantSharedState state) {
        this.nmsZombie = nmsZombie;
        this.plugin = plugin;
        this.state = state;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return state.currentTargetId == null;
    }

    @Override
    public boolean canContinueToUse() {
        return state.currentTargetId == null;
    }

    @Override
    public void start() {
        ticksUntilNextDest = 0;
        idleTicks = 0;
    }

    @Override
    public void stop() {
        nmsZombie.getNavigation().stop();
    }

    @Override
    public void tick() {
        if (nmsZombie.getNavigation().isDone()) {
            idleTicks++;
        } else {
            idleTicks = 0;
        }

        if (idleTicks >= 20) {
            ticksUntilNextDest = 0;
        }

        if (--ticksUntilNextDest > 0) {
            return;
        }

        ticksUntilNextDest = 40 + RNG.nextInt(61);
        pickAndMoveTo();
    }

    private void pickAndMoveTo() {
        Location origin = state.wanderOrigin;

        double r1 = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.wander-radius-phase1", 15.0);
        double r2 = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.wander-radius-phase2", 25.0);
        double r3 = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.wander-radius-phase3", 40.0);

        long aliveSeconds = (System.currentTimeMillis() - state.spawnTimeMs) / 1000L;
        double radius = aliveSeconds < 20 ? r1 : (aliveSeconds < 50 ? r2 : r3);

        for (int i = 0; i < MAX_PICK_ATTEMPTS; i++) {
            double angle = RNG.nextDouble() * Math.PI * 2;
            double dist = RNG.nextDouble() * radius;

            double x = origin.getX() + Math.cos(angle) * dist;
            double z = origin.getZ() + Math.sin(angle) * dist;
            int y = origin.getWorld().getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z));

            Location candidate = new Location(origin.getWorld(), x, y, z);
            if (candidate.distanceSquared(origin) <= radius * radius) {
                nmsZombie.getNavigation().moveTo(candidate.getX(), candidate.getY(), candidate.getZ(), 1.0);
                return;
            }
        }


        var fallback = DefaultRandomPos.getPos(nmsZombie, 10, 7);
        if (fallback != null) {
            nmsZombie.getNavigation().moveTo(fallback.x, fallback.y, fallback.z, 1.0);
        }
    }
}
