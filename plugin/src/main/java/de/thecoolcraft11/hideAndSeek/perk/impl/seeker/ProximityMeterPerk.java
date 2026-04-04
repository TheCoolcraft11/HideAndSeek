package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.UUID;

public class ProximityMeterPerk extends BasePerk {

    private record PingTier(Component subtitle, float soundPitch, int intervalTicks) {
    }

    @Override
    public String getId() {
        return "seeker_proximity_meter";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Proximity Meter", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("Subtitle ping based on nearest hider distance.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.CLOCK;
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
        return 100;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        scheduleNextPing(player, plugin);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
        plugin.getPerkStateManager().proximityMeterNearest.remove(player.getUniqueId());
        player.showTitle(Title.title(Component.empty(), Component.empty(),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1), Duration.ZERO)));
    }

    private void scheduleNextPing(Player seeker, HideAndSeek plugin) {
        double nearestDist = Double.MAX_VALUE;
        UUID nearestId = null;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            double dist = hider.getLocation().distance(seeker.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestId = hiderId;
            }
        }

        if (nearestId == null) {
            plugin.getPerkStateManager().proximityMeterNearest.remove(seeker.getUniqueId());
        } else {
            plugin.getPerkStateManager().proximityMeterNearest.put(seeker.getUniqueId(), nearestId);
        }
        PingTier tier = tierFor(nearestDist, plugin);

        seeker.showTitle(Title.title(
                Component.empty(),
                tier.subtitle,
                Title.Times.times(Duration.ZERO,
                        Duration.ofMillis(tier.intervalTicks * 50L + 250L),
                        Duration.ofMillis(250L))
        ));

        seeker.playSound(seeker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.5f, tier.soundPitch);

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!seeker.isOnline()) {
                return;
            }
            if (!plugin.getPerkStateManager().hasPurchased(seeker.getUniqueId(), getId())) {
                return;
            }
            scheduleNextPing(seeker, plugin);
        }, tier.intervalTicks);

        plugin.getPerkStateManager().storeTask(seeker, getId(), task);
    }

    private PingTier tierFor(double dist, HideAndSeek plugin) {
        double burning = plugin.getSettingRegistry().get("perks.perk.seeker_proximity_meter.threshold-burning", 5.0d);
        double veryWarm = plugin.getSettingRegistry().get("perks.perk.seeker_proximity_meter.threshold-very-warm", 10.0d);
        double warm = plugin.getSettingRegistry().get("perks.perk.seeker_proximity_meter.threshold-warm", 18.0d);
        double lukewarm = plugin.getSettingRegistry().get("perks.perk.seeker_proximity_meter.threshold-lukewarm", 30.0d);
        double cool = plugin.getSettingRegistry().get("perks.perk.seeker_proximity_meter.threshold-cool", 50.0d);

        if (dist <= burning) {
            return new PingTier(Component.text("BURNING HOT", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true), 2.0f, 20);
        }
        if (dist <= veryWarm) {
            return new PingTier(Component.text("Very Warm", NamedTextColor.RED), 1.8f, 30);
        }
        if (dist <= warm) {
            return new PingTier(Component.text("Warm", NamedTextColor.GOLD), 1.5f, 40);
        }
        if (dist <= lukewarm) {
            return new PingTier(Component.text("Lukewarm", NamedTextColor.YELLOW), 1.2f, 50);
        }
        if (dist <= cool) {
            return new PingTier(Component.text("Cool", NamedTextColor.AQUA), 0.9f, 60);
        }
        return new PingTier(Component.text("Cold", NamedTextColor.BLUE), 0.6f, 80);
    }
}

