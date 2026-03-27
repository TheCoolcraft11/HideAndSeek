package de.thecoolcraft11.hideAndSeek.model;

import org.bukkit.boss.BarColor;

public enum SeekingBossBarStaticColor {
    PINK(BarColor.PINK),
    BLUE(BarColor.BLUE),
    RED(BarColor.RED),
    GREEN(BarColor.GREEN),
    YELLOW(BarColor.YELLOW),
    PURPLE(BarColor.PURPLE),
    WHITE(BarColor.WHITE);

    private final BarColor barColor;

    SeekingBossBarStaticColor(BarColor barColor) {
        this.barColor = barColor;
    }

    public BarColor getBarColor() {
        return barColor;
    }
}

