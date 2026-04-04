package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkHiderTrapSenseSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec(
                        "perks.perk.hider_trap_sense.enabled",
                        true,
                        "Enable Trap Sense",
                        Material.TRIPWIRE_HOOK,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.perk.hider_trap_sense.cost",
                        60,
                        0,
                        10000,
                        "Cost of Trap Sense",
                        Material.EMERALD
                ),
                new LongSettingSpec(
                        "perks.perk.hider_trap_sense.cooldown-ticks",
                        0L,
                        0L,
                        20000L,
                        "Purchase cooldown for Trap Sense",
                        Material.CLOCK
                ),
                new DoubleSettingSpec(
                        "perks.perk.hider_trap_sense.indicator-range",
                        30.0d,
                        0.0d,
                        200.0d,
                        "Indicator range for Trap Sense",
                        Material.REDSTONE
                ),
                new DoubleSettingSpec(
                        "perks.perk.hider_trap_sense.glow-range",
                        20.0d,
                        0.0d,
                        200.0d,
                        "Glow range for Trap Sense",
                        Material.GLOWSTONE
                ),
                new DoubleSettingSpec(
                        "perks.perk.hider_trap_sense.warn-range",
                        6.0d,
                        0.0d,
                        200.0d,
                        "Warning range for Trap Sense",
                        Material.BELL
                ),
                new LongSettingSpec(
                        "perks.perk.hider_trap_sense.warn-cooldown-ticks",
                        40L,
                        0L,
                        20000L,
                        "Warning cooldown for Trap Sense",
                        Material.CLOCK
                )
        );
    }
}

