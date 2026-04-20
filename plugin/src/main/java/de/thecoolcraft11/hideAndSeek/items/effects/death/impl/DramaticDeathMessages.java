package de.thecoolcraft11.hideAndSeek.items.effects.death.impl;

import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DramaticDeathMessages implements DeathMessageSkin {

    @Override
    public Component getEnvironmentalDeathMessage(String victimName, String cause) {
        return switch (cause) {
            case "CAMPING" ->
                    base(victimName).append(Component.text("'s reign ended... by their own indolence.", NamedTextColor.DARK_PURPLE));
            case "WORLD_BORDER" ->
                    base(victimName).append(Component.text(" ventured beyond the veil and was claimed by the void.", NamedTextColor.DARK_PURPLE));
            case "DROWNING" -> base(victimName).append(
                    Component.text(" was silenced by the crushing depths.", NamedTextColor.DARK_PURPLE));
            case "FIRE" -> base(victimName).append(
                    Component.text(" was consumed by relentless flames.", NamedTextColor.DARK_PURPLE));
            case "LAVA" ->
                    base(victimName).append(Component.text(" sank into molten ruin.", NamedTextColor.DARK_PURPLE));
            case "SUFFOCATION" -> base(victimName).append(
                    Component.text(" was entombed where no breath remained.", NamedTextColor.DARK_PURPLE));
            case "FREEZING" -> base(victimName).append(
                    Component.text(" was claimed by merciless frost.", NamedTextColor.DARK_PURPLE));
            case "HOT_FLOOR" -> base(victimName).append(
                    Component.text(" could not endure the searing ground.", NamedTextColor.DARK_PURPLE));
            case "CONTACT" -> base(victimName).append(
                    Component.text(" bled out against the cruel terrain.", NamedTextColor.DARK_PURPLE));
            case "PERK_DEATH_ZONE" ->
                    base(victimName).append(Component.text(" could not escape the wrath of the Death Zone.", NamedTextColor.DARK_PURPLE));
            case "PERK_RELOCATE" ->
                    base(victimName).append(Component.text(" failed to heed relocation and perished.", NamedTextColor.DARK_PURPLE));
            default -> base(victimName).append(Component.text(" met their final doom.", NamedTextColor.DARK_PURPLE));
        };
    }

    @Override
    public Component getKillMessage(String killerName, String victimName) {
        return Component.text(killerName, NamedTextColor.DARK_RED)
                .append(Component.text(" has vanquished ", NamedTextColor.GOLD))
                .append(Component.text(victimName, NamedTextColor.RED))
                .append(Component.text(" in glorious combat!", NamedTextColor.GOLD));
    }

    private Component base(String victimName) {
        return Component.text(victimName, NamedTextColor.RED);
    }
}

