package de.thecoolcraft11.hideAndSeek;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class HideAndSeekBootstrapper implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {

        File configFile = new File(context.getDataDirectory().toFile(), "config.yml");

        boolean injectDatapack = loadInjectFlag(configFile);

        if (!injectDatapack) {
            return;
        }

        var manager = context.getLifecycleManager();

        manager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {

            var registrar = event.registrar();

            try {
                URL resource = getClass().getClassLoader().getResource("datapack");

                if (resource == null) {
                    throw new IllegalStateException("Datapack folder not found inside plugin jar");
                }

                URI uri = resource.toURI();

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

    private boolean loadInjectFlag(File configFile) {

        if (!configFile.exists()) {
            return false;
        }

        Yaml yaml = new Yaml();

        try (var reader = Files.newBufferedReader(configFile.toPath())) {

            Map<String, Object> config = yaml.load(reader);

            if (config == null) {
                return false;
            }

            Object value = config.get("inject-datapack");

            return Boolean.TRUE.equals(value);

        } catch (Exception e) {
            throw new RuntimeException("Failed to read config.yml in bootstrap", e);
        }
    }
}