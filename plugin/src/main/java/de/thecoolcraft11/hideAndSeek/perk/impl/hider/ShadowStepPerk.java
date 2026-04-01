package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ShadowStepPerk extends BasePerk {

    @Override
    public String getId() {
        return "hider_shadow_step";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Shadow Step", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("Auto-teleports once at critical HP.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.ENDER_PEARL;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.UNCOMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 100;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().shadowStepTriggered.put(player.getUniqueId(), false);
        BukkitTask monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.getGameMode() == GameMode.SPECTATOR) {
                return;
            }

            if (plugin.getPerkStateManager().shadowStepTriggered.getOrDefault(player.getUniqueId(), false)) {
                return;
            }

            double hpTrigger = plugin.getSettingRegistry().get("perks.perk.hider_shadow_step.hp-trigger", 5.0d);
            if (player.getHealth() > hpTrigger) {
                return;
            }

            plugin.getPerkStateManager().shadowStepTriggered.put(player.getUniqueId(), true);
            player.showTitle(Title.title(
                    Component.empty(),
                    Component.text("Shadow Step charging...", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1400), Duration.ofMillis(120))
            ));
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.9f, 1.0f);

            plugin.getPerkStateManager().cancelTask(player, getId());

            int chargeTicks = plugin.getSettingRegistry().get("perks.perk.hider_shadow_step.charge-ticks", 30);
            BukkitTask chargeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                plugin.getPerkStateManager().shadowStepChargeTask.remove(player.getUniqueId());
                if (!player.isOnline() || player.getGameMode() == GameMode.SPECTATOR || player.isDead()) {
                    return;
                }

                Location target = findTeleportLocation(player, plugin);
                if (target == null) {
                    return;
                }

                Location oldLoc = player.getLocation().clone();
                player.teleport(target);
                oldLoc.getWorld().spawnParticle(Particle.PORTAL, oldLoc, 30, 0.5, 0.8, 0.5, 0.05);
                target.getWorld().spawnParticle(Particle.PORTAL, target, 30, 0.5, 0.8, 0.5, 0.05);
                oldLoc.getWorld().playSound(oldLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                target.getWorld().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                player.showTitle(Title.title(Component.text("Shadow Step!", NamedTextColor.AQUA), Component.empty(),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(900), Duration.ofMillis(300))));

                onExpire(player, plugin);
            }, chargeTicks);

            plugin.getPerkStateManager().shadowStepChargeTask.put(player.getUniqueId(), chargeTask);
        }, 0L, 5L);

        plugin.getPerkStateManager().storeTask(player, getId(), monitorTask);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
        BukkitTask chargeTask = plugin.getPerkStateManager().shadowStepChargeTask.remove(player.getUniqueId());
        if (chargeTask != null) {
            chargeTask.cancel();
        }
    }

    private Location findTeleportLocation(Player player, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("perks.perk.hider_shadow_step.teleport-range", 15.0d);
        double minSeekerDistance = plugin.getSettingRegistry().get("perks.perk.hider_shadow_step.min-seeker-distance", 5.0d);

        List<Player> seekers = HideAndSeek.getDataController().getSeekers().stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline() && p.getWorld().equals(player.getWorld()))
                .toList();

        WorldBorder border = player.getWorld().getWorldBorder();
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(3.0, range);
            Location base = player.getLocation().clone().add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
            int y = base.getWorld().getHighestBlockYAt(base.getBlockX(), base.getBlockZ());
            Location tp = new Location(base.getWorld(), base.getBlockX() + 0.5, y + 1.0, base.getBlockZ() + 0.5, player.getYaw(), player.getPitch());

            if (!border.isInside(tp)) {
                continue;
            }

            Block feet = tp.getBlock();
            Block below = tp.clone().add(0, -1, 0).getBlock();
            if (!feet.isPassable() || !below.getType().isSolid()) {
                continue;
            }

            boolean farEnough = seekers.stream().allMatch(seeker -> seeker.getLocation().distance(tp) > minSeekerDistance);
            if (farEnough) {
                return tp;
            }
        }
        return null;
    }
}


