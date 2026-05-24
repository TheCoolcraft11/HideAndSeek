package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.DelayedActivationPerk;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTier;
import de.thecoolcraft11.hideAndSeek.perk.impl.BasePerk;
import de.thecoolcraft11.minigameframework.mappicker.CancelReason;
import de.thecoolcraft11.minigameframework.mappicker.MapPickerBuilder;
import de.thecoolcraft11.minigameframework.mappicker.MapPickerCallback;
import de.thecoolcraft11.minigameframework.mappicker.MapPickerResult;
import de.thecoolcraft11.minigameframework.mappicker.config.CoordDisplay;
import de.thecoolcraft11.minigameframework.mappicker.config.CursorMode;
import de.thecoolcraft11.minigameframework.mappicker.config.InputMethod;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.awt.*;
import java.util.UUID;

public class MapTeleportPerk extends BasePerk implements DelayedActivationPerk {
    @Override
    public String getId() {
        return "seeker_map_teleport";
    }

    @Override
    public Component getDisplayName() {
        return Component.text("Map Teleport", NamedTextColor.GOLD);
    }

    @Override
    public Component getDescription() {
        return Component.text("Select a teleport point on the map.", NamedTextColor.GRAY);
    }

    @Override
    public Material getIcon() {
        return Material.MAP;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.RARE;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return super.getCost();
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        int cost = plugin.getSettingRegistry().get("perks.perk.seeker_map_teleport.cost", getCost());
        double minDistance = plugin.getSettingRegistry().get("perks.perk.seeker_map_teleport.min-distance-from-hider", 5.0d);
        int blindnessTicks = plugin.getSettingRegistry().get("perks.perk.seeker_map_teleport.blindness-ticks", 40);
        int mapViewHeight = plugin.getSettingRegistry().get("perks.perk.global.map-picker.view-height", 350);

        Location center = player.getWorld().getWorldBorder().getCenter().clone();
        double radius = player.getWorld().getWorldBorder().getSize() / 2;
        int minX = (int) Math.floor(center.getX() - radius);
        int minZ = (int) Math.floor(center.getZ() - radius);
        int maxX = (int) Math.ceil(center.getX() + radius);
        int maxZ = (int) Math.ceil(center.getZ() + radius);

        try {
            MapPickerBuilder.forPlayer(plugin, player)
                    .world(player.getWorld())
                    .area(minX, minZ, maxX, maxZ)
                    .cursorMode(CursorMode.POINT)
                    .inputMethod(InputMethod.BOTH)
                    .title("Map Teleport")
                    .showPlayerMarker(true)
                    .freezePlayer(true)
                    .allowSnapToPosition(true)
                    .sendBlockPacketsToPlayer(player)
                    .coordDisplay(CoordDisplay.BOSSBAR)
                    .coordColor(NamedTextColor.RED)
                    .coordFormat("Teleporting to: X: {x}, Z: {z}")
                    .cursorColor(Color.ORANGE)
                    .rightClickShowsHelp(true)
                    .mapViewHeight(mapViewHeight)
                    .open(new MapPickerCallback() {
                        @Override
                        public void onConfirm(MapPickerResult result) {
                            int x = result.getWorldX();
                            int y = result.getSurfaceY();
                            int z = result.getWorldZ();


                            Location target = new Location(player.getWorld(), x + 0.5, y + 1.0, z + 0.5, player.getYaw(), player.getPitch());

                            for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                                Player hider = Bukkit.getPlayer(hiderId);
                                if (hider == null || !hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                                    continue;
                                }
                                if (hider.getLocation().distance(target) < minDistance) {
                                    player.sendMessage(Component.text("Too close to a hider.", NamedTextColor.RED));
                                    return;
                                }
                            }

                            player.teleport(target);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindnessTicks, 0, false, false, false));
                        }

                        @Override
                        public void onCancel(CancelReason reason) {
                            refundPurchase(player, plugin, cost);
                            player.sendMessage(Component.text("Teleport cancelled.", NamedTextColor.GRAY));
                        }
                    });
        } catch (IllegalStateException ex) {
            refundPurchase(player, plugin, cost);
            player.sendMessage(Component.text("Could not open the map picker right now. Your points were refunded.", NamedTextColor.RED));
        }
    }

    private void refundPurchase(Player player, HideAndSeek plugin, int cost) {
        plugin.getPerkService().getStateManager().removePurchased(player.getUniqueId(), getId());
        HideAndSeek.getDataController().addPoints(player.getUniqueId(), cost);
        plugin.getPerkShopGUI().refreshForPlayer(player);
    }
}


