package de.thecoolcraft11.hideAndSeek.items.api;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public interface GameItem {
    String getId();

    default List<String> getAllIds() {
        return List.of(getId());
    }

    default Set<String> getConfigKeys() {
        return Set.of();
    }

    ItemStack createItem(HideAndSeek plugin);

    String getDescription(HideAndSeek plugin, @Nullable Player player);

    void register(HideAndSeek plugin);
}
