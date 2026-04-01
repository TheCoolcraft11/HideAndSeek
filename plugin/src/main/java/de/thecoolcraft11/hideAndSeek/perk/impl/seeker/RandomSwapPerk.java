package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomSwapPerk extends BasePerk {
    @Override
    public String getId() {
        return "seeker_random_swap";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Random Swap", NamedTextColor.LIGHT_PURPLE);
    }

    @Override
    public Component getDescription() {
        return Component.text("Swap positions with a random hider.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.WARPED_FUNGUS_ON_A_STICK;
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
        return 250;
    }

    @Override
    public void onPurchase(Player seeker, HideAndSeek plugin) {
        boolean excludeHidden = plugin.getSettingRegistry().get("perks.perk.seeker_random_swap.exclude-hidden", true);
        int blindnessTicks = plugin.getSettingRegistry().get("perks.perk.seeker_random_swap.blindness-ticks", 40);

        List<Player> candidates = new ArrayList<>();
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            if (excludeHidden && HideAndSeek.getDataController().isHidden(hiderId)) {
                continue;
            }
            candidates.add(hider);
        }

        if (candidates.isEmpty()) {
            seeker.sendMessage(Component.text("No valid hider to swap with.", NamedTextColor.RED));
            return;
        }

        Player target = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        var seekerLoc = seeker.getLocation().clone();
        var hiderLoc = target.getLocation().clone();
        seeker.teleport(hiderLoc);
        target.teleport(seekerLoc);

        seeker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0, false, false, false));
        target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0, false, false, false));
    }
}

