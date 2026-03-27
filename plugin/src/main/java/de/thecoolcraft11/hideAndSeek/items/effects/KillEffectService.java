package de.thecoolcraft11.hideAndSeek.items.effects;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class KillEffectService {

    private final HideAndSeek plugin;

    public KillEffectService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void triggerKillEffect(Player killer, Player victim, Location killLocation) {
        if (killer == null || victim == null || killLocation == null) {
            return;
        }


        String selectedKillEffectId = ItemSkinSelectionService.getSelectedVariant(killer, KillEffectSkins.LOGICAL_ITEM_ID);
        if (selectedKillEffectId == null || selectedKillEffectId.isBlank()) {
            return;
        }
        if (!ItemSkinSelectionService.isUnlocked(killer.getUniqueId(), KillEffectSkins.LOGICAL_ITEM_ID, selectedKillEffectId)) {
            return;
        }

        KillEffect killEffect = KillEffectManager.getKillEffect(selectedKillEffectId);

        if (killEffect == null) {
            return;
        }

        try {
            killEffect.execute(killer, victim, killLocation, plugin);
        } catch (Exception e) {
            plugin.getLogger().warning("Error executing kill effect: " + e.getMessage());
        }
    }
}
