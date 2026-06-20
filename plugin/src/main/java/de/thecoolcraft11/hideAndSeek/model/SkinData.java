package de.thecoolcraft11.hideAndSeek.model;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

public record SkinData(String id, String name, ItemStack icon, String value, String signature, String translationKey) {

    public String getDisplayName(HideAndSeek plugin, CommandSender sender) {
        if (translationKey != null && !translationKey.isBlank()) {
            String translated = plugin.trText(sender, translationKey);
            if (!translated.equals("!" + translationKey + "!")) {
                return translated;
            }
        }
        return name;
    }

    @Override
    public @NonNull String toString() {
        return "SkinData{id='" + id + "', name='" + name + "'}";
    }
}