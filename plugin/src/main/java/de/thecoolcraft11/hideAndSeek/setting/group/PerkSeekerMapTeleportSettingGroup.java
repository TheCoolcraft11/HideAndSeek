package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerMapTeleportSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec(
                        "perks.perk.seeker_map_teleport.enabled",
                        true,
                        "Enable Map Teleport",
                        Material.COMPASS,
                        true
                ),
                new IntegerSettingSpec(
                        "perks.perk.seeker_map_teleport.cost",
                        150,
                        0,
                        10000,
                        "Cost of Map Teleport",
                        Material.EMERALD
                ),
                new LongSettingSpec(
                        "perks.perk.seeker_map_teleport.purchase-cooldown-ticks",
                        600L,
                        0L,
                        20000L,
                        "Purchase cooldown for Map Teleport",
                        Material.CLOCK
                ),
                new DoubleSettingSpec(
                        "perks.perk.seeker_map_teleport.min-distance-from-hider",
                        5.0d,
                        0.0d,
                        200.0d,
                        "Minimum distance from hiders for Map Teleport",
                        Material.COMPASS
                ),
                new IntegerSettingSpec(
                        "perks.perk.seeker_map_teleport.blindness-ticks",
                        40,
                        0,
                        2000,
                        "Blindness duration for Map Teleport",
                        Material.INK_SAC
                ),
                new BooleanSettingSpec(
                        "perks.perk.seeker_map_teleport.finite",
                        false,
                        "Whether Map Teleport is finite",
                        Material.BARRIER,
                        true
                )
        );
    }
}

