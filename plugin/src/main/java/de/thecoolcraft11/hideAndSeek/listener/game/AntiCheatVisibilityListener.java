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
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiCheatVisibilityListener implements Listener {
    private final HideAndSeek plugin;
    private BukkitTask reconcileTask;
    private final Set<VisibilityPair> hiddenPairs = new HashSet<>();

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
        hiddenPairs.clear();
        plugin.getNmsAdapter().clearVisibilityFilters();
    }

    public void refreshSoon() {
        Bukkit.getScheduler().runTask(plugin, this::reconcileVisibility);
    }

    public void resetNow() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            restoreAllVisibility();
            hiddenPairs.clear();
            plugin.getNmsAdapter().clearVisibilityFilters();
        });
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
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        refreshSoon();
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        refreshSoon();
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        refreshSoon();
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
        double seekingLosRevealFov = Math.max(5.0, plugin.getSettingRegistry().get("anticheat.seeking.line-of-sight.fov", 60.0));
        boolean blockMode = String.valueOf(plugin.getSettingRegistry().get("game.mode", "NORMAL")).equals("BLOCK");

        List<UUID> seekers = new ArrayList<>(HideAndSeek.getDataController().getSeekers());
        List<UUID> hiders = new ArrayList<>(HideAndSeek.getDataController().getHiders());

        Set<VisibilityPair> desiredHiddenPairs = new HashSet<>();

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

                VisibilityPair pair = new VisibilityPair(seekerId, hiderId);
                boolean wasHidden = hiddenPairs.contains(pair);

                try {
                    if (nmsPacketFilter) {
                        boolean applied = plugin.getNmsAdapter().setEntityVisibilityForViewer(seeker, hider, shouldSee);
                        if (!applied || (shouldSee && wasHidden)) {
                            applyBukkitVisibility(seeker, hider, shouldSee, true);
                        }
                    } else {
                        applyBukkitVisibility(seeker, hider, shouldSee, false);
                    }
                } catch (Exception ex) {
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().warning("Anti-cheat visibility apply failed for " + seeker.getName() + " -> " + hider.getName() + ": " + ex.getMessage());
                    }
                }

                if (!shouldSee) {
                    desiredHiddenPairs.add(pair);
                }
            }
        }

        restoreStalePairs(desiredHiddenPairs);
        hiddenPairs.clear();
        hiddenPairs.addAll(desiredHiddenPairs);
    }

    private void restoreStalePairs(Set<VisibilityPair> desiredHiddenPairs) {
        for (VisibilityPair pair : new HashSet<>(hiddenPairs)) {
            if (desiredHiddenPairs.contains(pair)) {
                continue;
            }

            Player viewer = Bukkit.getPlayer(pair.viewerId());
            Player target = Bukkit.getPlayer(pair.targetId());
            if (viewer == null || !viewer.isOnline() || target == null || !target.isOnline()) {
                continue;
            }

            try {
                plugin.getNmsAdapter().setEntityVisibilityForViewer(viewer, target, true);
                applyBukkitVisibility(viewer, target, true, true);
            } catch (Exception ex) {
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().warning("Anti-cheat stale visibility restore failed for " + viewer.getName() + " -> " + target.getName() + ": " + ex.getMessage());
                }
            }
        }
    }

    private void applyBukkitVisibility(Player viewer, Player target, boolean shouldSee, boolean forceRefreshShow) {
        if (shouldSee) {
            if (forceRefreshShow) {
                viewer.hideEntity(plugin, target);
            }
            viewer.showEntity(plugin, target);
        } else {
            viewer.hideEntity(plugin, target);
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
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());

        for (Player viewer : onlinePlayers) {
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            for (Player target : onlinePlayers) {
                if (target == null || !target.isOnline() || viewer.getUniqueId().equals(target.getUniqueId())) {
                    continue;
                }

                try {
                    plugin.getNmsAdapter().setEntityVisibilityForViewer(viewer, target, true);
                    viewer.hideEntity(plugin, target);
                    viewer.showEntity(plugin, target);
                } catch (Exception ex) {
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().warning("Anti-cheat visibility restore failed for " + viewer.getName() + " -> " + target.getName() + ": " + ex.getMessage());
                    }
                }
            }
        }

        hiddenPairs.clear();
    }

    private record VisibilityPair(UUID viewerId, UUID targetId) {
    }
}
