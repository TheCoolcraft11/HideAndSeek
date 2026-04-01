package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.AreaWarnHelper;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.*;

public class RelocatePerk extends BasePerk {

    private static final Map<UUID, List<AreaWarnHelper>> sessions = new HashMap<>();

    @Override
    public String getId() {
        return "seeker_relocate";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Relocate", NamedTextColor.RED);
    }

    @Override
    public Component getDescription() {
        return Component.text("Force hiders to move away from their current position.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.LEAD;
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
        return 280;
    }

    @Override
    public void onPurchase(Player seeker, HideAndSeek plugin) {
        int escapeSeconds = plugin.getSettingRegistry().get("perks.perk.seeker_relocate.escape-seconds", 60);
        double radius = plugin.getSettingRegistry().get("perks.perk.seeker_relocate.radius", 12.0d);

        List<AreaWarnHelper> helpers = sessions.computeIfAbsent(seeker.getUniqueId(), ignored -> new ArrayList<>());

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline()) {
                hider.sendMessage(Component.text("Relocate activated - move out of your marked zone!", NamedTextColor.RED));
            }
        }

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }

            Location anchor = hider.getLocation().clone();
            AreaWarnHelper helper = new AreaWarnHelper(plugin, anchor, radius, escapeSeconds * 20);
            helper.start(List.of(hiderId));
            helpers.add(helper);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                    helper.stop();
                    helpers.remove(helper);
                    return;
                }
                if (helper.isInsideZone(hider.getLocation())) {
                    plugin.getPlayerHitListener().markEnvironmentalDeath(hiderId, PlayerHitListener.EnvironmentalDeathCause.PERK_RELOCATE);
                    hider.setHealth(0.0);
                }
                helper.stop();
                helpers.remove(helper);
            }, escapeSeconds * 20L);
        }
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        List<AreaWarnHelper> helpers = sessions.remove(player.getUniqueId());
        if (helpers != null) {
            for (AreaWarnHelper helper : helpers) {
                helper.stop();
            }
        }
    }
}



