package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;


public class InfernoBladeKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null) {
            return;
        }


        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_BLAZE_DEATH, 0.9f, 1.0f);
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_FIRE_AMBIENT, 0.7f, 0.9f);


        for (int i = 0; i < 35; i++) {
            double offsetX = (Math.random() - 0.5) * 2.5;
            double offsetY = Math.random() * 2.5;
            double offsetZ = (Math.random() - 0.5) * 2.5;

            killLocation.getWorld().spawnParticle(Particle.FLAME,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.3);
        }


        for (int i = 0; i < 25; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            killLocation.getWorld().spawnParticle(Particle.SMOKE,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0.1, 0.1, 0.1, 0.2);
        }


        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 3;
            double offsetZ = (Math.random() - 0.5) * 3;

            killLocation.getWorld().spawnParticle(Particle.LANDING_LAVA,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }
    }
}
