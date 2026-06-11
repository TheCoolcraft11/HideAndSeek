package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class HitDisplayPerk extends BasePerk {

    public static final String ID = "seeker_hit_display";

    @Override
    public String getId() {
        return ID;
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
        return super.getCost();
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {

    }
}
