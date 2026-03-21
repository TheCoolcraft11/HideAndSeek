package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AntiCheatVisibilityListener implements Listener {
    private final HideAndSeek plugin;
    private BukkitTask reconcileTask;

    public AntiCheatVisibilityListener(HideAndSeek plugin) {
        this.plugin = plugin;
        this.reconcileTask = Bukkit.getScheduler().runTaskTimer(plugin, this::reconcileVisibility, 5L, 10L);
    }

    public void shutdown() {
        if (reconcileTask != null) {
            reconcileTask.cancel();
            reconcileTask = null;
        }
        restoreAllVisibility();
        plugin.getNmsAdapter().clearVisibilityFilters();
    }

    public void refreshSoon() {
        Bukkit.getScheduler().runTask(plugin, this::reconcileVisibility);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::reconcileVisibility, 2L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, this::reconcileVisibility, 2L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::reconcileVisibility);
    }

    private void reconcileVisibility() {
        boolean nmsPacketFilter = plugin.getNmsAdapter().hasCapability(NmsCapabilities.ANTI_CHEAT_PACKET_FILTER);
        if (!plugin.getSettingRegistry().get("anticheat.enabled", true)) {
            restoreAllVisibility();
            plugin.getNmsAdapter().clearVisibilityFilters();
            return;
        }

        String phase = plugin.getStateManager().getCurrentPhaseId();
        boolean hideDuringHiding = plugin.getSettingRegistry().get("anticheat.hiding-filter-enabled", true);
        boolean proximityDuringSeeking = plugin.getSettingRegistry().get("anticheat.seeking-filter-enabled", true);
        double seekingRange = Math.max(1.0, plugin.getSettingRegistry().get("anticheat.seeking-visibility-range", 12.0));
        double seekingRangeSq = seekingRange * seekingRange;
        boolean seekingLosRevealEnabled = plugin.getSettingRegistry().get("anticheat.seeking-los-reveal-enabled", true);
        double seekingLosRevealRange = Math.max(seekingRange, plugin.getSettingRegistry().get("anticheat.seeking-los-reveal-range", 64.0));
        double seekingLosRevealFov = Math.max(5.0, plugin.getSettingRegistry().get("anticheat.seeking-los-reveal-fov", 24.0));
        boolean blockMode = String.valueOf(plugin.getSettingRegistry().get("game.gametype", "NORMAL")).equals("BLOCK");

        List<UUID> seekers = new ArrayList<>(HideAndSeek.getDataController().getSeekers());
        List<UUID> hiders = new ArrayList<>(HideAndSeek.getDataController().getHiders());

        for (UUID seekerId : seekers) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker == null || !seeker.isOnline()) {
                continue;
            }

            for (UUID hiderId : hiders) {
                if (seekerId.equals(hiderId)) {
                    continue;
                }

                Player hider = Bukkit.getPlayer(hiderId);
                if (hider == null || !hider.isOnline()) {
                    continue;
                }

                boolean shouldSee = true;
                if (phase.equals("hiding") && hideDuringHiding) {
                    shouldSee = false;
                } else if (phase.equals("seeking") && proximityDuringSeeking) {
                    if (!seeker.getWorld().equals(hider.getWorld())) {
                        shouldSee = false;
                    } else if (blockMode && HideAndSeek.getDataController().isHidden(hiderId)) {
                        shouldSee = false;
                    } else {
                        shouldSee = seeker.getLocation().distanceSquared(hider.getLocation()) <= seekingRangeSq
                                || (seekingLosRevealEnabled && hasLineOfSightReveal(seeker, hider, seekingLosRevealRange, seekingLosRevealFov));
                    }
                }

                try {
                    if (nmsPacketFilter) {
                        boolean applied = plugin.getNmsAdapter().setEntityVisibilityForViewer(seeker, hider, shouldSee);
                        if (!applied) {
                            if (shouldSee) {
                                seeker.showEntity(plugin, hider);
                            } else {
                                seeker.hideEntity(plugin, hider);
                            }
                        }
                    } else {
                        if (shouldSee) {
                            seeker.showEntity(plugin, hider);
                        } else {
                            seeker.hideEntity(plugin, hider);
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    private boolean hasLineOfSightReveal(Player seeker, Player hider, double maxRange, double maxFovDegrees) {
        if (!seeker.getWorld().equals(hider.getWorld())) {
            return false;
        }

        Location eye = seeker.getEyeLocation();
        Location target = hider.getLocation().add(0, Math.min(1.2, hider.getHeight() * 0.75), 0);
        Vector toTarget = target.toVector().subtract(eye.toVector());
        double distance = toTarget.length();
        if (distance <= 0.001 || distance > maxRange) {
            return false;
        }

        Vector dir = toTarget.clone().normalize();
        double fovRadians = Math.toRadians(maxFovDegrees);
        if (eye.getDirection().normalize().angle(dir) > fovRadians) {
            return false;
        }

        var blockHit = seeker.getWorld().rayTraceBlocks(eye, dir, distance, FluidCollisionMode.NEVER, true);
        if (blockHit != null) {
            return false;
        }

        var entityHit = seeker.getWorld().rayTraceEntities(
                eye,
                dir,
                distance,
                0.35,
                entity -> entity instanceof Player p && p.getUniqueId().equals(hider.getUniqueId())
        );

        return entityHit != null && entityHit.getHitEntity() != null;
    }

    private void restoreAllVisibility() {
        List<UUID> seekers = new ArrayList<>(HideAndSeek.getDataController().getSeekers());
        List<UUID> hiders = new ArrayList<>(HideAndSeek.getDataController().getHiders());

        for (UUID seekerId : seekers) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker == null || !seeker.isOnline()) {
                continue;
            }

            for (UUID hiderId : hiders) {
                if (seekerId.equals(hiderId)) {
                    continue;
                }

                Player hider = Bukkit.getPlayer(hiderId);
                if (hider == null || !hider.isOnline()) {
                    continue;
                }

                try {
                    plugin.getNmsAdapter().setEntityVisibilityForViewer(seeker, hider, true);
                    seeker.showEntity(plugin, hider);
                } catch (Exception ignored) {
                }
            }
        }
    }
}


