package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.goals;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantBridge;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant.AssistantSharedState;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;

public class AssistantAlertGoal extends Goal {

    private final PathfinderMob nmsMob;
    private final Plugin plugin;
    private final AssistantSharedState state;

    public AssistantAlertGoal(PathfinderMob nmsMob, Plugin plugin, AssistantSharedState state) {
        this.nmsMob = nmsMob;
        this.plugin = plugin;
        this.state = state;
        this.setFlags(EnumSet.noneOf(Flag.class));
    }

    @SuppressWarnings("resource")
    @Override
    public boolean canUse() {
        if (state.currentTargetId == null) {
            return false;
        }

        long cooldownTicks = AssistantBridge.getIntSetting(plugin, "seeker-items.assistant.alert-cooldown", 80);
        long nowTick = nmsMob.level().getGameTime();
        if (nowTick - state.lastAlertTick < cooldownTicks) {
            return false;
        }

        Location targetLoc = resolveTargetLocation();
        if (targetLoc == null) {
            return false;
        }

        double range = AssistantBridge.getDoubleSetting(plugin, "seeker-items.assistant.alert-range", 12.0);
        Location mobLoc = nmsMob.getBukkitEntity().getLocation();
        return mobLoc.distance(targetLoc) <= range;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @SuppressWarnings("resource")
    @Override
    public void start() {
        state.lastAlertTick = nmsMob.level().getGameTime();

        Location mobLoc = nmsMob.getBukkitEntity().getLocation();
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_WOLF_AMBIENT, 1.2f, 0.85f);
        mobLoc.getWorld().playSound(mobLoc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.5f);
        mobLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, mobLoc, 8, 0.25, 0.35, 0.25, 0.02);

        Location targetLoc = resolveTargetLocation();
        if (targetLoc != null) {

            AssistantBridge.sendBeam(plugin, targetLoc, "alert");
        }

        if (state.currentTargetId != null) {
            Player seeker = null;
            String owner = nmsMob.getBukkitEntity().getPersistentDataContainer()
                    .get(new org.bukkit.NamespacedKey(plugin, "assistant_owner"), org.bukkit.persistence.PersistentDataType.STRING);
            if (owner != null) {
                try {
                    seeker = org.bukkit.Bukkit.getPlayer(java.util.UUID.fromString(owner));
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (seeker != null && seeker.isOnline()) {
                seeker.sendActionBar(net.kyori.adventure.text.Component.text("ASSISTANT ALERT - Target nearby!", net.kyori.adventure.text.format.NamedTextColor.RED));
            }
        }
    }

    private Location resolveTargetLocation() {
        if (state.currentTargetId == null) {
            return null;
        }

        Player target = AssistantBridge.getOnlinePlayer(state.currentTargetId);
        if (target != null && target.isOnline() && !AssistantBridge.isHidden(state.currentTargetId)) {
            return target.getLocation();
        }

        return state.lastKnownPosition;
    }
}
