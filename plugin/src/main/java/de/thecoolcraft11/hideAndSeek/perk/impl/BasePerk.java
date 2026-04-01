package de.thecoolcraft11.hideAndSeek.perk.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public abstract class BasePerk implements PerkDefinition {

    @Override
    public Component getDisplayName() {
        return Component.text(getId().replace('_', ' '), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public Component getDescription() {
        return Component.text("No description.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    @Override
    public Material getIcon() {
        return Material.NETHER_STAR;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.COMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public PerkType getType() {
        return PerkType.PASSIVE;
    }

    @Override
    public int getCost() {
        return 100;
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        
    }
}

