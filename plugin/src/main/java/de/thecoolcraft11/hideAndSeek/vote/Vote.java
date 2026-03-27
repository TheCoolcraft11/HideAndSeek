package de.thecoolcraft11.hideAndSeek.vote;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Vote<T> {
    private final ConcurrentMap<UUID, T> votes = new ConcurrentHashMap<>();

    public void castVote(UUID playerId, T value) {
        if (playerId == null || value == null) {
            return;
        }
        votes.put(playerId, value);
    }

    public Optional<T> getVote(UUID playerId) {
        return Optional.ofNullable(votes.get(playerId));
    }

    public boolean hasVote(UUID playerId) {
        return votes.containsKey(playerId);
    }

    public void clearVote(UUID playerId) {
        if (playerId == null) {
            return;
        }
        votes.remove(playerId);
    }

    public void reset() {
        votes.clear();
    }

    public Map<T, Long> countVotes(Set<UUID> eligibleVoters) {
        Map<T, Long> counts = new HashMap<>();
        for (Map.Entry<UUID, T> entry : votes.entrySet()) {
            if (eligibleVoters.contains(entry.getKey())) {
                counts.merge(entry.getValue(), 1L, Long::sum);
            }
        }
        return counts;
    }

    public Map<T, Long> countVotes(Set<UUID> eligibleVoters, Collection<T> allowedValues) {
        Map<T, Long> counts = new HashMap<>();
        Set<T> allowedSet = new HashSet<>(allowedValues);

        for (Map.Entry<UUID, T> entry : votes.entrySet()) {
            if (eligibleVoters.contains(entry.getKey()) && allowedSet.contains(entry.getValue())) {
                counts.merge(entry.getValue(), 1L, Long::sum);
            }
        }
        return counts;
    }

    public Set<Map.Entry<UUID, T>> getAllVotes() {
        return votes.entrySet();
    }

    public boolean isEmpty() {
        return votes.isEmpty();
    }

    public int size() {
        return votes.size();
    }
}

