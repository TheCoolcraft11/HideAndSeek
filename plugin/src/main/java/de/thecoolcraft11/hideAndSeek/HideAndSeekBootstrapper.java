package de.thecoolcraft11.hideAndSeek;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.net.URI;
import java.util.Objects;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class HideAndSeekBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {

        File configFile = new File(context.getDataDirectory().toFile(), "config.yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        if (!config.getBoolean("inject-datapack", false)) {
            return;
        }

        var manager = context.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {

            var registrar = event.registrar();

            try {
                URI uri = Objects.requireNonNull(
                        getClass().getResource("/datapack")
                ).toURI();

                registrar.discoverPack(
                        context.getPluginMeta(),
                        uri,
                        "datapack",
                        datapackConfig -> {
                            datapackConfig.autoEnableOnServerStart(true);
                            datapackConfig.title(Component.text("HideAndSeek Internal Datapack"));
                        }
                );

            } catch (Exception e) {
                throw new RuntimeException("Failed to load embedded datapack", e);
            }
        });
    }
}