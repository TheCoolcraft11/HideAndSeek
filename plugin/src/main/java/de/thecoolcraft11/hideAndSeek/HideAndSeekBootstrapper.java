package de.thecoolcraft11.hideAndSeek;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;

@SuppressWarnings({"UnstableApiUsage", "unused"})
public class HideAndSeekBootstrapper implements PluginBootstrap {

    private static FileSystem openFileSystem(URI uri) throws IOException {
        try {
            return FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
            return FileSystems.newFileSystem(uri, Map.<String, Object>of());
        }
    }

    @Override
    public void bootstrap(BootstrapContext context) {
        File configFile = new File(context.getDataDirectory().toFile(), "config.yml");
        Map<String, Object> config = loadConfig(configFile);
        if (config == null) return;

        boolean copyToDisk = Boolean.TRUE.equals(config.get("copy-datapack-to-disk"));
        boolean injectDatapack = Boolean.TRUE.equals(config.get("inject-datapack"));

        if (copyToDisk) {
            Path targetDir = context.getDataDirectory().resolve("datapack");
            if (Files.notExists(targetDir)) {
                extractDatapack(targetDir);
            }
            registerDatapackFromDisk(context, targetDir);
        } else if (injectDatapack) {
            registerDatapackFromJar(context);
        }
    }

    private void registerDatapackFromJar(BootstrapContext context) {
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

    private void registerDatapackFromDisk(BootstrapContext context, Path datapackDir) {
        var manager = context.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.DATAPACK_DISCOVERY, event -> {
            var registrar = event.registrar();
            try {
                URI uri = datapackDir.toUri();
                registrar.discoverPack(
                        context.getPluginMeta(),
                        uri,
                        "datapack",
                        datapackConfig -> {
                            datapackConfig.autoEnableOnServerStart(true);
                            datapackConfig.title(Component.text("HideAndSeek Datapack"));
                        }
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to load datapack from disk", e);
            }
        });
    }

    private void extractDatapack(Path targetDir) {
        try {
            URL resource = getClass().getClassLoader().getResource("datapack");
            if (resource == null) {
                throw new IllegalStateException("Datapack folder not found inside plugin jar");
            }
            URI uri = resource.toURI();

            if ("jar".equals(uri.getScheme())) {
                extractFromJar(uri, targetDir);
            } else if ("file".equals(uri.getScheme())) {
                extractFromDirectory(Path.of(uri), targetDir);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract datapack to disk", e);
        }
    }

    private void extractFromJar(URI jarUri, Path targetDir) throws Exception {
        FileSystem fs = openFileSystem(jarUri);
        try {
            Path source = fs.getPath("/datapack");
            try (Stream<Path> walk = Files.walk(source)) {
                walk.forEach(path -> {
                    try {
                        Path dest = targetDir.resolve(source.relativize(path).toString());
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(dest);
                        } else {
                            Files.createDirectories(dest.getParent());
                            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to copy datapack entry: " + path, e);
                    }
                });
            }
        } finally {
            fs.close();
        }
    }

    private void extractFromDirectory(Path sourceDir, Path targetDir) throws Exception {
        try (Stream<Path> walk = Files.walk(sourceDir)) {
            walk.forEach(path -> {
                try {
                    Path dest = targetDir.resolve(sourceDir.relativize(path).toString());
                    if (Files.isDirectory(path)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy datapack entry: " + path, e);
                }
            });
        }
    }

    private Map<String, Object> loadConfig(File configFile) {
        if (!configFile.exists()) return null;
        Yaml yaml = new Yaml();
        try (var reader = Files.newBufferedReader(configFile.toPath())) {
            return yaml.load(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read config.yml in bootstrap", e);
        }
    }
}
