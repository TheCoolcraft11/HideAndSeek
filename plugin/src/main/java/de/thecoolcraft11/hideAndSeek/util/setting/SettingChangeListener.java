package de.thecoolcraft11.hideAndSeek.util.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;

public class SettingChangeListener {
    private final HideAndSeek plugin;

    public SettingChangeListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public static void register(HideAndSeek plugin) {
        SettingChangeListener listener = new SettingChangeListener(plugin);

        SeekerItems.getAllConfigKeys().forEach(key ->
                plugin.getSettingRegistry().onSettingChange(key, listener::onSettingChange));

        HiderItems.getAllConfigKeys().forEach(key ->
                plugin.getSettingRegistry().onSettingChange(key, listener::onSettingChange));


        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.enabled", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.name-layout", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.progress-mode", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.color.mode", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.color.static-color", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.animation.enabled", listener::onSettingChange);
        plugin.getSettingRegistry().onSettingChange("game.seeking-bossbar.animation.speed-ticks", listener::onSettingChange);
    }

    public void onSettingChange(String key, Object oldValue, Object newValue) {
        SeekerItems.reregisterSpecificItem(key, plugin);
        HiderItems.reregisterSpecificItem(key, plugin);

        if (key.startsWith("game.seeking-bossbar.")) {
            plugin.getSeekingBossBarService().reloadSettings();
        }
    }
}
