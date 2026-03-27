package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class FrostbiteKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null) {
            return;
        }


        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_GLASS_BREAK, 0.8f, 0.7f);
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_SNOW_BREAK, 0.6f, 0.8f);


        for (int i = 0; i < 30; i++) {
            double offsetX = (Math.random() - 0.5) * 2.5;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2.5;

            killLocation.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }


        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 2;

            killLocation.getWorld().spawnParticle(Particle.SOUL,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.15);
        }


        for (int i = 0; i < 18; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2.5;
            double offsetZ = (Math.random() - 0.5) * 3;

            killLocation.getWorld().spawnParticle(Particle.DUST,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }
    }
}
