package de.thecoolcraft11.hideAndSeek.nms;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NmsLoader {

    private NmsLoader() {
    }

    public static NmsAdapter load(HideAndSeek plugin) {
        Logger logger = plugin.getLogger();
        boolean enabled = plugin.getConfig().getBoolean("nms.enabled");
        if (!enabled) {
            logger.info("NMS is disabled. Using Paper fallback.");
            return new NoopNmsAdapter();
        }
        String serverVersion = Bukkit.getMinecraftVersion();
        String packageName = "de.thecoolcraft11.hideAndSeek.nms.impl";
        String path = packageName.replace('.', '/');
        try {
            File jarFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());

            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();


                    if (name.startsWith(path) && name.endsWith(".class") && !name.contains("$")) {
                        String className = name.replace('/', '.').substring(0, name.length() - 6);

                        try {
                            Class<?> clazz = Class.forName(className);

                            if (NmsAdapter.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                NmsAdapter adapter = (NmsAdapter) clazz.getDeclaredConstructor().newInstance();
                                if (adapter.isCompatible(serverVersion)) {
                                    logger.info("Matched and Loaded: " + adapter.name());
                                    return adapter;
                                }
                            }
                        } catch (ReflectiveOperationException | LinkageError e) {
                            logger.log(Level.FINE, "Failed to load NMS adapter class " + className + ": " + e.getMessage(), e);
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