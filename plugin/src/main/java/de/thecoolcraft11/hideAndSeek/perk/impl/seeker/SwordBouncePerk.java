package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class SwordBouncePerk extends BasePerk {

    public static final String ID = "seeker_sword_bounce";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Sword Bounce", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("The Seeker's Blade ricochets to nearby hiders after each hit.", NamedTextColor.GRAY);
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

