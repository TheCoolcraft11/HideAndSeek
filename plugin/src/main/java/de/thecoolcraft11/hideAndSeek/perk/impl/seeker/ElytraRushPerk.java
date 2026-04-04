package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ElytraRushPerk extends BasePerk {

    private static final Set<UUID> noFall = new HashSet<>();

    @Override
    public String getId() {
        return "seeker_elytra_rush";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Elytra Rush", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("Temporary gliding boost.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.RARE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return 180;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        long durationTicks = plugin.getSettingRegistry().get("perks.perk.seeker_elytra_rush.duration-ticks", 600L);
        double launchPower = plugin.getSettingRegistry().get("perks.perk.seeker_elytra_rush.launch-power", 1.8d);
        boolean fallbackToLevitation = plugin.getSettingRegistry().get("perks.perk.seeker_elytra_rush.fallback-to-levitation", true);

        plugin.getPerkStateManager().cancelTask(player, getId());
        plugin.getPerkStateManager().cancelTask(player, getId() + "_tick");

        Vector velocity = player.getVelocity();
        velocity.setY(Math.max(velocity.getY(), launchPower));
        player.setVelocity(velocity);
        player.setGliding(true);

        if (fallbackToLevitation) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                if (!player.isGliding()) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, (int) Math.min(60L, durationTicks), 0, false, false, true));
                }
            }, 1L);
        }

        noFall.add(player.getUniqueId());

        BukkitTask tickTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticksPassed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    return;
                }

                ticksPassed++;

                boolean onGround = player.getLocation().clone().subtract(0.0, 0.1, 0.0).getBlock().getType().isSolid();
                if (onGround && ticksPassed >= 10) {
                    stopForPlayer(player, plugin);
                    return;
                }

                player.setGliding(true);
            }
        }, 1L, 1L);


        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            noFall.remove(player.getUniqueId());
            plugin.getPerkStateManager().cancelTask(player, getId() + "_tick");
            player.setGliding(false);
        }, durationTicks);

        plugin.getPerkStateManager().storeTask(player, getId() + "_tick", tickTask);
        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        noFall.remove(player.getUniqueId());
        plugin.getPerkStateManager().cancelTask(player, getId() + "_tick");
        plugin.getPerkStateManager().cancelTask(player, getId());
        player.setGliding(false);
    }

    public static boolean hasNoFall(UUID playerId) {
        return noFall.contains(playerId);
    }

    public static void stopForPlayer(Player player, HideAndSeek plugin) {
        if (player == null || plugin == null) {
            return;
        }
        noFall.remove(player.getUniqueId());
        plugin.getPerkStateManager().cancelTask(player, "seeker_elytra_rush_tick");
        plugin.getPerkStateManager().cancelTask(player, "seeker_elytra_rush");
        player.setGliding(false);
    }
}







