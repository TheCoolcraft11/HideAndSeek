package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ClassicDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        return switch (cause) {
            case "CAMPING" ->
                    base(victimName).append(Component.text(" was struck down for camping too long.", NamedTextColor.YELLOW));
            case "WORLD_BORDER" ->
                    base(victimName).append(Component.text(" was consumed by the world border.", NamedTextColor.YELLOW));
            case "DROWNING" -> base(victimName).append(
                    Component.text(" drowned after staying underwater too long.", NamedTextColor.YELLOW));
            case "FIRE" -> base(victimName).append(Component.text(" burned for too long.", NamedTextColor.YELLOW));
            case "LAVA" -> base(victimName).append(
                    Component.text(" sank into lava and could not recover.", NamedTextColor.YELLOW));
            case "SUFFOCATION" ->
                    base(victimName).append(Component.text(" suffocated in a tight space.", NamedTextColor.YELLOW));
            case "FREEZING" -> base(victimName).append(Component.text(" froze solid.", NamedTextColor.YELLOW));
            case "HOT_FLOOR" -> base(victimName).append(
                    Component.text(" stood on scorching ground for too long.", NamedTextColor.YELLOW));
            case "CONTACT" -> base(victimName).append(
                    Component.text(" was shredded by hazardous terrain.", NamedTextColor.YELLOW));
            case "PERK_DEATH_ZONE" ->
                    base(victimName).append(Component.text(" failed to escape the Death Zone.", NamedTextColor.YELLOW));
            case "PERK_RELOCATE" ->
                    base(victimName).append(Component.text(" did not relocate in time.", NamedTextColor.YELLOW));
            default ->
                    base(victimName).append(Component.text(" was eliminated by the environment.", NamedTextColor.YELLOW));
        };
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.RED)
                .append(Component.text(" eliminated ", NamedTextColor.GRAY))
                .append(Component.text(victimName, NamedTextColor.GREEN))
                .append(Component.text(".", NamedTextColor.GRAY));
    }

    private Component base(String victimName) {
        return Component.text(victimName, NamedTextColor.GREEN);
    }
}

