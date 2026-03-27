package de.thecoolcraft11.hideAndSeek.items.effects;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface KillEffect {

    void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin);
}
