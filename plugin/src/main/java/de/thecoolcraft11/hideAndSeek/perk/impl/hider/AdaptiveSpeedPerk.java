package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class AdaptiveSpeedPerk extends BasePerk {
    @Override
    public String getId() {
        return "hider_adaptive_speed";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Adaptive Speed", NamedTextColor.YELLOW);
    }

    @Override
    public Component getDescription() {
        return Component.text("Gain Speed when low HP.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.SUGAR;
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
        long cooldown = plugin.getSettingRegistry().get("perks.perk.hider_adaptive_speed.cooldown-ticks", 300L);
        double threshold = plugin.getSettingRegistry().get("perks.perk.hider_adaptive_speed.hp-threshold", 0.5d);
        int duration = plugin.getSettingRegistry().get("perks.perk.hider_adaptive_speed.speed-duration-ticks", 100);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            long now = System.currentTimeMillis();
            long last = plugin.getPerkStateManager().lastTriggerTime.getOrDefault(player.getUniqueId(), 0L);
            if (now - last < cooldown * 50L) {
                return;
            }

            double maxHp = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH) != null
                    ? player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() : 20.0;

            if (player.getHealth() <= maxHp * threshold) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 0, false, false, true));
                plugin.getPerkStateManager().lastTriggerTime.put(player.getUniqueId(), now);
            }
        }, 0L, 5L);

        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
        player.removePotionEffect(PotionEffectType.SPEED);
    }
}

