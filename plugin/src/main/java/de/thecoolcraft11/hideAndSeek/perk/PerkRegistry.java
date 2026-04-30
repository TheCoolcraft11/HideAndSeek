package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.impl.hider.*;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PerkRegistry {

    private final HideAndSeek plugin;
    private final List<PerkDefinition> allPerks = new ArrayList<>();
    private List<PerkDefinition> roundHiderPerks = List.of();
    private List<PerkDefinition> roundSeekerPerks = List.of();

    public PerkRegistry(HideAndSeek plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerAll() {
        allPerks.add(new AdaptiveSpeedPerk());
        allPerks.add(new SeekerWarningPerk());
        allPerks.add(new ExtraLifePerk());
        allPerks.add(new ShadowStepPerk());
        allPerks.add(new CamouflagePerk());
        allPerks.add(new DoubleJumpPerk());
        allPerks.add(new TrapSensePerk());

        allPerks.add(new DeathZonePerk());
        allPerks.add(new RandomSwapPerk());
        allPerks.add(new MapTeleportPerk());
        allPerks.add(new RelocatePerk());
        allPerks.add(new ElytraRushPerk());
        allPerks.add(new ProximityMeterPerk());
        allPerks.add(new ScentTrailPerk());
        allPerks.add(new AutoAimPerk());
        allPerks.add(new HitDisplayPerk());
    }

    public void selectRoundPerks() {
        int n = plugin.getSettingRegistry().get("perks.perks-per-round", 3);
        roundHiderPerks = selectRandom(PerkTarget.HIDER, n);
        roundSeekerPerks = selectRandom(PerkTarget.SEEKER, n);
    }

    public boolean isFiniteSeekerPerk(String perkId) {
        if (perkId == null || perkId.isBlank()) {
            return false;
        }
        return plugin.getSettingRegistry().get("perks.perk." + perkId + ".finite", false);
    }

    public int getFinitePlayerLimit() {
        return plugin.getSettingRegistry().get("perks.finite-player-limit", 1);
    }

    public int getFinitePlayerLimit(PerkTarget target) {
        if (target == PerkTarget.HIDER) {
            return plugin.getSettingRegistry().get("perks.hider-finite-player-limit", getFinitePlayerLimit());
        }
        if (target == PerkTarget.SEEKER) {
            return plugin.getSettingRegistry().get("perks.seeker-finite-player-limit", 0);
        }
        return 0;
    }

    public long getRebuyCooldownTicks(PerkDefinition perk) {
        if (perk == null || perk.getTarget() != PerkTarget.SEEKER || isFiniteSeekerPerk(perk.getId())) {
            return 0L;
        }

        return switch (perk.getId()) {
            case "seeker_death_zone" ->
                    plugin.getSettingRegistry().get("perks.perk.seeker_death_zone.purchase-cooldown-ticks", 1200L);
            case "seeker_random_swap" ->
                    plugin.getSettingRegistry().get("perks.perk.seeker_random_swap.purchase-cooldown-ticks", 200L);
            case "seeker_map_teleport" ->
                    plugin.getSettingRegistry().get("perks.perk.seeker_map_teleport.purchase-cooldown-ticks", 200L);
            case "seeker_relocate" ->
                    plugin.getSettingRegistry().get("perks.perk.seeker_relocate.purchase-cooldown-ticks", 1200L);
            case "seeker_elytra_rush" ->
                    plugin.getSettingRegistry().get("perks.perk.seeker_elytra_rush.purchase-cooldown-ticks", 600L);
            default -> 0L;
        };
    }

    private List<PerkDefinition> selectRandom(PerkTarget target, int count) {
        List<PerkDefinition> pool = allPerks.stream()
                .filter(p -> p.getTarget() == target)
                .filter(p -> plugin.getSettingRegistry().get("perks.perk." + p.getId() + ".enabled", true))
                .filter(p -> plugin.getLoadoutManager().isPerkAllowed(target, p.getId()))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public List<PerkDefinition> getRoundPerksForHider() {
        return roundHiderPerks;
    }

    public List<PerkDefinition> getRoundPerksForSeeker() {
        return roundSeekerPerks;
    }

    public List<PerkDefinition> getAllPerks() {
        return Collections.unmodifiableList(allPerks);
    }
}

