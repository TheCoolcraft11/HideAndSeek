package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class GiantSpatulaKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null) {
            return;
        }


        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_ARROW_SHOOT, 0.7f, 0.5f);
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_WOOL_PLACE, 0.6f, 1.3f);


        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * 2.5;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2.5;

            killLocation.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }


        for (int i = 0; i < 15; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 2;

            killLocation.getWorld().spawnParticle(Particle.NOTE,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, Math.random(), Math.random(), Math.random(), 0.1);
        }


        for (int i = 0; i < 18; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2.5;
            double offsetZ = (Math.random() - 0.5) * 3;

            killLocation.getWorld().spawnParticle(Particle.ITEM_SNOWBALL,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.15);
        }


        for (int i = 0; i < 12; i++) {
            double offsetX = (Math.random() - 0.5) * 2.5;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 2.5;

            killLocation.getWorld().spawnParticle(Particle.SPLASH,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }
    }
}
