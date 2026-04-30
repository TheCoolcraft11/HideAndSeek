package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class HitDisplayPerk extends BasePerk {

    public static final String ID = "seeker_hit_display";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Hit Display", NamedTextColor.GREEN);
    }

    @Override
    public Component getDescription() {
        return Component.text("Shows the sword's projected flight curve while charging.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.FILLED_MAP;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.UNCOMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return 120;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {

    }
}

