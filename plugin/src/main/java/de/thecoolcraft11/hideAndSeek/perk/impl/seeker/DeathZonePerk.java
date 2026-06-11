package de.thecoolcraft11.hideAndSeek.perk.impl.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener;
import de.thecoolcraft11.hideAndSeek.perk.AreaWarnHelper;
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

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;

public class DeathZonePerk extends BasePerk implements DelayedActivationPerk {

    private static final Map<UUID, List<AreaWarnHelper>> sessions = new HashMap<>();

    @Override
    public String getId() {
        return "seeker_death_zone";
    }

    @Override
    public Material getIcon() {
        return Material.WITHER_ROSE;
    }

    @Override
    public PerkTier getTier() {
        return PerkTier.LEGENDARY;
    }

    @Override
    public PerkTarget getTarget() {
        return PerkTarget.SEEKER;
    }

    @Override
    public int getCost() {
        return super.getCost();
    }

    public static void clearWarningsFor(UUID playerId) {
        for (List<AreaWarnHelper> helpers : sessions.values()) {
            for (AreaWarnHelper helper : helpers) {
                helper.clearForPlayer(playerId);
            }
        }
    }

    private void refundPurchase(Player player, HideAndSeek plugin, int cost) {
        plugin.getPerkService().getStateManager().removePurchased(player.getUniqueId(), getId());
        HideAndSeek.getDataController().addPoints(player.getUniqueId(), cost);
        plugin.getPerkShopGUI().refreshForPlayer(player);
    }

    @Override
    public void onPurchase(Player player, HideAndSeek plugin) {
        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(plugin.trText(player, "perk.seeker_death_zone.wrong_phase"));
            return;
        }

        int cost = plugin.getSettingRegistry().get("perks.perk.seeker_death_zone.cost", getCost());
        int escapeSeconds = plugin.getSettingRegistry().get("perks.perk.seeker_death_zone.escape-seconds", 60);
        int zoneRadius = plugin.getSettingRegistry().get("perks.perk.seeker_death_zone.radius", 32);
        int mapViewHeight = plugin.getSettingRegistry().get("perks.perk.global.map-picker.view-height", 350);

        Location center = player.getWorld().getWorldBorder().getCenter().clone();
        double radius = player.getWorld().getWorldBorder().getSize() / 2;
        int minX = (int) Math.floor(center.getX() - radius);
        int minZ = (int) Math.floor(center.getZ() - radius);
        int maxX = (int) Math.ceil(center.getX() + radius);
        int maxZ = (int) Math.ceil(center.getZ() + radius);


        try {
            List<AreaWarnHelper> helpers = sessions.computeIfAbsent(player.getUniqueId(), ignored -> new ArrayList<>());

            MapPickerBuilder.forPlayer(plugin, player)
                    .world(player.getWorld())
                    .area(minX, minZ, maxX, maxZ)
                    .cursorMode(CursorMode.CIRCLE)
                    .inputMethod(InputMethod.BOTH)
                    .title(plugin.trText(player, "perk.seeker_death_zone.map_picker_title"))
                    .showPlayerMarker(true)
                    .freezePlayer(true)
                    .allowSnapToPosition(true)
                    .sendBlockPacketsToPlayer(player)
                    .coordDisplay(CoordDisplay.BOSSBAR)
                    .coordColor(NamedTextColor.RED)
                    .coordFormat(plugin.trText(player, "perk.seeker_death_zone.map_picker_coord_format"))
                    .cursorColor(Color.RED)
                    .rightClickShowsHelp(true)
                    .circleRadius(zoneRadius)
                    .circleMaxRadius(zoneRadius)
                    .circleMinRadius(zoneRadius)
                    .mapViewHeight(mapViewHeight)
                    .open(new MapPickerCallback() {
                        @Override
                        public void onConfirm(MapPickerResult result) {
                            Location center = new Location(player.getWorld(), result.getWorldX() + 0.5, player.getY(), result.getWorldZ() + 0.5);
                            double radius = Math.max(1.0d, result.getRadius());
                            List<UUID> hidersSnapshot = new ArrayList<>(HideAndSeek.getDataController().getHiders());

                            AreaWarnHelper helper = new AreaWarnHelper(plugin, center, radius, escapeSeconds * 20);
                            helper.start(hidersSnapshot);
                            helpers.add(helper);

                            for (UUID hiderId : hidersSnapshot) {
                                Player hider = Bukkit.getPlayer(hiderId);
                                if (hider != null && hider.isOnline()) {
                                    hider.sendMessage(plugin.trText(hider, "perk.seeker_death_zone.hider_warning"));
                                }
                            }

                            player.sendMessage(plugin.trText(player, "perk.seeker_death_zone.activated"));

                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                for (UUID hiderId : hidersSnapshot) {
                                    Player hider = Bukkit.getPlayer(hiderId);
                                    if (hider == null || !hider.isOnline() || hider.getGameMode() == GameMode.SPECTATOR) {
                                        continue;
                                    }
                                    if (!HideAndSeek.getDataController().getHiders().contains(hiderId)) {
                                        continue;
                                    }
                                    if (helper.isInsideZone(hider.getLocation())) {
                                        plugin.getPlayerHitListener().markEnvironmentalDeath(hiderId, PlayerHitListener.EnvironmentalDeathCause.PERK_DEATH_ZONE);
                                        hider.setHealth(0.0);
                                    }
                                }
                                helper.stop();
                                helpers.remove(helper);
                                if (helpers.isEmpty()) {
                                    sessions.remove(player.getUniqueId());
                                }
                            }, escapeSeconds * 20L);
                        }

                        @Override
                        public void onCancel(CancelReason reason) {
                            refundPurchase(player, plugin, cost);
                            player.sendMessage(plugin.trText(player, "perk.seeker_death_zone.cancelled"));
                        }
                    });
        } catch (IllegalStateException ex) {
            refundPurchase(player, plugin, cost);
            player.sendMessage(plugin.trText(player, "perk.seeker_death_zone.cant_open"));
        }
    }

    @Override
    public void onExpire(Player player, HideAndSeek plugin) {
        List<AreaWarnHelper> helpers = sessions.remove(player.getUniqueId());
        if (helpers == null) {
            return;
        }

        for (AreaWarnHelper helper : helpers) {
            helper.stop();
        }
    }
}

