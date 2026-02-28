package de.thecoolcraft11.hideAndSeek.util;


public enum SpeedBoostType {
    SPEED_EFFECT("Speed Effect", "Applies a speed potion effect"),
    VELOCITY_BOOST("Velocity Boost", "Gives a forward velocity boost");

    private final String displayName;
    private final String description;

    SpeedBoostType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static SpeedBoostType fromString(String value) {
        return switch (value.toLowerCase()) {
            case "velocity_boost", "velocity", "boost" -> VELOCITY_BOOST;
            default -> SPEED_EFFECT;
        };
    }

    @Override
    public String toString() {
        return name();
    }
}

