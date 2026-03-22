package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.SeekerKillModeEnum;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class SeekerKillModeListener implements Listener {
    private final HideAndSeek plugin;
    private final Map<UUID, Long> lastGazeKillTime = new HashMap<>();
    private static final long GAZE_KILL_COOLDOWN = 1000;

    public SeekerKillModeListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!(event.getDamager() instanceof Player damager)) {
            return;
        }


        if (!HideAndSeek.getDataController().getHiders().contains(victim.getUniqueId())) {
            return;
        }

        if (!HideAndSeek.getDataController().getSeekers().contains(damager.getUniqueId())) {
            return;
        }


        SeekerKillModeEnum killModeObj = plugin.getSettingRegistry().get("game.seekers.kill-mode", SeekerKillModeEnum.NORMAL);
        SeekerKillModeEnum killMode = (killModeObj != null) ?
                killModeObj : SeekerKillModeEnum.NORMAL;


        if (killMode == SeekerKillModeEnum.ONE_HIT) {
            event.setDamage(Double.MAX_VALUE);
            victim.setHealth(0);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player seeker = event.getPlayer();
        UUID seekerId = seeker.getUniqueId();


        if (!HideAndSeek.getDataController().getSeekers().contains(seekerId)) {
            return;
        }


        String currentPhase = event.getPlayer().getServer().getPluginManager().getPlugin("HideAndSeek") != null
                ? ((HideAndSeek) Objects.requireNonNull(event.getPlayer().getServer().getPluginManager().getPlugin("HideAndSeek"))).getStateManager().getCurrentPhaseId()
                : null;

        if (currentPhase == null || !currentPhase.equals("seeking")) {
            return;
        }


        SeekerKillModeEnum killModeObj = plugin.getSettingRegistry().get("game.seekers.kill-mode", SeekerKillModeEnum.NORMAL);
        SeekerKillModeEnum killMode = (killModeObj != null) ?
                killModeObj : SeekerKillModeEnum.NORMAL;


        if (killMode != SeekerKillModeEnum.GAZE_KILL) {
            return;
        }


        long now = System.currentTimeMillis();
        long lastKill = lastGazeKillTime.getOrDefault(seekerId, 0L);
        if (now - lastKill < GAZE_KILL_COOLDOWN) {
            return;
        }

        double maxDistance = plugin.getSettingRegistry().get("game.seekers.gaze-kill.max-distance", 10.0);
        double fovDegrees = plugin.getSettingRegistry().get("game.seekers.gaze-kill.fov", 30.0);
        boolean showParticles = plugin.getSettingRegistry().get("game.seekers.gaze-kill.show-particles", true);


        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = org.bukkit.Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || hider.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                continue;
            }


            if (!seeker.getWorld().equals(hider.getWorld())) {
                continue;
            }


            if (canSeePlayer(seeker, hider, maxDistance, fovDegrees, showParticles)) {

                hider.damage(Objects.requireNonNull(hider.getAttribute(Attribute.MAX_HEALTH)).getBaseValue(), seeker);
                lastGazeKillTime.put(seekerId, now);


                seeker.sendMessage(Component.text("You killed ", NamedTextColor.GREEN)
                        .append(Component.text(hider.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" by looking at them!", NamedTextColor.GREEN)));
                hider.sendMessage(Component.text("You were killed by ", NamedTextColor.RED)
                        .append(Component.text(seeker.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" through their gaze!", NamedTextColor.RED)));
                break;
            }
        }
    }

    private static boolean canSeePlayer(Player seeker, Player hider, double maxDistance, double fovDegrees, boolean showParticles) {
        if (!seeker.getWorld().equals(hider.getWorld())) return false;

        Location seekerEyes = seeker.getEyeLocation();
        Location hiderEyes = hider.getEyeLocation();

        double distance = seekerEyes.distance(hiderEyes);
        if (distance > maxDistance) return false;

        Vector toHider = hiderEyes.toVector().subtract(seekerEyes.toVector()).normalize();

        if (fovDegrees > 0) {
            Vector lookDir = seekerEyes.getDirection().normalize();
            double dot = lookDir.dot(toHider);
            double cosFOV = Math.cos(Math.toRadians(fovDegrees / 2));
            if (dot < cosFOV) return false;
        }

        RayTraceResult result = seeker.getWorld().rayTrace(
                seekerEyes,
                toHider,
                distance,
                FluidCollisionMode.NEVER,
                true,
                0.3,
                entity -> entity.equals(hider)
        );

        boolean canSee = result != null && result.getHitEntity() != null;

        if (showParticles && canSee) {
            drawLaserLine(seekerEyes, hiderEyes, seeker.getWorld());

            hider.getWorld().spawnParticle(
                    Particle.DUST,
                    hiderEyes,
                    5,
                    0.3, 0.3, 0.3,
                    new Particle.DustOptions(Color.RED, 1.5f)
            );
        }

        return canSee;
    }

    private static void drawLaserLine(Location start, Location end, org.bukkit.World world) {
        Vector direction = end.toVector().subtract(start.toVector());
        double distance = direction.length();
        Vector step = direction.normalize().multiply(0.2);

        Location point = start.clone();
        for (double d = 0; d < distance; d += 0.2) {
            world.spawnParticle(
                    Particle.DUST,
                    point,
                    1,
                    0,
                    0,
                    0,
                    new Particle.DustOptions(Color.RED, 0.8f)
            );
            point.add(step);
        }
    }
}




