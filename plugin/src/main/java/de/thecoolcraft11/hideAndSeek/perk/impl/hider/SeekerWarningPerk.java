package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.UUID;

public class SeekerWarningPerk extends BasePerk {
    @Override
    public String getId() {
        return "hider_seeker_warning";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Seeker Warning", NamedTextColor.RED);
    }

    @Override
    public Component getDescription() {
        return Component.text("Warns when a seeker approaches.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.BELL;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.RARE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 140;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("perks.perk.hider_seeker_warning.range", 12.0d);
        double movementRange = plugin.getSettingRegistry().get("perks.perk.hider_seeker_warning.movement-range", 8.0d);
        double fov = plugin.getSettingRegistry().get("perks.perk.hider_seeker_warning.fov", 45.0d);
        long cooldownTicks = plugin.getSettingRegistry().get("perks.perk.hider_seeker_warning.cooldown-ticks", 60L);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long now = System.currentTimeMillis();
            long last = plugin.getPerkStateManager().lastTriggerTime.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < cooldownTicks * 50L) {
                return;
            }

            for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
                Player seeker = Bukkit.getPlayer(seekerId);
                if (seeker == null || !seeker.isOnline() || !seeker.getWorld().equals(player.getWorld())) {
                    continue;
                }

                double dist = seeker.getLocation().distance(player.getLocation());
                if (dist > range) {
                    continue;
                }

                Vector toHider = player.getLocation().toVector().subtract(seeker.getEyeLocation().toVector()).normalize();
                double angle = Math.toDegrees(seeker.getEyeLocation().getDirection().angle(toHider));
                boolean inFov = angle <= fov;
                boolean movingCloser = dist <= movementRange && seeker.getVelocity().dot(toHider) > 0.05;

                if (inFov || movingCloser) {
                    player.showTitle(Title.title(
                            Component.empty(),
                            Component.text("Seeker approaching!", NamedTextColor.RED),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(120))
                    ));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.4f, 0.8f);

                    if (plugin.getNmsAdapter().hasCapability(de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities.CLIENT_FAKE_BORDER_WARNING)) {
                        int warningDistance = (int) Math.ceil(Math.max(4.0, movementRange));
                        plugin.getNmsAdapter().showWarningBorder(player, warningDistance);
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline()) {
                                plugin.getNmsAdapter().resetWarningBorder(player);
                            }
                        }, 24L);
                    }

                    plugin.getPerkStateManager().lastTriggerTime.put(player.getUniqueId(), now);
                    break;
                }
            }
        }, 0L, 10L);

        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
    }
}


