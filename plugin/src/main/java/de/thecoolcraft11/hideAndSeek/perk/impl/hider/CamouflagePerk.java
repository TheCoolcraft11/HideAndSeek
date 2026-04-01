package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Set;

public class CamouflagePerk extends BasePerk {

    private static final Set<PotionEffectType> WHITELIST = Set.of(
            PotionEffectType.SATURATION,
            PotionEffectType.WATER_BREATHING,
            PotionEffectType.NIGHT_VISION,
            PotionEffectType.INVISIBILITY
    );

    @Override
    public String getId() {
        return "hider_camouflage";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Camouflage", NamedTextColor.GREEN);
    }

    @Override
    public Component getDescription() {
        return Component.text("Cleanses negative effects repeatedly.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.FERN;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.UNCOMMON;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 80;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        cleanse(player);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.5f);
        player.showTitle(Title.title(Component.text("Camouflage active", NamedTextColor.GREEN),
                Component.text("Effects cleansed", NamedTextColor.DARK_GREEN),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1200), Duration.ofMillis(200))));

        long interval = plugin.getSettingRegistry().get("perks.perk.hider_camouflage.re-cleanse-interval-ticks", 100L);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> cleanse(player), interval, interval);
        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
    }

    private void cleanse(Player hider) {
        if (hider == null || !hider.isOnline()) {
            return;
        }
        for (PotionEffect effect : new ArrayList<>(hider.getActivePotionEffects())) {
            if (!WHITELIST.contains(effect.getType())) {
                hider.removePotionEffect(effect.getType());
            }
        }
        hider.setGlowing(false);
    }
}

