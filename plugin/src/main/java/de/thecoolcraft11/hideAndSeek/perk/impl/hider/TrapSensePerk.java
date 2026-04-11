package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TrapSensePerk extends BasePerk {

    @Override
    public String getId() {
        return "hider_trap_sense";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Trap Sense", NamedTextColor.RED);
    }

    @Override
    public Component getDescription() {
        return Component.text("Highlights nearby traps.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.SPYGLASS;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.COMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 60;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        double indicatorRange = plugin.getSettingRegistry().get("perks.perk.hider_trap_sense.indicator-range", 30.0d);
        double glowRange = plugin.getSettingRegistry().get("perks.perk.hider_trap_sense.glow-range", 20.0d);
        double warnRange = plugin.getSettingRegistry().get("perks.perk.hider_trap_sense.warn-range", 6.0d);
        long warnCooldown = plugin.getSettingRegistry().get("perks.perk.hider_trap_sense.warn-cooldown-ticks", 40L);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            boolean hasTrapNearby = false;

            Set<UUID> shownIndicators = plugin.getPerkStateManager().trapSenseShownIndicators
                    .computeIfAbsent(player.getUniqueId(), ignored -> new HashSet<>());

            for (Location trapLoc : ItemStateManager.cageTrapLocations.keySet()) {
                if (differentWorld(player.getLocation(), trapLoc)) {
                    continue;
                }
                double dist = player.getLocation().distance(trapLoc);
                if (dist <= indicatorRange) {
                    for (double y = 0; y <= 3; y += 0.5) {
                        player.spawnParticle(Particle.DUST, trapLoc.clone().add(0.5, y, 0.5), 1, 0, 0, 0, 0,
                                new Particle.DustOptions(Color.RED, 1.0f));
                    }
                }
                if (dist <= warnRange) {
                    hasTrapNearby = true;
                }
            }

            boolean canGlow = plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_ENTITY_GLOWING);
            if (canGlow) {
                updateTrapIndicatorVisibility(player, shownIndicators, glowRange, plugin);

                updateGlowSet(player, ItemStateManager.proximitySensorEntities, glowRange, plugin, Color.RED);
                updateGlowSet(player, ItemStateManager.cageTrapIndicatorEntities, glowRange, plugin, Color.YELLOW);
                updateGlowSet(player, ItemStateManager.cameraEntities, glowRange, plugin, Color.BLUE);
            }

            long now = System.currentTimeMillis();
            long last = plugin.getPerkStateManager().lastTriggerTime.getOrDefault(player.getUniqueId(), 0L);
            if (hasTrapNearby && now - last >= warnCooldown * 50L) {
                player.showTitle(Title.title(
                        Component.empty(),
                        Component.text("Trap nearby!", NamedTextColor.RED),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1000), Duration.ofMillis(120))
                ));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, 0.6f);
                plugin.getPerkStateManager().lastTriggerTime.put(player.getUniqueId(), now);
            }
        }, 0L, 20L);

        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());

        Set<UUID> shownIndicators = plugin.getPerkStateManager().trapSenseShownIndicators.remove(player.getUniqueId());
        if (shownIndicators != null) {
            for (UUID entityId : shownIndicators) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null && entity.isValid()) {
                    player.hideEntity(plugin, entity);
                }
            }
        }

        Set<UUID> glowed = plugin.getPerkStateManager().trapSenseGlowedEntities.remove(player.getUniqueId());
        if (glowed != null && plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_ENTITY_GLOWING)) {
            for (UUID entityId : glowed) {
                Entity entity = Bukkit.getEntity(entityId);
                if (entity != null) {
                    plugin.getNmsAdapter().setEntityGlowingForViewer(player, entity, false);
                    if (entity instanceof Display display) {
                        display.setGlowColorOverride(null);
                    }
                }
            }
        }
    }

    private void updateTrapIndicatorVisibility(Player viewer, Set<UUID> shownIndicators, double range, HideAndSeek plugin) {
        for (UUID id : ItemStateManager.cageTrapIndicatorEntities) {
            Entity entity = Bukkit.getEntity(id);
            if (entity == null || !entity.isValid() || differentWorld(viewer.getLocation(), entity.getLocation())) {
                continue;
            }

            boolean inRange = viewer.getLocation().distance(entity.getLocation()) <= range;
            if (inRange) {
                viewer.showEntity(plugin, entity);
                shownIndicators.add(id);
                continue;
            }

            if (shownIndicators.remove(id)) {
                viewer.hideEntity(plugin, entity);

                plugin.getNmsAdapter().setEntityGlowingForViewer(viewer, entity, false);
                if (entity instanceof Display display) {
                    display.setGlowColorOverride(null);
                }
            }
        }
    }

    private void updateGlowSet(Player viewer, Set<UUID> candidates, double range, HideAndSeek plugin, Color glowColor) {
        for (UUID id : candidates) {
            Entity entity = Bukkit.getEntity(id);
            if (entity == null || !entity.isValid() || differentWorld(viewer.getLocation(), entity.getLocation())) {
                continue;
            }
            if (viewer.getLocation().distance(entity.getLocation()) <= range) {
                if (entity instanceof Display display) {
                    display.setGlowColorOverride(glowColor);
                }
                plugin.getNmsAdapter().setEntityGlowingForViewer(viewer, entity, true);
            } else {
                plugin.getNmsAdapter().setEntityGlowingForViewer(viewer, entity, false);
                if (entity instanceof Display display) {
                    display.setGlowColorOverride(null);
                }
            }
        }
    }

    private boolean differentWorld(Location a, Location b) {
        return a == null || b == null || a.getWorld() == null || !a.getWorld().equals(b.getWorld());
    }
}







