package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class HumorousDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        return switch (cause) {
            case "CAMPING" ->
                    base(victimName).append(Component.text(" became one with the campfire. Literally.", NamedTextColor.GREEN));
            case "WORLD_BORDER" ->
                    base(victimName).append(Component.text(" tried to find the end of the world... and found it.", NamedTextColor.GREEN));
            case "DROWNING" -> base(victimName).append(
                    Component.text(" ran out of air and out of excuses.", NamedTextColor.GREEN));
            case "FIRE" ->
                    base(victimName).append(Component.text(" was grilled to medium-well.", NamedTextColor.GREEN));
            case "LAVA" -> base(victimName).append(
                    Component.text(" took a lava bath and instantly regretted it.", NamedTextColor.GREEN));
            case "SUFFOCATION" -> base(victimName).append(
                    Component.text(" learned that walls are not breathable.", NamedTextColor.GREEN));
            case "FREEZING" -> base(victimName).append(
                    Component.text(" became a limited-edition ice sculpture.", NamedTextColor.GREEN));
            case "HOT_FLOOR" -> base(victimName).append(
                    Component.text(" did the magma dance one step too long.", NamedTextColor.GREEN));
            case "CONTACT" -> base(victimName).append(
                    Component.text(" hugged a cactus and lost the argument.", NamedTextColor.GREEN));
            case "PERK_DEATH_ZONE" ->
                    base(victimName).append(Component.text(" discovered that zones have feelings too.", NamedTextColor.GREEN));
            case "PERK_RELOCATE" ->
                    base(victimName).append(Component.text(" teleported into the shadow realm by accident.", NamedTextColor.GREEN));
            default ->
                    base(victimName).append(Component.text(" became part of the environment.", NamedTextColor.GREEN));
        };
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.AQUA)
                .append(Component.text(" sent ", NamedTextColor.GRAY))
                .append(Component.text(victimName, NamedTextColor.YELLOW))
                .append(Component.text(" to the shadow realm!", NamedTextColor.GRAY));
    }

    private Component base(String victimName) {
        return Component.text(victimName, NamedTextColor.YELLOW);
    }
}

