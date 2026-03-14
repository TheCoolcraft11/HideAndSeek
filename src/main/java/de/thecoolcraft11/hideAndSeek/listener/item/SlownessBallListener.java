package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SlownessBallListener implements Listener {
    private final HideAndSeek plugin;

    public SlownessBallListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }


        try {
            Boolean isSlownessBall = snowball.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "slowness_ball"),
                    PersistentDataType.BOOLEAN
            );
            if (isSlownessBall == null || !isSlownessBall) {
                return;
            }
        } catch (Exception e) {
            return;
        }


        if (event.getHitEntity() instanceof Player seeker) {

            if (!HideAndSeek.getDataController().getSeekers().contains(seeker.getUniqueId())) {
                return;
            }


            int duration = 6;
            int amplifier = 1;
            try {
                Integer storedDuration = snowball.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "slowness_ball_duration"),
                        PersistentDataType.INTEGER
                );
                Integer storedAmplifier = snowball.getPersistentDataContainer().get(
                        new NamespacedKey(plugin, "slowness_ball_amplifier"),
                        PersistentDataType.INTEGER
                );
                if (storedDuration != null) duration = storedDuration;
                if (storedAmplifier != null) amplifier = storedAmplifier;
            } catch (Exception ignored) {

            }

            String skin = snowball.getPersistentDataContainer().get(
                    new NamespacedKey(plugin, "slowness_ball_skin"),
                    PersistentDataType.STRING
            );


            seeker.addPotionEffect(new PotionEffect(
                    PotionEffectType.SLOWNESS,
                    duration * 20,
                    amplifier,
                    false,
                    true,
                    true
            ));


            if ("sticky_honey".equals(skin)) {
                seeker.getWorld().spawnParticle(Particle.DRIPPING_HONEY, seeker.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
                seeker.getWorld().playSound(seeker.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.8f, 0.9f);
            } else if ("tar_ball".equals(skin)) {
                seeker.getWorld().spawnParticle(Particle.ASH, seeker.getLocation(), 20, 0.3, 0.3, 0.3, 0.05);
                seeker.getWorld().playSound(seeker.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.8f, 0.7f);
            } else {
                seeker.getWorld().spawnParticle(
                        Particle.SNOWFLAKE,
                        seeker.getLocation(),
                        15,
                        0.3, 0.3, 0.3,
                        0.1
                );
            }

            seeker.sendMessage(Component.text("You've been hit by a slowness ball!", NamedTextColor.AQUA));


            snowball.remove();
        }
    }
}


