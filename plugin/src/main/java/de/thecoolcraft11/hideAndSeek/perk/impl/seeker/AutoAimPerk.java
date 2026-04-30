package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class AutoAimPerk extends BasePerk {

    public static final String ID = "seeker_auto_aim";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Auto Aim", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("Guides the Seeker's Blade toward nearby hiders.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.TARGET;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.RARE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return 180;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {

    }
}

