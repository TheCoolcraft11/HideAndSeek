package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderShadowStepSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.hider_shadow_step.enabled", true, "Enable Shadow Step",
                        Material.ENDER_EYE, true),
                new IntegerSettingSpec("perks.perk.hider_shadow_step.cost", 100, 0, 10000, "Cost of Shadow Step",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.hider_shadow_step.cooldown-ticks", 0L, 0L, 20000L,
                        "Purchase cooldown for Shadow Step", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.hider_shadow_step.hp-trigger", 5.0d, 0.0d, 40.0d,
                        "Health threshold that triggers Shadow Step", Material.HEART_OF_THE_SEA),
                new LongSettingSpec("perks.perk.hider_shadow_step.charge-ticks", 30L, 1L, 600L,
                        "Charge time before teleport", Material.CLOCK),
                new DoubleSettingSpec("perks.perk.hider_shadow_step.teleport-range", 15.0d, 1.0d, 100.0d,
                        "Teleport search range", Material.ENDER_PEARL),
                new DoubleSettingSpec("perks.perk.hider_shadow_step.min-seeker-distance", 5.0d, 0.0d, 100.0d,
                        "Minimum distance from seekers for teleport target", Material.COMPASS));
    }
}
