package de.thecoolcraft11.hideAndSeek.vote;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class VoteManager {
    private final HideAndSeek plugin;
    private final Random random = new Random();
    private final Object readinessLock = new Object();
    private final Vote<GameModeEnum> gamemodeVote = new Vote<>();
    private final Vote<String> mapVote = new Vote<>();
    private final ConcurrentMap<UUID, Boolean> readyStates = new ConcurrentHashMap<>();

    public VoteManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public boolean isGamemodeVotingEnabled() {
        var result = plugin.getSettingService().getSetting("game.vote_gamemode_enabled");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean isMapVotingEnabled() {
        var result = plugin.getSettingService().getSetting("game.vote_map_enabled");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean showVoteCounts() {
        var result = plugin.getSettingService().getSetting("game.vote_show_counts");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean isReadinessEnabled() {
        var result = plugin.getSettingService().getSetting("game.readiness_enabled");
        Object value = result.isSuccess() ? result.getValue() : true;
        return value instanceof Boolean enabled ? enabled : true;
    }

    public boolean isVotingEnabled() {
        return isGamemodeVotingEnabled() || isMapVotingEnabled();
    }

    public boolean isNotLobbyPhase() {
        return !"lobby".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId());
    }

    public Optional<GameModeEnum> getGamemodeVote(UUID playerId) {
        return gamemodeVote.getVote(playerId);
    }

    public Optional<String> getMapVote(UUID playerId) {
        return mapVote.getVote(playerId);
    }

    public boolean isReady(UUID playerId) {
        if (!isReadinessEnabled()) {
            return true;
        }
        return readyStates.getOrDefault(playerId, false);
    }

    public void setReady(UUID playerId, boolean ready) {
        if (playerId == null) {
            return;
        }
        if (!isReadinessEnabled()) {
            readyStates.remove(playerId);
            return;
        }
        readyStates.put(playerId, ready);
    }

    public boolean toggleReady(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (!isReadinessEnabled()) {
            return true;
        }
        return readyStates.compute(playerId, (id, current) -> current == null || !current);
    }

    public void clearReady(UUID playerId) {
        if (playerId == null) {
            return;
        }
        readyStates.remove(playerId);
    }

    public void resetReadiness() {
        readyStates.clear();
    }

    public void castGamemodeVote(UUID playerId, GameModeEnum mode) {
        if (playerId == null || mode == null) {
            return;
        }
        gamemodeVote.castVote(playerId, mode);
        if (isReadinessEnabled()) {
            setReady(playerId, false);
        }
    }

    public void castMapVote(UUID playerId, String mapName) {
        if (playerId == null || mapName == null || mapName.isBlank()) {
            return;
        }
        mapVote.castVote(playerId, mapName);
        if (isReadinessEnabled()) {
            setReady(playerId, false);
        }
    }

    public void clearVotes(UUID playerId) {
        if (playerId == null) {
            return;
        }
        gamemodeVote.clearVote(playerId);
        mapVote.clearVote(playerId);
        readyStates.remove(playerId);
    }

    public void resetVotes() {
        gamemodeVote.reset();
        mapVote.reset();
        readyStates.clear();
    }

    public Set<UUID> getOnlineVoterIds() {
        Set<UUID> onlineIds = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            onlineIds.add(player.getUniqueId());
        }
        return onlineIds;
    }

    public Set<UUID> getNotReadyOnlinePlayerIds() {
        if (!isReadinessEnabled()) {
            return Set.of();
        }
        Set<UUID> notReady = new HashSet<>();
        for (UUID playerId : getOnlineVoterIds()) {
            if (!isReady(playerId)) {
                notReady.add(playerId);
            }
        }
        return notReady;
    }

    public boolean areAllEligiblePlayersReady() {
        if (!isReadinessEnabled()) {
            return true;
        }
        Set<UUID> onlineIds = getOnlineVoterIds();
        if (onlineIds.isEmpty()) {
            return false;
        }
        for (UUID playerId : onlineIds) {
            if (!isReady(playerId)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasCompletedVote(UUID playerId) {
        if (playerId == null) {
            return false;
        }
        if (!isVotingEnabled()) {
            return true;
        }
        boolean hasGamemodeVote = !isGamemodeVotingEnabled() || gamemodeVote.hasVote(playerId);
        boolean hasMapVote = !isMapVotingEnabled() || mapVote.hasVote(playerId);
        return hasGamemodeVote && hasMapVote;
    }

    public boolean markReadyIfVoteComplete(UUID playerId) {
        if (!isReadinessEnabled()) {
            return false;
        }
        if (!hasCompletedVote(playerId)) {
            return false;
        }
        setReady(playerId, true);
        return true;
    }

    public boolean tryAutoStartIfEveryoneReady() {
        if (!isReadinessEnabled() || isNotLobbyPhase()) {
            return false;
        }
        synchronized (readinessLock) {
            if (!areAllEligiblePlayersReady()) {
                return false;
            }
            return plugin.getStateManager().setPhase("hiding", true);
        }
    }

    public Map<GameModeEnum, Long> countGamemodeVotes(Set<UUID> eligibleVoters) {
        return gamemodeVote.countVotes(eligibleVoters);
    }

    public Map<String, Long> countMapVotes(Set<UUID> eligibleVoters, Collection<String> eligibleMaps) {
        return mapVote.countVotes(eligibleVoters, eligibleMaps);
    }

    public VotingResult resolveVotingResult(Set<UUID> eligibleVoters) {
        boolean anyVotes = false;
        GameModeEnum winningGamemode = null;

        if (isGamemodeVotingEnabled()) {
            Map<GameModeEnum, Long> modeCounts = countGamemodeVotes(eligibleVoters);
            if (!modeCounts.isEmpty()) {
                anyVotes = true;
                winningGamemode = pickRandomTopEntry(modeCounts);
            }
        }

        String winningMap = null;
        if (isMapVotingEnabled()) {
            Collection<String> eligibleMaps;
            if (isGamemodeVotingEnabled()) {
                eligibleMaps = winningGamemode == null
                        ? List.of()
                        : plugin.getMapManager().getAvailableMapsForMode(winningGamemode);
            } else {
                eligibleMaps = plugin.getMapManager().getMapsForVoting();
            }

            if (!eligibleMaps.isEmpty()) {
                Map<String, Long> mapCounts = countMapVotes(eligibleVoters, eligibleMaps);
                if (!mapCounts.isEmpty()) {
                    anyVotes = true;
                    winningMap = pickRandomTopEntry(mapCounts);
                }
            }
        }

        return new VotingResult(winningGamemode, winningMap, anyVotes);
    }

    private <T> T pickRandomTopEntry(Map<T, Long> counts) {
        if (counts.isEmpty()) {
            return null;
        }

        long highestVotes = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<T> topEntries = counts.entrySet().stream()
                .filter(entry -> entry.getValue() == highestVotes)
                .map(Map.Entry::getKey)
                .toList();

        return topEntries.get(random.nextInt(topEntries.size()));
    }
}


