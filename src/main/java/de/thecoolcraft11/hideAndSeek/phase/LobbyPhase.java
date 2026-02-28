package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

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


        for (Player player : Bukkit.getOnlinePlayers()) {
            player.setGameMode(GameMode.SURVIVAL);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.getInventory().clear();

            Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(1.0);

            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        }

        plugin.getLogger().info("Lobby phase started. Waiting for teams to be set up...");

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Team dummyTeam = scoreboardManager.getMainScoreboard().getTeam("hiders_display");
        if (dummyTeam != null) dummyTeam.unregister();
    }

    @Override
    public void onEnd(MinigameFramework plugin) {

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

    private void assignRoles(MinigameFramework plugin) {
        List<Team> teams = new ArrayList<>(plugin.getTeamManager().getAllTeams());

        if (teams.size() != 2) {
            plugin.getLogger().warning("Hide and Seek requires exactly 2 teams!");
            return;
        }

        Team hidersTeam;
        Team seekersTeam;


        var fixedSeekerTeamResult = plugin.getSettingService().getSetting("game.fixed_seeker_team");
        String fixedSeekerTeamName = (fixedSeekerTeamResult.isSuccess() && fixedSeekerTeamResult.getValue() instanceof String) ?
                (String) fixedSeekerTeamResult.getValue() : "";

        boolean useFixedSeekerTeam = fixedSeekerTeamName != null && !fixedSeekerTeamName.trim().isEmpty();

        if (useFixedSeekerTeam) {

            seekersTeam = plugin.getTeamManager().getTeam(fixedSeekerTeamName);
            if (seekersTeam == null) {
                plugin.getLogger().warning("Fixed seeker team '" + fixedSeekerTeamName + "' not found! Falling back to random.");
                useFixedSeekerTeam = false;
            } else {
                hidersTeam = teams.get((teams.indexOf(seekersTeam) + 1) % 2);
                plugin.getLogger().info("Using fixed seeker team: " + seekersTeam.getName());


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

            var randomDistResult = plugin.getSettingService().getSetting("game.random_team_distribution");
            Object randomDistObj = randomDistResult.isSuccess() ? randomDistResult.getValue() : true;
            boolean randomDistribution = (randomDistObj instanceof Boolean) ? (Boolean) randomDistObj : true;

            if (randomDistribution) {
                var seekerCountResult = plugin.getSettingService().getSetting("game.seeker_count");
                Object seekerCountObj = seekerCountResult.isSuccess() ? seekerCountResult.getValue() : 1;
                int seekerCount = (seekerCountObj instanceof Integer) ? (Integer) seekerCountObj : 1;

                Random random = new Random();
                hidersTeam = teams.get(random.nextInt(2));
                seekersTeam = teams.get((teams.indexOf(hidersTeam) + 1) % 2);

                List<Player> allPlayers = new ArrayList<>();
                for (Team team : teams) {
                    allPlayers.addAll(plugin.getTeamManager().getPlayersInTeam(team));
                }

                java.util.Collections.shuffle(allPlayers);

                int seekersToAssign = Math.min(seekerCount, allPlayers.size() - 1);

                for (int i = 0; i < allPlayers.size(); i++) {
                    Player player = allPlayers.get(i);
                    if (i < seekersToAssign) {
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
                Random random = new Random();
                hidersTeam = teams.get(random.nextInt(2));
                seekersTeam = teams.get((teams.indexOf(hidersTeam) + 1) % 2);

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
}
