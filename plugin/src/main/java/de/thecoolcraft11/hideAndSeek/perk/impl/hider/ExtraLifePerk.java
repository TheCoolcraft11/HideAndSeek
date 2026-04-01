package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExtraLifePerk extends BasePerk {

    private static final Map<UUID, Integer> baselinePoints = new ConcurrentHashMap<>();

    @Override
    public String getId() {
        return "hider_extra_life";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Extra Life", NamedTextColor.LIGHT_PURPLE);
    }

    @Override
    public Component getDescription() {
        return Component.text("Earn absorption hearts from points.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.GOLDEN_APPLE;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.EPIC;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 200;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        UUID playerId = player.getUniqueId();
        baselinePoints.put(playerId, HideAndSeek.getDataController().getPoints(playerId));

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int pointsPerHeart = plugin.getSettingRegistry().get("perks.perk.hider_extra_life.points-per-heart", 100);
            int maxHearts = plugin.getSettingRegistry().get("perks.perk.hider_extra_life.max-hearts", 5);
            int earned = HideAndSeek.getDataController().getPoints(playerId) - baselinePoints.getOrDefault(playerId, 0);
            int hearts = Math.max(0, Math.min(maxHearts, earned / Math.max(1, pointsPerHeart)));
            if (plugin.getPerkStateManager().absorptionHearts.getOrDefault(playerId, 0) == hearts) {
                return;
            }

            AttributeInstance attr = player.getAttribute(Attribute.MAX_ABSORPTION);
            if (attr != null) {
                attr.setBaseValue(maxHearts * 2.0d);
            }
            player.setAbsorptionAmount(hearts * 2.0d);
            plugin.getPerkStateManager().absorptionHearts.put(playerId, hearts);
        }, 0L, 30L);

        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
        baselinePoints.remove(player.getUniqueId());
        player.setAbsorptionAmount(0);
    }
}

