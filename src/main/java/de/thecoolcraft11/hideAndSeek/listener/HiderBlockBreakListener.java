package de.thecoolcraft11.hideAndSeek.listener;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class HiderBlockBreakListener implements Listener {
    private final HideAndSeek plugin;

    public HiderBlockBreakListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();


        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return;
        }

        if (!plugin.getStateManager().areBlockRestrictionsActiveForPlayer(player)) return;


        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        if (!(currentPhase.equals("hiding") || currentPhase.equals("seeking"))) {
            return;
        }


        event.setDropItems(false);
        event.setExpToDrop(0);
    }
}


