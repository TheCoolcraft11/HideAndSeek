package de.thecoolcraft11.hideAndSeek.perk.impl.hider;

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
import org.bukkit.scheduler.BukkitTask;

public class DoubleJumpPerk extends BasePerk {
    @Override
    public String getId() {
        return "hider_double_jump";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Double Jump", NamedTextColor.AQUA);
    }

    @Override
    public Component getDescription() {
        return Component.text("Press jump mid-air for a second leap.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.FEATHER;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.RARE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.HIDER;
    }

    @Override
    public int getCost() {
        return 120;
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(true);
        }

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || player.getGameMode() != GameMode.SURVIVAL) {
                return;
            }
            if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
                player.setAllowFlight(false);
                return;
            }
            if (player.isOnGround()) {
                player.setAllowFlight(true);
                player.setFlying(false);
            }
        }, 0L, 5L);

        plugin.getPerkStateManager().storeTask(player, getId(), task);
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        plugin.getPerkStateManager().cancelTask(player, getId());
        if (player.getGameMode() == GameMode.SURVIVAL) {
            player.setAllowFlight(false);
        }
    }
}

