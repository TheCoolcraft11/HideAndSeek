package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScentTrailPerk extends BasePerk {

    private record TrailPoint(Location location, long expiresAtMs) {
    }

    @Override
    public String getId() {
        return "seeker_scent_trail";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Scent Trail", NamedTextColor.GOLD);
    }

    @Override
    public Component getDescription() {
        return Component.text("Shows recent hider movement trail.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.BRUSH;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.UNCOMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return 90;
    }

    @Override
    public void onPurchase(Player seeker, HideAndSeek plugin) {
        int interval = plugin.getSettingRegistry().get("perks.perk.seeker_scent_trail.trail-interval-ticks", 5);
        double moveThreshold = plugin.getSettingRegistry().get("perks.perk.seeker_scent_trail.move-threshold", 0.3d);
        int lifeSeconds = plugin.getSettingRegistry().get("perks.perk.seeker_scent_trail.particle-lifetime-seconds", 10);

        Map<UUID, Location> lastPos = new HashMap<>();
        List<TrailPoint> trail = new ArrayList<>();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();

            Iterator<TrailPoint> iterator = trail.iterator();
            while (iterator.hasNext()) {
                TrailPoint point = iterator.next();
                if (point.expiresAtMs() <= now) {
                    iterator.remove();
                    continue;
                }
                if (!seeker.isOnline() || !seeker.getWorld().equals(point.location().getWorld())) {
                    continue;
                }

                
                seeker.spawnParticle(Particle.DUST,
                        point.location(),
                        8, 0.22, 0.02, 0.22, 0,
                        new Particle.DustOptions(Color.fromRGB(120, 72, 35), 1.8f));
                seeker.spawnParticle(Particle.FALLING_DUST,
                        point.location(),
                        4, 0.18, 0.02, 0.18, 0,
                        Material.DIRT.createBlockData());
            }

            for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                Player hider = Bukkit.getPlayer(hiderId);
                if (hider == null || !hider.isOnline() || !hider.getWorld().equals(seeker.getWorld())) {
                    continue;
                }
                if (HideAndSeek.getDataController().isHidden(hiderId)) {
                    continue;
                }

                Location current = hider.getLocation().clone();
                Location previous = lastPos.put(hiderId, current);
                if (previous == null || previous.distance(current) <= moveThreshold) {
                    continue;
                }

                Location scentLoc = current.clone().add(0, 0.05, 0);
                trail.add(new TrailPoint(scentLoc, now + lifeSeconds * 1000L));
                seeker.spawnParticle(Particle.DUST,
                        scentLoc,
                        10, 0.24, 0.03, 0.24, 0,
                        new Particle.DustOptions(Color.fromRGB(140, 82, 40), 2.0f));
            }
        }, 0L, Math.max(2L, interval));

        plugin.getPerkStateManager().storeTask(seeker, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
    }
}


