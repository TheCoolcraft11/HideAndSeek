package de.thecoolcraft11.hideAndSeek.nms;

import de.thecoolcraft11.hideAndSeek.nms.meta.NmsAdapterMeta;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public final class NmsLoader {

    private NmsLoader() {
    }

    public static NmsAdapter load(Logger logger, boolean enabled) {
        if (!enabled) {
            logger.info("NMS is disabled. Using Paper fallback.");
            return new NoopNmsAdapter();
        }
        String serverVersion = Bukkit.getMinecraftVersion();
        String packageName = "de.thecoolcraft11.hideAndSeek.nms.impl";
        String path = packageName.replace('.', '/');
        try {
            File jarFile = new File(NmsLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();


                    if (name.startsWith(path) && name.endsWith("Meta.class") && !name.contains("$")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);

                        try {
                            Class<?> clazz = Class.forName(className, false, NmsLoader.class.getClassLoader());

                            if (NmsAdapterMeta.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                NmsAdapterMeta meta = (NmsAdapterMeta) clazz.getDeclaredConstructor().newInstance();
                                if (meta.supports(serverVersion)) {
                                    NmsAdapter adapter = meta.implementation().getDeclaredConstructor().newInstance();
                                    logger.info("Matched and Loaded: " + adapter.name());
                                    return adapter;
                                }
                            }
                        } catch (ReflectiveOperationException | LinkageError e) {
                            logger.warning("Failed to load NMS adapter class " + className + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to scan JAR for NMS adapters: " + e.getMessage());
        }

        logger.warning("No compatible NMS adapter found for " + serverVersion + ". Using Paper fallback.");
        return new NoopNmsAdapter();
    }
}
