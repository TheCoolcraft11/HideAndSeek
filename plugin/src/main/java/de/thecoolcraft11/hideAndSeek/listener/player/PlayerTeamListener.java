package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Team;

public class PlayerTeamListener implements Listener {

    private final HideAndSeek plugin;

    public PlayerTeamListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer().getScoreboard().getEntryTeam(event.getPlayer().getName()) == null) {
            var hiderTeamResult = plugin.getSettingService().getSetting("game.teams.fixed-hider-team");
            Object hiderTeamObj = hiderTeamResult.isSuccess() ? hiderTeamResult.getValue() : "Hiders";
            String hiderTeam = hiderTeamObj.toString();

            Team team = event.getPlayer().getScoreboard().getTeam(hiderTeam);
            if (team != null) {
                team.addEntry(event.getPlayer().getName());
            } else {
                plugin.getLogger().warning("The team '" + hiderTeam + "' does not exist. Please check your configuration.");
            }
        }
    }

}
