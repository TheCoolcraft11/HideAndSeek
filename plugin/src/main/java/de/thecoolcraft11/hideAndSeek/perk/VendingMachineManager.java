package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class VendingMachineManager {

    private final HideAndSeek plugin;
    private final Set<String> vendingLocations = new HashSet<>();
    private final List<UUID> labelEntities = new ArrayList<>();

    public VendingMachineManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void placeVendingMachines() {
        String mapName = HideAndSeek.getDataController().getCurrentMapName();
        if (mapName == null || mapName.isBlank()) {
            return;
        }

        MapData mapData = plugin.getMapManager().getMapData(mapName);
        if (mapData == null) {
            return;
        }

        List<String> locationStrings = mapData.getVendingMachineLocations();
        if (locationStrings.isEmpty()) {
            return;
        }

        World world = Bukkit.getWorld("has_" + mapName);
        if (world == null) {
            plugin.getLogger().warning("Game world not loaded for map: " + mapName);
            return;
        }

        for (String locStr : locationStrings) {
            try {
                String[] parts = locStr.split(",");
                int x = Integer.parseInt(parts[0].trim());
                int y = Integer.parseInt(parts[1].trim());
                int z = Integer.parseInt(parts[2].trim());

                Location loc = new Location(world, x, y, z);
                loc.getBlock().setType(Material.DROPPER);
                spawnShopLabel(loc);
                vendingLocations.add(blockKey(loc));
            } catch (Exception ex) {
                plugin.getLogger().warning("Invalid vending machine location '" + locStr + "' on map " + mapName + ": " + ex.getMessage());
            }
        }
    }

    public void removeVendingMachines() {
        for (String key : vendingLocations) {
            Location loc = keyToLocation(key);
            if (loc != null && loc.getBlock().getType() == Material.DROPPER) {
                loc.getBlock().setType(Material.AIR);
            }
        }
        vendingLocations.clear();

        for (UUID entityId : labelEntities) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null) {
                entity.remove();
            }
        }
        labelEntities.clear();
    }

    public boolean isVendingMachine(Location location) {
        return location != null && vendingLocations.contains(blockKey(location));
    }

    private void spawnShopLabel(Location loc) {
        ArmorStand stand = loc.getWorld().spawn(loc.clone().add(0.5, 1.0, 0.5), ArmorStand.class, s -> {
            s.setVisible(false);
            s.setGravity(false);
            s.setInvulnerable(true);
            s.setMarker(true);
            s.setSmall(true);
            s.setCustomNameVisible(true);
            s.customName(Component.text("Perk Shop", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
            s.getPersistentDataContainer().set(new NamespacedKey(plugin, "perk_shop_label"), PersistentDataType.BOOLEAN, true);
        });
        labelEntities.add(stand.getUniqueId());
    }

    private String blockKey(Location loc) {
        return loc.getWorld().getUID() + ":" + loc.getBlockX() + ":" + loc.getBlockY() + ":" + loc.getBlockZ();
    }

    private @Nullable Location keyToLocation(String key) {
        try {
            String[] parts = key.split(":");
            World world = Bukkit.getWorld(UUID.fromString(parts[0]));
            if (world == null) {
                return null;
            }
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (Exception ex) {
            return null;
        }
    }
}

