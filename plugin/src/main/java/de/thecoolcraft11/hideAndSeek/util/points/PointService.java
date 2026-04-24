package de.thecoolcraft11.hideAndSeek.util.points;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class PointService {
    private final HideAndSeek plugin;

    private final Map<UUID, Integer> hiderProximitySeconds = new HashMap<>();
    private final Map<UUID, Integer> seekerCaptures = new HashMap<>();
    private final Set<UUID> utilitySpottedHiders = new HashSet<>();

    private final Set<UUID> nearMissInDangerZone = new HashSet<>();
    private final Map<UUID, Long> nearMissEscapeDeadlineMs = new HashMap<>();

    private BukkitTask seekingTrackerTask;
    private boolean firstBloodAwarded;

    public PointService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void resetRoundState() {
        stopSeekingTracking();
        hiderProximitySeconds.clear();
        seekerCaptures.clear();
        utilitySpottedHiders.clear();
        nearMissInDangerZone.clear();
        nearMissEscapeDeadlineMs.clear();
        firstBloodAwarded = false;
    }

    public int getInt(String path, int fallback) {
        return plugin.getSettingRegistry().get(path, fallback);
    }

    public double getDouble(String path, double fallback) {
        return plugin.getSettingRegistry().get(path, fallback);
    }

    public int award(UUID playerId, PointAction action) {
        if (playerId == null || action == null) {
            return 0;
        }
        int points = getInt(action.getSettingPath(), action.getDefaultPoints());
        if (points != 0) {
            HideAndSeek.getDataController().addPoints(playerId, points);
        }
        if (action == PointAction.HIDER_TAUNT_SMALL || action == PointAction.HIDER_TAUNT_LARGE) {
            var active = HideAndSeek.getActiveInstance();
            if (active instanceof HideAndSeek) {
                var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
                if (statsService != null) {
                    statsService.recordTauntUsed(playerId);
                }
            }
        }
        return points;
    }

    public void markUtilitySpotted(UUID hiderId) {
        utilitySpottedHiders.add(hiderId);
    }

    public void onHiderDamagedBySeeker(Player seeker, Player hider, double finalDamage) {
        if (seeker == null || hider == null) {
            return;
        }
        if (finalDamage < hider.getHealth()) {
            award(seeker.getUniqueId(), PointAction.SEEKER_INTERCEPTION);
        }
    }

    public int onHiderEliminated(Player hider, Player killer) {
        if (hider == null || killer == null) {
            return 0;
        }

        UUID killerId = killer.getUniqueId();
        var active = HideAndSeek.getActiveInstance();
        if (active instanceof HideAndSeek) {
            var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
            if (statsService != null) {
                statsService.recordSeekerKill(killerId);
            }
        }
        int killPoints = award(killerId, PointAction.SEEKER_KILL);
        seekerCaptures.merge(killerId, 1, Integer::sum);

        if (!firstBloodAwarded) {
            award(killerId, PointAction.SEEKER_FIRST_BLOOD);
            firstBloodAwarded = true;
        }

        double assistRange = getDouble("points.seeker.assist.range", 16.0);
        double assistRangeSquared = assistRange * assistRange;

        for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
            if (seekerId.equals(killerId)) {
                continue;
            }

            Player seeker = Bukkit.getPlayer(seekerId);
            if (!isActiveSeeker(seeker)) {
                continue;
            }
            if (!seeker.getWorld().equals(hider.getWorld())) {
                continue;
            }
            if (seeker.getLocation().distanceSquared(hider.getLocation()) <= assistRangeSquared) {
                award(seekerId, PointAction.SEEKER_ASSIST);
            }
        }

        nearMissInDangerZone.remove(hider.getUniqueId());
        nearMissEscapeDeadlineMs.remove(hider.getUniqueId());
        return killPoints;
    }

    public void startSeekingTracking() {
        stopSeekingTracking();

        int intervalSeconds = Math.max(1, getInt("points.tracking.interval-seconds", 1));
        long periodTicks = intervalSeconds * 20L;

        seekingTrackerTask = new BukkitRunnable() {
            @Override
            public void run() {
                tickSeekingTracking();
            }
        }.runTaskTimer(plugin, periodTicks, periodTicks);
    }

    public void stopSeekingTracking() {
        if (seekingTrackerTask != null) {
            seekingTrackerTask.cancel();
            seekingTrackerTask = null;
        }
    }

    private void tickSeekingTracking() {
        List<Player> activeSeekers = new ArrayList<>();
        for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (isActiveSeeker(seeker)) {
                activeSeekers.add(seeker);
            }
        }

        List<Player> activeHiders = new ArrayList<>();
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (isActiveHider(hider)) {
                activeHiders.add(hider);
            }
        }

        double hiderProximityRange = getDouble("points.hider.proximity.range", 8.0);
        double hiderProximityRangeSquared = hiderProximityRange * hiderProximityRange;

        double nearMissRange = getDouble("points.hider.near-miss.range", 3.0);
        double nearMissRangeSquared = nearMissRange * nearMissRange;

        double seekerActiveHunterRange = getDouble("points.seeker.active-hunter.range", 16.0);
        double seekerActiveHunterRangeSquared = seekerActiveHunterRange * seekerActiveHunterRange;

        for (Player hider : activeHiders) {
            boolean closeToAnySeeker = false;
            boolean nearMissDanger = false;

            for (Player seeker : activeSeekers) {
                if (!seeker.getWorld().equals(hider.getWorld())) {
                    continue;
                }

                double distanceSquared = seeker.getLocation().distanceSquared(hider.getLocation());
                if (distanceSquared <= hiderProximityRangeSquared) {
                    closeToAnySeeker = true;
                }
                if (distanceSquared <= nearMissRangeSquared) {
                    nearMissDanger = true;
                }
            }

            UUID hiderId = hider.getUniqueId();
            if (closeToAnySeeker) {
                award(hiderId, PointAction.HIDER_PROXIMITY_BONUS);
                hiderProximitySeconds.merge(hiderId, 1, Integer::sum);
            }

            if (nearMissDanger) {
                nearMissInDangerZone.add(hiderId);
                nearMissEscapeDeadlineMs.remove(hiderId);
            } else if (nearMissInDangerZone.remove(hiderId)) {
                int escapeSeconds = Math.max(1, getInt("points.hider.near-miss.escape-seconds", 4));
                nearMissEscapeDeadlineMs.put(hiderId, System.currentTimeMillis() + (escapeSeconds * 1000L));
            }
        }

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> nearMissIterator = nearMissEscapeDeadlineMs.entrySet().iterator();
        while (nearMissIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = nearMissIterator.next();
            UUID hiderId = entry.getKey();
            long deadline = entry.getValue();

            if (now < deadline) {
                continue;
            }

            Player hider = Bukkit.getPlayer(hiderId);
            if (isActiveHider(hider)) {
                award(hiderId, PointAction.HIDER_NEAR_MISS);
            }
            nearMissIterator.remove();
        }

        for (Player seeker : activeSeekers) {
            boolean nearAnyHider = false;
            for (Player hider : activeHiders) {
                if (!hider.getWorld().equals(seeker.getWorld())) {
                    continue;
                }

                double distanceSquared = hider.getLocation().distanceSquared(seeker.getLocation());
                if (distanceSquared <= seekerActiveHunterRangeSquared) {
                    nearAnyHider = true;
                    break;
                }
            }

            if (nearAnyHider) {
                award(seeker.getUniqueId(), PointAction.SEEKER_ACTIVE_HUNTER);
            }
        }
    }

    public void awardRoundEndBonuses(List<UUID> survivingHiders) {
        for (UUID hiderId : survivingHiders) {
            award(hiderId, PointAction.HIDER_SURVIVOR);
            if (!utilitySpottedHiders.contains(hiderId)) {
                award(hiderId, PointAction.HIDER_SPECIAL_GHOST);
            }
        }

        int topProximity = hiderProximitySeconds.values().stream().max(Integer::compareTo).orElse(0);
        if (topProximity > 0) {
            hiderProximitySeconds.entrySet().stream()
                    .filter(entry -> entry.getValue() == topProximity)
                    .forEach(entry -> award(entry.getKey(), PointAction.HIDER_SPECIAL_DISTRACTOR));
        }

        int topCaptures = seekerCaptures.values().stream().max(Integer::compareTo).orElse(0);
        if (topCaptures > 0) {
            seekerCaptures.entrySet().stream()
                    .filter(entry -> entry.getValue() == topCaptures)
                    .forEach(entry -> award(entry.getKey(), PointAction.SEEKER_SPECIAL_BLOODHOUND));
        }
    }

    private boolean isActiveHider(Player player) {
        return player != null
                && player.isOnline()
                && player.getGameMode() != GameMode.SPECTATOR
                && HideAndSeek.getDataController().getHiders().contains(player.getUniqueId());
    }

    private boolean isActiveSeeker(Player player) {
        return player != null
                && player.isOnline()
                && player.getGameMode() != GameMode.SPECTATOR
                && HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId());
    }
}
