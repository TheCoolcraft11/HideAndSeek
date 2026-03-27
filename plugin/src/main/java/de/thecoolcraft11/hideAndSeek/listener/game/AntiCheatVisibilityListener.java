package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.items.seeker.CameraItem;
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
import org.bukkit.potion.PotionEffectType;
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
        boolean hideDuringHiding = plugin.getSettingRegistry().get("anticheat.hiding.filter.enabled", true);
        boolean proximityDuringSeeking = plugin.getSettingRegistry().get("anticheat.seeking.filter.enabled", true);
        double seekingRange = Math.max(1.0, plugin.getSettingRegistry().get("anticheat.seeking.visibility-range", 12.0));
        double seekingRangeSq = seekingRange * seekingRange;
        boolean seekingLosRevealEnabled = plugin.getSettingRegistry().get("anticheat.seeking.line-of-sight.enabled", true);
        double seekingLosRevealRange = Math.max(seekingRange, plugin.getSettingRegistry().get("anticheat.seeking.line-of-sight.range", 64.0));
        double seekingLosRevealFov = Math.max(5.0, plugin.getSettingRegistry().get("anticheat.seeking.line-of-sight.fov", 24.0));
        boolean blockMode = String.valueOf(plugin.getSettingRegistry().get("game.mode", "NORMAL")).equals("BLOCK");

        List<UUID> seekers = new ArrayList<>(HideAndSeek.getDataController().getSeekers());
        List<UUID> hiders = new ArrayList<>(HideAndSeek.getDataController().getHiders());

        for (UUID seekerId : seekers) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker == null || !seeker.isOnline()) {
                continue;
            }


            ItemStateManager.CameraSessionState cameraSession = ItemStateManager.activeCameraSessions.get(seekerId);
            Location seekerViewLocation = null;
            Vector seekerViewDirection = null;
            double seekerViewDistanceSq = seekingRangeSq;

            if (cameraSession != null) {
                var cameraData = getCameraViewData(seekerId, cameraSession);
                if (cameraData != null) {
                    seekerViewLocation = cameraData.location;
                    seekerViewDirection = cameraData.direction;
                    seekerViewDistanceSq = cameraData.rangeSq;
                }
            }

            if (seekerViewLocation == null) {
                seekerViewLocation = seeker.getEyeLocation();
                seekerViewDirection = seeker.getLocation().getDirection();
                seekerViewDistanceSq = seekingRangeSq;
            }

            for (UUID hiderId : hiders) {
                if (seekerId.equals(hiderId)) {
                    continue;
                }

                Player hider = Bukkit.getPlayer(hiderId);
                if (hider == null || !hider.isOnline()) {
                    continue;
                }

                boolean isGlowing = hider.hasPotionEffect(PotionEffectType.GLOWING)
                        || HideAndSeek.getDataController().isGlowing(hiderId);

                boolean shouldSee = true;
                if (!isGlowing) {
                    if (phase.equals("hiding") && hideDuringHiding) {
                        shouldSee = false;
                    } else if (phase.equals("seeking") && proximityDuringSeeking) {
                        if (!seekerViewLocation.getWorld().equals(hider.getWorld())) {
                            shouldSee = false;
                        } else if (blockMode && HideAndSeek.getDataController().isHidden(hiderId)) {
                            shouldSee = false;
                        } else {
                            shouldSee = seekerViewLocation.distanceSquared(hider.getLocation()) <= seekerViewDistanceSq
                                    || (seekingLosRevealEnabled && hasLineOfSightReveal(seekerViewLocation, seekerViewDirection, hider, seekingLosRevealRange, seekingLosRevealFov));
                        }
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

    private CameraViewData getCameraViewData(UUID seekerId, ItemStateManager.CameraSessionState session) {
        var cameras = CameraItem.getPlacedCameras(seekerId);
        if (cameras.isEmpty()) {
            return null;
        }

        int idx = Math.floorMod(session.currentIndex(), cameras.size());
        var camera = cameras.get(idx);
        Location viewLoc = CameraItem.getViewLocation(camera);
        Vector viewDir = getDirectionFromYaw(session.rotationYaw());

        double seekingRange = Math.max(1.0, plugin.getSettingRegistry().get("anticheat.seeking.visibility-range", 12.0));
        return new CameraViewData(viewLoc, viewDir, seekingRange * seekingRange);
    }

    private Vector getDirectionFromYaw(float yaw) {
        float radians = (float) Math.toRadians(yaw);
        double x = -Math.sin(radians);
        double z = Math.cos(radians);
        return new Vector(x, 0, z).normalize();
    }

    private boolean hasLineOfSightReveal(Location eye, Vector eyeDirection, Player hider, double maxRange, double maxFovDegrees) {
        if (!eye.getWorld().equals(hider.getWorld())) {
            return false;
        }

        Location target = hider.getLocation().add(0, Math.min(1.2, hider.getHeight() * 0.75), 0);
        Vector toTarget = target.toVector().subtract(eye.toVector());
        double distance = toTarget.length();
        if (distance <= 0.001 || distance > maxRange) {
            return false;
        }

        Vector dir = toTarget.clone().normalize();
        double fovRadians = Math.toRadians(maxFovDegrees);
        if (eyeDirection.normalize().angle(dir) > fovRadians) {
            return false;
        }

        var blockHit = eye.getWorld().rayTraceBlocks(eye, dir, distance, FluidCollisionMode.NEVER, true);
        if (blockHit != null) {
            return false;
        }

        var entityHit = eye.getWorld().rayTraceEntities(
                eye,
                dir,
                distance,
                0.35,
                entity -> entity instanceof Player p && p.getUniqueId().equals(hider.getUniqueId())
        );

        return entityHit != null && entityHit.getHitEntity() != null;
    }

    private record CameraViewData(Location location, Vector direction, double rangeSq) {
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
