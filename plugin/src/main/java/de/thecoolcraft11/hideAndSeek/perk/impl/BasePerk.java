package de.thecoolcraft11.hideAndSeek.perk.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkType;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public abstract class BasePerk implements PerkDefinition {

    @Override
    public Component getDisplayName(@Nullable Player player) {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        if (plugin != null) {
            return plugin.tr(player, "perk." + getId() + ".name");
        }
        return Component.text(getId().replace('_', ' '));
    }

    @Override
    public Component getDescription(@Nullable Player player) {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        if (plugin != null) {
            return plugin.tr(player, "perk." + getId() + ".description");
        }
        return Component.text(getId().replace('_', ' '));
    }

    @Override
    public Material getIcon() {
        return Material.NETHER_STAR;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.NONE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.NONE;
    }

    @Override
    public PerkType getType() {
        return PerkType.NONE;
    }

    @Override
    public int getCost() {
        HideAndSeek plugin = (HideAndSeek) HideAndSeek.getActiveInstance();
        return plugin == null ? 0 : plugin.getSettingRegistry().get("perks.perk." + getId() + ".cost", 0);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {

    }
}

