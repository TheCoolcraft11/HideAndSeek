package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderAdaptiveSpeedSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.hider_adaptive_speed.enabled", true, "Enable Adaptive Speed",
                        Material.SUGAR, true),
                new IntegerSettingSpec("perks.perk.hider_adaptive_speed.cost", 80, 0, 10000, "Cost of Adaptive Speed",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.hider_adaptive_speed.cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Adaptive Speed", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.hider_adaptive_speed.hp-threshold", 0.5d, 0.0d, 1.0d,
                        "Health threshold for Adaptive Speed", Material.GOLDEN_APPLE),
                new IntegerSettingSpec("perks.perk.hider_adaptive_speed.speed-duration-ticks", 100, 1, 2000,
                        "Speed duration for Adaptive Speed", Material.SUGAR),
                new LongSettingSpec("perks.perk.hider_adaptive_speed.trigger-cooldown-ticks", 300L, 0L, 20000L,
                        "Trigger cooldown for Adaptive Speed", Material.CLOCK));
    }
}
