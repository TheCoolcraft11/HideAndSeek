package de.thecoolcraft11.hideAndSeek.vote;

import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;

public record VotingResult(GameModeEnum winningGamemode, String winningMap, boolean hasAnyVotes) {
}

