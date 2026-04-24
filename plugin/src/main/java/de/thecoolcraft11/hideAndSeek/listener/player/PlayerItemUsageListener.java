package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerItemUsageListener implements Listener {

    private final HideAndSeek plugin;

    public PlayerItemUsageListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        switch (event.getAction()) {
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
            }
            default -> {
                return;
            }
        }

        Player player = event.getPlayer();
        if (!isActiveParticipant(player)) {
            return;
        }

        String itemId = resolveItemId(player);
        if (itemId == null) {
            return;
        }

        var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
        if (statsService != null) {
            statsService.recordItemUsed(player.getUniqueId(), itemId);
        }
    }

    private boolean isActiveParticipant(Player player) {
        String phase = plugin.getStateManager().getCurrentPhaseId();
        if (!"hiding".equals(phase) && !"seeking".equals(phase)) {
            return false;
        }
        return HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())
                || HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId());
    }

    private String resolveItemId(Player player) {
        for (String id : HiderItems.getAllItemIds()) {
            if (plugin.getCustomItemManager().hasItemInMainHand(player, id)) {
                return id;
            }
        }

        for (String id : SeekerItems.getAllItemIds()) {
            if (plugin.getCustomItemManager().hasItemInMainHand(player, id)) {
                return id;
            }
        }

        return null;
    }
}



