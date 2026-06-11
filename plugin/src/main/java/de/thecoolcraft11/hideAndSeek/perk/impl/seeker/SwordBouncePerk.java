package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class SwordBouncePerk extends BasePerk {

    public static final String ID = "seeker_sword_bounce";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Material getIcon() {
        return Material.RABBIT_FOOT;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.EPIC;
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

