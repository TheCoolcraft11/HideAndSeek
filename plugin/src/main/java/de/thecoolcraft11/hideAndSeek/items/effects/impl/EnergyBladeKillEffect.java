package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class EnergyBladeKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null) {
            return;
        }


        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8f, 1.0f);
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.6f, 1.2f);


        for (int i = 0; i < 30; i++) {
            double offsetX = (Math.random() - 0.5) * 3;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 3;

            killLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.3);
        }


        for (int i = 0; i < 20; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 1.5;
            double offsetZ = (Math.random() - 0.5) * 2;

            killLocation.getWorld().spawnParticle(Particle.END_ROD,
                    killLocation.clone().add(offsetX, offsetY, offsetZ),
                    1, 0, 0, 0, 0.2);
        }
    }
}
