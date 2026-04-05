package de.thecoolcraft11.hideAndSeek.vote;

public enum PreferredRole {
    HIDER,
    SEEKER;

    public String displayName() {
        return this == HIDER ? "Hider" : "Seeker";
    }
}
