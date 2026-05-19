package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.SeekerAssistantCreeperEntity;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.SeekerAssistantEntity;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.assistant.SeekerAssistantSkeletonEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SeekerAssistantHandler {

    private static final String SKIN_GHOST_DRONE = "skin_ghost_drone";
    private static final String SKIN_BATTLE_MECH = "skin_battle_mech";
    private static final String SKIN_STEEL_GOLEM = "skin_steel_golem";
    private final Map<UUID, Set<UUID>> assistantIdsBySeeker = new ConcurrentHashMap<>();

    private static void configureAssistantBukkitEntity(Entity bukkit, Player seeker, Plugin plugin) {
        if (bukkit instanceof Mob mob) {
            mob.setCanPickupItems(false);
            mob.setSilent(false);
            mob.setRemoveWhenFarAway(false);
            mob.customName(net.kyori.adventure.text.Component.text("Seeker's Assistant"));
            mob.setCustomNameVisible(true);

            var speed = mob.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
            if (speed != null) {
                speed.setBaseValue(0.38D);
            }
            var health = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (health != null) {
                health.setBaseValue(6.0D);
                mob.setHealth(6.0D);
            }
            var fallDamage = mob.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER);
            if (fallDamage != null) {
                fallDamage.setBaseValue(0.0D);
            }
        }

        if (bukkit instanceof org.bukkit.entity.Creeper creeper) {
            creeper.setPowered(false);
            creeper.setExplosionRadius(0);
            creeper.setMaxFuseTicks(Short.MAX_VALUE);
        }

        bukkit.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "assistant_entity"),
                PersistentDataType.BOOLEAN, true);
        bukkit.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "assistant_owner"),
                PersistentDataType.STRING, seeker.getUniqueId().toString());

    }

    public Entity spawnSeekerAssistant(Plugin plugin, Player seeker, Location location,
                                       String assistantSkin) {
        if (plugin == null || seeker == null || location == null || location.getWorld() == null) {
            return null;
        }

        try {
            Level nmsLevel = ((CraftWorld) location.getWorld()).getHandle();
            PathfinderMob assistant = createAssistantEntity(
                    plugin, seeker, location, nmsLevel, assistantSkin);

            switch (assistant) {
                case SeekerAssistantEntity zombieAssistant -> zombieAssistant.injectGoals();
                case SeekerAssistantSkeletonEntity skeletonAssistant -> skeletonAssistant.injectGoals();
                case SeekerAssistantCreeperEntity creeperAssistant -> creeperAssistant.injectGoals();
                default -> {
                }
            }

            assistant.setPos(location.getX(), location.getY(), location.getZ());
            nmsLevel.addFreshEntity(assistant);

            Entity bukkit = assistant.getBukkitEntity();
            configureAssistantBukkitEntity(bukkit, seeker, plugin);

            assistantIdsBySeeker
                    .computeIfAbsent(seeker.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet())
                    .add(bukkit.getUniqueId());


            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Entity entity = plugin.getServer().getEntity(bukkit.getUniqueId());
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
                Set<UUID> ids = assistantIdsBySeeker.get(seeker.getUniqueId());
                if (ids != null) {
                    ids.remove(bukkit.getUniqueId());
                    if (ids.isEmpty()) {
                        assistantIdsBySeeker.remove(seeker.getUniqueId());
                    }
                }
            }, 90L * 20L);

            return bukkit;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private PathfinderMob createAssistantEntity(Plugin plugin, Player seeker, Location location,
                                                Level nmsLevel, String assistantSkin) {
        if (SKIN_GHOST_DRONE.equals(assistantSkin) || SKIN_STEEL_GOLEM.equals(assistantSkin)) {
            return new SeekerAssistantSkeletonEntity(
                    plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
        }
        if (SKIN_BATTLE_MECH.equals(assistantSkin)) {
            return new SeekerAssistantCreeperEntity(
                    plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
        }
        return new SeekerAssistantEntity(
                plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
    }

    public void removeAssistantsForSeeker(Plugin plugin, UUID seekerId) {
        Set<UUID> ids = assistantIdsBySeeker.remove(seekerId);
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (UUID assistantId : ids) {
            Entity entity = plugin.getServer().getEntity(assistantId);
            if (entity != null) {
                Location loc = entity.getLocation();
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.4, 0.5, 0.4, 0.03);
                    loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.8f);
                }
                entity.remove();
            }
        }
    }

    public Map<UUID, Set<UUID>> getAssistantIdsBySeeker() {
        return assistantIdsBySeeker;
    }
}
