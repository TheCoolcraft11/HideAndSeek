package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CreativeDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        if (plugin == null) {
            return Component.text(victimName + " was eliminated.");
        }
        String key = "death_messages.msg_creative." + cause.toLowerCase();
        String raw = plugin.trText(null, key, java.util.Map.of("victim", victimName));
        return MiniMessage.miniMessage().deserialize(raw);
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        if (plugin == null) {
            return Component.text(killerName + " eliminated " + victimName + ".");
        }
        String raw = plugin.trText(null, "death_messages.msg_creative.kill",
                java.util.Map.of("victim", victimName, "killer", killerName));
        return MiniMessage.miniMessage().deserialize(raw);
    }
}
