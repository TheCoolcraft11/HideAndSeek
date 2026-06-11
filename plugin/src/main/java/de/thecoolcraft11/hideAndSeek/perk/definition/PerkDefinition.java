package de.thecoolcraft11.hideAndSeek.perk.definition;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public interface PerkDefinition {

    String getId();

    Component getDisplayName(@Nullable Player player);

    Component getDescription(@Nullable Player player);

    Material getIcon();

    PerkTier getTier();

    PerkTarget getTarget();

    PerkType getType();

    int getCost();

    void onPurchase(Player player, HideAndSeek plugin);

    void onExpire(Player player, HideAndSeek plugin);
}

