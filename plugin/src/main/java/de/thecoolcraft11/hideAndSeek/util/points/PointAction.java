package de.thecoolcraft11.hideAndSeek.util.points;

public enum PointAction {
    HIDER_SURVIVAL_TICK("points.hider.survival.amount", 5),
    HIDER_PROXIMITY_BONUS("points.hider.proximity.amount-per-second", 2),
    HIDER_NEAR_MISS("points.hider.near-miss.amount", 50),
    HIDER_TAUNT_SMALL("points.hider.taunt.small", 25),
    HIDER_TAUNT_LARGE("points.hider.taunt.large", 75),
    HIDER_SHARPSHOOTER("points.hider.sharpshooter.amount", 20),
    HIDER_SURVIVOR("points.hider.survivor.amount", 100),
    HIDER_SPECIAL_GHOST("points.hider.special.ghost", 200),
    HIDER_SPECIAL_DISTRACTOR("points.hider.special.distractor", 200),

    SEEKER_ACTIVE_HUNTER("points.seeker.active-hunter.amount-per-second", 2),
    SEEKER_UTILITY_SUCCESS("points.seeker.utility-success.amount", 40),
    SEEKER_INTERCEPTION("points.seeker.interception.amount", 15),
    SEEKER_KILL("points.seeker.kill.amount", 300),
    SEEKER_ASSIST("points.seeker.assist.amount", 100),

    SEEKER_SPECIAL_BLOODHOUND("points.seeker.special.bloodhound", 200),
    SEEKER_FIRST_BLOOD("points.seeker.first-blood.amount", 100),
    SEEKER_ENVIRONMENTAL_ELIMINATION("points.seeker.environmental-elimination.amount", 50);

    private final String settingPath;
    private final int defaultPoints;

    PointAction(String settingPath, int defaultPoints) {
        this.settingPath = settingPath;
        this.defaultPoints = defaultPoints;
    }

    public String getSettingPath() {
        return settingPath;
    }

    public int getDefaultPoints() {
        return defaultPoints;
    }
}

