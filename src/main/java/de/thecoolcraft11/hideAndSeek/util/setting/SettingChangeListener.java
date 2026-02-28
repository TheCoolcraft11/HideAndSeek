package de.thecoolcraft11.hideAndSeek.util.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;

import java.util.Set;

public class SettingChangeListener {
    private static final Set<String> HIDER_COOLDOWN_KEYS = Set.of(
            "hider-items.sound.cooldown",
            "hider-items.explosion.cooldown",
            "hider-items.random-block.cooldown",
            "hider-items.speed-boost.cooldown",
            "hider-items.crossbow.cooldown",
            "hider-items.knockback-stick.cooldown",
            "hider-items.block-swap.cooldown",
            "hider-items.big-firecracker.cooldown",
            "hider-items.firework-rocket.cooldown",
            "hider-items.medkit.cooldown"
    );

    private static final Set<String> SEEKER_COOLDOWN_KEYS = Set.of(
            "seeker-items.grappling-hook.cooldown",
            "seeker-items.ink-splash.cooldown",
            "seeker-items.lightning-freeze.cooldown",
            "seeker-items.glowing-compass.cooldown",
            "seeker-items.curse-spell.cooldown",
            "seeker-items.block-randomizer.cooldown",
            "seeker-items.chain-pull.cooldown"
    );

    private static final Set<String> ALL_COOLDOWN_KEYS = Set.of(
            "hider-items.sound.cooldown",
            "hider-items.explosion.cooldown",
            "hider-items.random-block.cooldown",
            "hider-items.speed-boost.cooldown",
            "hider-items.crossbow.cooldown",
            "hider-items.knockback-stick.cooldown",
            "hider-items.block-swap.cooldown",
            "hider-items.big-firecracker.cooldown",
            "hider-items.firework-rocket.cooldown",
            "hider-items.medkit.cooldown",
            "seeker-items.grappling-hook.cooldown",
            "seeker-items.ink-splash.cooldown",
            "seeker-items.lightning-freeze.cooldown",
            "seeker-items.glowing-compass.cooldown",
            "seeker-items.curse-spell.cooldown",
            "seeker-items.block-randomizer.cooldown",
            "seeker-items.chain-pull.cooldown"
    );

    private final HideAndSeek plugin;

    public SettingChangeListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void onSettingChange(String key, Object oldValue, Object newValue) {
        if (HIDER_COOLDOWN_KEYS.contains(key)) {
            HiderItems.reregisterCooldownItems(plugin);
        }

        if (SEEKER_COOLDOWN_KEYS.contains(key)) {
            SeekerItems.reregisterCooldownItems(plugin);
        }
    }

    public static void register(HideAndSeek plugin) {
        SettingChangeListener listener = new SettingChangeListener(plugin);
        var registry = plugin.getSettingRegistry();

        for (String key : ALL_COOLDOWN_KEYS) {
            registry.onSettingChange(key, listener::onSettingChange);
        }
    }
}
