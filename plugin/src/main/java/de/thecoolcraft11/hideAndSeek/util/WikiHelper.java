package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.*;

public class WikiHelper {

    public static void ensureWikiExists(HideAndSeek plugin) {
        try {
            new WikiHelper().saveWikiFolder(plugin);
        } catch (IOException | URISyntaxException e) {
            plugin.getLogger().severe("Failed to extract wiki folder: " + e.getMessage());
        }
    }

    public static void registerPlaceholders(HideAndSeek plugin) {
        plugin.getWikiRegistry().addPlaceholderResolver((player, key) -> {
            if (key.startsWith("setting-")) {
                Object o = plugin.getSettingRegistry().get(key.substring("setting-".length()));
                if (o == null) return plugin.tr(player, "wiki.setting_not_found");
                return Component.text(o.toString());
            }
            if (key.startsWith("rarity-")) {
                LoadoutItemType itemType = LoadoutItemType.fromID("has_hider_" + key.substring("rarity-".length()));
                if (itemType == null) {
                    itemType = LoadoutItemType.fromID("has_seeker_" + key.substring("rarity-".length()));
                }
                if (itemType != null) {
                    String color = switch (itemType.getRarity()) {
                        case COMMON -> "white";
                        case UNCOMMON -> "green";
                        case RARE -> "blue";
                        case EPIC -> "light_purple";
                        case LEGENDARY -> "gold";
                    };
                    return Component.text("<" + color + ">" + itemType.getRarity().name() + "</" + color + ">");
                }
            }
            if (key.startsWith("desc-")) {
                GameItem gameItem = HiderItems.getItem("has_hider_" + key.substring("desc-".length()));
                if (gameItem == null) {
                    gameItem = SeekerItems.getItem("has_seeker_" + key.substring("desc-".length()));
                }
                if (gameItem != null) {
                    return Component.text(gameItem.getDescription(plugin, player));
                }
            }
            if (key.startsWith("perk-rarity-")) {
                String perkId = key.substring("perk-rarity-".length());
                for (PerkDefinition perk : plugin.getPerkRegistry().getAllPerks()) {
                    if (perk.getId().equals(perkId)) {
                        String color = switch (perk.getTier()) {
                            case COMMON -> "white";
                            case UNCOMMON -> "green";
                            case RARE -> "blue";
                            case EPIC -> "light_purple";
                            case LEGENDARY -> "gold";
                            default -> "white";
                        };
                        return Component.text("<" + color + ">" + perk.getTier().name() + "</" + color + ">");
                    }
                }
            }
            return plugin.tr(player, "wiki.resolver_not_found");
        });

        plugin.getWikiRegistry().addItemPlaceholderResolver((player, key) -> {
            if (key.startsWith("item-")) {
                GameItem gameItem = SeekerItems.getItem("has_seeker_" + key.substring("item-".length()));
                if (gameItem == null) {
                    gameItem = HiderItems.getItem("has_hider_" + key.substring("item-".length()));
                }
                if (gameItem == null) return new ItemStack(Material.BARRIER);
                return gameItem.createItem(plugin);
            }
            return new ItemStack(Material.AIR);
        });

    }

    private void saveWikiFolder(HideAndSeek plugin) throws IOException, URISyntaxException {
        Path jarPath = Paths.get(getClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());

        try (FileSystem fs = FileSystems.newFileSystem(jarPath)) {
            Path source = fs.getPath("wiki");
            Path target = plugin.getDataFolder().toPath().resolve("wiki");

            Files.walk(source).forEach(path -> {
                try {
                    Path relative = source.relativize(path);
                    Path destination = target.resolve(relative.toString());

                    if (Files.isDirectory(path)) {
                        Files.createDirectories(destination);
                    } else {
                        Files.createDirectories(destination.getParent());
                        Files.copy(path, destination,
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to extract wiki file: " + e.getMessage());
                }
            });
        }
    }
}
