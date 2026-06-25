package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class BlindnessAntiCheatListener implements Listener {
    private final HideAndSeek plugin;
    private final Set<VisibilityPair> hiddenPairs = new HashSet<>();
    private BukkitTask reconcileTask;

    public BlindnessAntiCheatListener(HideAndSeek plugin) {
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
    public void onQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::reconcileVisibility);
    }

    private void refreshSoon() {
        Bukkit.getScheduler().runTask(plugin, this::reconcileVisibility);
    }

    private void reconcileVisibility() {
        if (!plugin.getSettingRegistry().get("anticheat.blindness.enabled", true)) {
            restoreAllVisibility();
            return;
        }

        double range = Math.max(0.5, plugin.getSettingRegistry().get("anticheat.blindness.range", 2.0));
        double rangeSq = range * range;

        boolean nmsPacketFilter = plugin.getNmsAdapter().hasCapability(NmsCapabilities.ANTI_CHEAT_PACKET_FILTER);
        List<UUID> hiders = new ArrayList<>(HideAndSeek.getDataController().getHiders());

        Set<VisibilityPair> desiredHiddenPairs = new HashSet<>();

        for (UUID hiderId : hiders) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline()) {
                continue;
            }

            boolean hasBlindness = hider.hasPotionEffect(PotionEffectType.BLINDNESS);
            boolean isInBlockMode = HideAndSeek.getDataController().isHidden(hiderId);

            if (!hasBlindness || !isInBlockMode) {
                continue;
            }

            for (Player other : Bukkit.getOnlinePlayers()) {
                if (other.getUniqueId().equals(hiderId)) {
                    continue;
                }

                if (!hider.getWorld().equals(other.getWorld())) {
                    desiredHiddenPairs.add(new VisibilityPair(hiderId, other.getUniqueId()));
                    continue;
                }

                double distSq = hider.getLocation().distanceSquared(other.getLocation());
                boolean shouldSee = distSq <= rangeSq;

                VisibilityPair pair = new VisibilityPair(hiderId, other.getUniqueId());
                if (!shouldSee) {
                    desiredHiddenPairs.add(pair);
                }

                boolean wasHidden = hiddenPairs.contains(pair);

                try {
                    if (nmsPacketFilter) {
                        boolean applied = plugin.getNmsAdapter().setEntityVisibilityForViewer(hider, other, shouldSee);
                        if (!applied || (shouldSee && wasHidden)) {
                            applyBukkitVisibility(hider, other, shouldSee, true);
                        }
                    } else {
                        applyBukkitVisibility(hider, other, shouldSee, false);
                    }
                } catch (Exception ex) {
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().warning(
                                "Blindness anti-cheat visibility failed for " + hider.getName() + " -> " + other.getName() + ": " + ex.getMessage());
                    }
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
                    plugin.getLogger().warning(
                            "Blindness stale visibility restore failed for " + viewer.getName() + " -> " + target.getName() + ": " + ex.getMessage());
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

    private void restoreAllVisibility() {
        for (VisibilityPair pair : new HashSet<>(hiddenPairs)) {
            Player viewer = Bukkit.getPlayer(pair.viewerId());
            Player target = Bukkit.getPlayer(pair.targetId());
            if (viewer == null || !viewer.isOnline() || target == null || !target.isOnline()) {
                continue;
            }

            try {
                plugin.getNmsAdapter().setEntityVisibilityForViewer(viewer, target, true);
                viewer.hideEntity(plugin, target);
                viewer.showEntity(plugin, target);
            } catch (Exception ex) {
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().warning(
                            "Blindness visibility restore failed for " + viewer.getName() + " -> " + target.getName() + ": " + ex.getMessage());
                }
            }
        }

        hiddenPairs.clear();
    }

    private record VisibilityPair(UUID viewerId, UUID targetId) {
    }
}
