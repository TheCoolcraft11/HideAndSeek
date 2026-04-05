package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.seeker.CameraItem;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.PlayerStateResetUtil;
import de.thecoolcraft11.hideAndSeek.util.map.MapConfigHelper;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import de.thecoolcraft11.hideAndSeek.vote.PreferredRole;
import de.thecoolcraft11.hideAndSeek.vote.VotingResult;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class LobbyPhase implements GamePhase {
    @Override
    public String getId() {
        return "lobby";
    }

    @Override
    public String getDisplayName() {
        return "Lobby";
    }

    @Override
    public void onStart(MinigameFramework plugin) {

        HideAndSeek.getDataController().reset();
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        CameraItem.clearAllCameraState(hideAndSeekPlugin);
        hideAndSeekPlugin.getMapManager().clearAppliedSettingOverrides();
        hideAndSeekPlugin.getVoteManager().resetVotes();


        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStateResetUtil.resetPlayerCompletely(player, true);
            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        }

        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Lobby phase started. Waiting for teams to be set up...");
        }

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Team dummyTeam = scoreboardManager.getMainScoreboard().getTeam("hiders_display");
        if (dummyTeam != null) dummyTeam.unregister();
    }

    @Override
    public void onEnd(MinigameFramework plugin) {
        applyVotingOutcome((HideAndSeek) plugin);

        assignRoles(plugin);


        Component message = Component.text("Hide and Seek is starting!", NamedTextColor.GOLD);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(message);
        }
    }

    @Override
    public List<String> getAllowedTransitions() {
        return List.of("hiding");
    }

    private void applyVotingOutcome(HideAndSeek plugin) {
        VotingResult result = plugin.getVoteManager().resolveVotingResult(plugin.getVoteManager().getOnlineVoterIds());

        if (!result.hasAnyVotes()) {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("No votes submitted in lobby; using default map/gamemode behavior.");
            }
            return;
        }

        if (result.winningGamemode() != null) {
            GameModeEnum winningMode = result.winningGamemode();
            var setResult = plugin.getSettingService().setSetting("game.mode", winningMode.name());
            if (setResult.isSuccess()) {
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Applied voted gamemode: " + winningMode.name());
                }
            } else {
                plugin.getLogger().warning("Failed to apply voted gamemode " + winningMode.name() + ": " + setResult.getErrorMessage());
            }
        }

        if (HideAndSeek.getDataController().isMapSelectionLocked()) {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Skipped voted map because map selection is locked to: " + HideAndSeek.getDataController().getCurrentMapName());
            }
            return;
        }

        if (result.winningMap() != null && !result.winningMap().isBlank()) {
            HideAndSeek.getDataController().setCurrentMapName(result.winningMap());
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Applied voted map: " + result.winningMap());
            }
        }
    }

    private void assignRoles(MinigameFramework plugin) {
        List<Team> teams = new ArrayList<>(plugin.getTeamManager().getAllTeams());

        if (teams.size() != 2) {
            plugin.getLogger().warning("Hide and Seek requires exactly 2 teams!");
            return;
        }

        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMapName == null || currentMapName.isEmpty()) {
            String randomMapName = ((HideAndSeek) plugin).getMapManager().selectRandomMapName(Bukkit.getOnlinePlayers().size());
            if (randomMapName != null && !randomMapName.isEmpty()) {
                HideAndSeek.getDataController().setCurrentMapName(randomMapName);
            }
        }

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;

        Team hidersTeam;
        Team seekersTeam;


        var randomDistResult = plugin.getSettingService().getSetting("game.team-distribution.random");
        Object randomDistObj = randomDistResult.isSuccess() ? randomDistResult.getValue() : true;
        boolean randomDistribution = (randomDistObj instanceof Boolean) ? (Boolean) randomDistObj : true;

        if (randomDistribution) {

            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Random team distribution enabled");
            }


            currentMapName = HideAndSeek.getDataController().getCurrentMapName();
            MapData currentMapData = null;
            if (currentMapName != null && !currentMapName.isEmpty()) {
                currentMapData = hideAndSeekPlugin.getMapManager().getMapData(currentMapName);
            }


            List<Player> allPlayers = new ArrayList<>();
            for (Team team : teams) {
                allPlayers.addAll(plugin.getTeamManager().getPlayersInTeam(team));
            }


            int seekerCount = MapConfigHelper.calculateSeekerCount(
                    plugin,
                    allPlayers.size(),
                    currentMapData
            );

            Random random = new Random();
            hidersTeam = teams.get(random.nextInt(2));
            seekersTeam = teams.get((teams.indexOf(hidersTeam) + 1) % 2);

            java.util.Collections.shuffle(allPlayers);

            int seekersToAssign = Math.clamp(seekerCount, 0, Math.max(0, allPlayers.size() - 1));

            Set<UUID> selectedSeekerIds = hideAndSeekPlugin.getVoteManager().isRolePreferenceVotingEnabled()
                    ? selectSeekersByPreference(allPlayers, seekersToAssign, hideAndSeekPlugin)
                    : selectFirstPlayersAsSeekers(allPlayers, seekersToAssign);

            for (Player player : allPlayers) {
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Removing player " + player.getName() + " from team");
                }
                plugin.getTeamManager().removePlayerFromTeam(player);

                if (selectedSeekerIds.contains(player.getUniqueId())) {

                    plugin.getTeamManager().addPlayerToTeam(player, seekersTeam.getName());
                    HideAndSeek.getDataController().addSeeker(player.getUniqueId());
                    plugin.getTeamManager().addRole(player, "seeker");

                    Title title = Title.title(
                            Component.text("You are a SEEKER!", NamedTextColor.RED),
                            Component.text("Find and tag the hiders!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    );
                    player.showTitle(title);
                } else {

                    plugin.getTeamManager().addPlayerToTeam(player, hidersTeam.getName());
                    HideAndSeek.getDataController().addHider(player.getUniqueId());
                    plugin.getTeamManager().addRole(player, "hider");

                    Title title = Title.title(
                            Component.text("You are a HIDER!", NamedTextColor.GREEN),
                            Component.text("Hide from the seekers!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    );
                    player.showTitle(title);
                }
            }
        } else {

            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Fixed team distribution enabled");
            }

            var fixedSeekerTeamResult = plugin.getSettingService().getSetting("game.teams.fixed-seeker-team");
            String fixedSeekerTeamName = (fixedSeekerTeamResult.isSuccess() && fixedSeekerTeamResult.getValue() instanceof String) ?
                    (String) fixedSeekerTeamResult.getValue() : "";

            boolean useFixedSeekerTeam = fixedSeekerTeamName != null && !fixedSeekerTeamName.trim().isEmpty();

            if (useFixedSeekerTeam) {

                seekersTeam = plugin.getTeamManager().getTeam(fixedSeekerTeamName);
                if (seekersTeam == null) {
                    plugin.getLogger().warning("Fixed seeker team '" + fixedSeekerTeamName + "' not found! Using team-based assignment.");
                    useFixedSeekerTeam = false;
                } else {
                    hidersTeam = teams.get((teams.indexOf(seekersTeam) + 1) % 2);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Using fixed seeker team: " + seekersTeam.getName());
                    }

                    for (Player player : plugin.getTeamManager().getPlayersInTeam(seekersTeam)) {
                        HideAndSeek.getDataController().addSeeker(player.getUniqueId());
                        plugin.getTeamManager().addRole(player, "seeker");
                        Title title = Title.title(
                                Component.text("You are a SEEKER!", NamedTextColor.RED),
                                Component.text("Find and tag the hiders!", NamedTextColor.YELLOW),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                        );
                        player.showTitle(title);
                    }

                    for (Player player : plugin.getTeamManager().getPlayersInTeam(hidersTeam)) {
                        HideAndSeek.getDataController().addHider(player.getUniqueId());
                        plugin.getTeamManager().addRole(player, "hider");
                        Title title = Title.title(
                                Component.text("You are a HIDER!", NamedTextColor.GREEN),
                                Component.text("Hide from the seekers!", NamedTextColor.YELLOW),
                                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                        );
                        player.showTitle(title);
                    }
                }
            }

            if (!useFixedSeekerTeam) {

                Random random = new Random();
                hidersTeam = teams.get(random.nextInt(2));
                seekersTeam = teams.get((teams.indexOf(hidersTeam) + 1) % 2);


                for (Team team : teams) {
                    for (Player player : plugin.getTeamManager().getPlayersInTeam(team)) {
                        plugin.getTeamManager().removeRole(player, "hider");
                        plugin.getTeamManager().removeRole(player, "seeker");
                    }
                }

                for (Player player : plugin.getTeamManager().getPlayersInTeam(hidersTeam)) {
                    HideAndSeek.getDataController().addHider(player.getUniqueId());
                    plugin.getTeamManager().addRole(player, "hider");

                    Title title = Title.title(
                            Component.text("You are a HIDER!", NamedTextColor.GREEN),
                            Component.text("Hide from the seekers!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    );
                    player.showTitle(title);
                }

                for (Player player : plugin.getTeamManager().getPlayersInTeam(seekersTeam)) {
                    HideAndSeek.getDataController().addSeeker(player.getUniqueId());
                    plugin.getTeamManager().addRole(player, "seeker");

                    Title title = Title.title(
                            Component.text("You are a SEEKER!", NamedTextColor.RED),
                            Component.text("Find and tag the hiders!", NamedTextColor.YELLOW),
                            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
                    );
                    player.showTitle(title);
                }
            }
        }
    }

    private Set<UUID> selectFirstPlayersAsSeekers(List<Player> allPlayers, int seekersToAssign) {
        Set<UUID> selectedSeekerIds = new HashSet<>();
        for (int i = 0; i < seekersToAssign && i < allPlayers.size(); i++) {
            selectedSeekerIds.add(allPlayers.get(i).getUniqueId());
        }
        return selectedSeekerIds;
    }

    private Set<UUID> selectSeekersByPreference(List<Player> allPlayers, int seekersToAssign, HideAndSeek hideAndSeekPlugin) {
        Set<UUID> selectedSeekerIds = new HashSet<>();
        if (seekersToAssign <= 0 || allPlayers.isEmpty()) {
            return selectedSeekerIds;
        }

        List<Player> remaining = new ArrayList<>(allPlayers);
        for (int i = 0; i < seekersToAssign && !remaining.isEmpty(); i++) {
            Player chosen = pickWeightedPlayer(remaining, hideAndSeekPlugin);
            selectedSeekerIds.add(chosen.getUniqueId());
            remaining.remove(chosen);
        }
        return selectedSeekerIds;
    }

    private Player pickWeightedPlayer(List<Player> candidates, HideAndSeek hideAndSeekPlugin) {
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }

        double totalWeight = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            weights[i] = getWeightForPreferredRole(candidates.get(i), hideAndSeekPlugin);
            totalWeight += weights[i];
        }

        Random random = new Random();
        if (totalWeight <= 0.0) {
            return candidates.get(random.nextInt(candidates.size()));
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (roll < cumulative) {
                return candidates.get(i);
            }
        }

        Player lastCandidate = null;
        for (Player candidate : candidates) {
            lastCandidate = candidate;
        }
        return lastCandidate;
    }

    private double getWeightForPreferredRole(Player player, HideAndSeek hideAndSeekPlugin) {
        PreferredRole preferredRole = hideAndSeekPlugin.getVoteManager().getPreferredRoleVote(player.getUniqueId()).orElse(null);
        if (preferredRole == PreferredRole.SEEKER) {
            return 4.0;
        }
        if (preferredRole == PreferredRole.HIDER) {
            return 0.35;
        }
        return 1.0;
    }


    @Override
    public boolean allowBlockBreak() {
        return false;
    }

    @Override
    public boolean allowBlockPlace() {
        return false;
    }

    @Override
    public boolean allowDamage() {
        return false;
    }

    @Override
    public boolean allowBlockInteraction() {
        return false;
    }

    @Override
    public boolean allowEntityInteraction() {
        return false;
    }

    @Override
    public boolean allowBlockDetection() {
        return false;
    }

    @Override
    public boolean allowEntityDetection() {
        return false;
    }

    @Override
    public boolean allowBlockPhysics() {
        return false;
    }

    @Override
    public boolean allowEntityChangeBlock() {
        return false;
    }

    @Override
    public boolean allowBlockExplosions() {
        return false;
    }

    @Override
    public boolean allowEntityExplosions() {
        return false;
    }

    @Override
    public boolean allowBlockDrops() {
        return false;
    }

    @Override
    public boolean allowBlockExperienceDrop() {
        return false;
    }

    @Override
    public boolean allowEntityDrops() {
        return false;
    }

    @Override
    public boolean allowEntityExperienceDrop() {
        return false;
    }

    @Override
    public boolean allowHunger() {
        return false;
    }

    @Override
    public boolean allowEntityPortals() {
        return false;
    }
}
