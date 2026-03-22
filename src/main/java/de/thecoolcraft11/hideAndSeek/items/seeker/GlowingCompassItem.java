package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class GlowingCompassItem implements GameItem {
    public static final String ID = "has_seeker_glowing_compass";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Glowing Compass", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to glow the nearest hider", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("seeker-items.glowing-compass.duration", 10);
        Number range = plugin.getSettingRegistry().get("seeker-items.glowing-compass.range", 50);
        int points = plugin.getPointService().getInt("points.seeker.utility-success.amount", 40);
        return String.format("Reveal nearest hider within %d blocks with glow for %ds, grants %d points.", range.intValue(), duration.intValue(), points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int glowCooldown = plugin.getSettingRegistry().get("seeker-items.glowing-compass.cooldown", 25);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> glowHider(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> glowHider(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(glowCooldown * 20)
                .withCustomCooldown(glowCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

    }

    private static void glowHider(Player seeker, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("seeker-items.glowing-compass.range", 50.0);
        int duration = plugin.getSettingRegistry().get("seeker-items.glowing-compass.duration", 10);
        boolean tacticalTablet = ItemSkinSelectionService.isSelected(seeker, ID, "skin_tactical_tablet");
        boolean oracleEye = ItemSkinSelectionService.isSelected(seeker, ID, "skin_eye_of_the_oracle");
        boolean dowsingRod = ItemSkinSelectionService.isSelected(seeker, ID, "skin_dowsing_rod");

        Player nearest = null;
        double distance = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;
            if (!hider.getWorld().equals(seeker.getWorld())) continue;

            double dist = hider.getLocation().distance(seeker.getLocation());
            if (dist < distance) {
                distance = dist;
                nearest = hider;
            }
        }

        if (nearest != null) {
            applyGlowEffect(nearest, duration, plugin);
            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(nearest.getUniqueId());
            seeker.sendMessage(Component.text(nearest.getName() + " is now glowing!", NamedTextColor.GOLD));
            if (tacticalTablet) {
                seeker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.03);
                seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1, 0), 8, 0.25, 0.25, 0.25, 0.01);
                seeker.playSound(seeker.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.55f, 1.4f);
            } else if (oracleEye) {
                seeker.getWorld().spawnParticle(Particle.ENCHANT, seeker.getLocation().add(0, 1, 0), 20, 0.5, 0.3, 0.5, 0.2);
                seeker.getWorld().spawnParticle(Particle.PORTAL, seeker.getLocation().add(0, 1, 0), 10, 0.35, 0.25, 0.35, 0.07);
                seeker.playSound(seeker.getLocation(), Sound.ENTITY_ENDER_EYE_LAUNCH, 0.65f, 1.0f);
            } else if (dowsingRod) {
                seeker.getWorld().spawnParticle(Particle.CRIT, seeker.getLocation().add(0, 1, 0), 10, 0.35, 0.3, 0.35, 0.05);
                seeker.getWorld().spawnParticle(Particle.WAX_ON, seeker.getLocation().add(0, 1, 0), 8, 0.25, 0.2, 0.25, 0.01);
                seeker.playSound(seeker.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_HIT, 0.6f, 0.8f);
            }
            spawnScanTrail(seeker, nearest, tacticalTablet, oracleEye, dowsingRod);
        } else {
            seeker.sendMessage(Component.text("No hiders found nearby!", NamedTextColor.RED));
        }
    }

    private static void applyGlowEffect(Player hider, int duration, HideAndSeek plugin) {
        UUID hiderId = hider.getUniqueId();

        Player seeker = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (HideAndSeek.getDataController().getSeekers().contains(online.getUniqueId())
                    && online.getWorld().equals(hider.getWorld())) {
                double dist = online.getLocation().distanceSquared(hider.getLocation());
                if (dist < nearestDistance) {
                    nearestDistance = dist;
                    seeker = online;
                }
            }
        }
        boolean tacticalTablet = seeker != null && ItemSkinSelectionService.isSelected(seeker, ID, "skin_tactical_tablet");
        boolean oracleEye = seeker != null && ItemSkinSelectionService.isSelected(seeker, ID, "skin_eye_of_the_oracle");
        boolean dowsingRod = seeker != null && ItemSkinSelectionService.isSelected(seeker, ID, "skin_dowsing_rod");

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");


        HideAndSeek.getDataController().setGlowing(hiderId, true);

        if (isBlockMode) {

            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
            if (display != null && display.isValid()) {
                display.setGlowing(true);
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Team hiderTeam = scoreboard.getEntryTeam(hider.getName());
                if (hiderTeam != null) {
                    display.setGlowColorOverride(textColorToColor(hiderTeam.color()));
                }
            } else if (HideAndSeek.getDataController().isHidden(hiderId)) {

                var hiddenBlock = HideAndSeek.getDataController().getHiddenBlock(hiderId);
                var lastLoc = HideAndSeek.getDataController().getLastLocation(hiderId);
                var blockData = HideAndSeek.getDataController().getChosenBlockData(hiderId);
                if ((hiddenBlock != null || lastLoc != null) && blockData != null) {
                    Location spawnLoc = hiddenBlock != null
                            ? hiddenBlock.getLocation().clone().add(0.5, 0, 0.5)
                            : lastLoc.getBlock().getLocation().clone().add(0.5, 0, 0.5);
                    BlockDisplay tempDisplay = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, bd -> {
                        bd.setBlock(blockData);
                        bd.setGlowing(true);
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.5f, 0.001f, -0.5f),
                                new AxisAngle4f(0, 0, 0, 0),
                                new Vector3f(1.001f, 1.001f, 1.001f),
                                new AxisAngle4f(0, 0, 0, 0)
                        ));
                        bd.setBrightness(new Display.Brightness(15, 15));
                        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                        Team hiderTeam = scoreboard.getEntryTeam(hider.getName());
                        if (hiderTeam != null) {
                            bd.setGlowColorOverride(textColorToColor(hiderTeam.color()));
                        }
                        bd.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING, hiderId.toString());
                    });

                    HideAndSeek.getDataController().setBlockDisplay(hiderId, tempDisplay);
                }
            }
        } else {

            hider.setGlowing(true);
        }

        Location fxLoc = hider.getLocation().add(0, 1, 0);
        if (tacticalTablet) {
            hider.getWorld().spawnParticle(Particle.END_ROD, fxLoc, 12, 0.3, 0.4, 0.3, 0.02);
            hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, fxLoc, 10, 0.26, 0.35, 0.26, 0.02);
            hider.playSound(hider.getLocation(), Sound.BLOCK_BEACON_AMBIENT, 0.4f, 1.6f);
        } else if (oracleEye) {
            hider.getWorld().spawnParticle(Particle.PORTAL, fxLoc, 16, 0.3, 0.4, 0.3, 0.1);
            hider.getWorld().spawnParticle(Particle.ENCHANT, fxLoc, 8, 0.22, 0.3, 0.22, 0.02);
            hider.playSound(hider.getLocation(), Sound.ENTITY_ENDERMAN_AMBIENT, 0.35f, 1.2f);
        } else if (dowsingRod) {
            hider.getWorld().spawnParticle(Particle.WAX_ON, fxLoc, 10, 0.25, 0.35, 0.25, 0.02);
            hider.getWorld().spawnParticle(Particle.CRIT, fxLoc, 8, 0.2, 0.28, 0.2, 0.02);
            hider.playSound(hider.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.35f, 0.85f);
        }

        new org.bukkit.scheduler.BukkitRunnable() {
            final int maxTicks = duration * 20;
            int ticks = 0;

            @Override
            public void run() {
                if (!hider.isOnline() || ticks >= maxTicks || !HideAndSeek.getDataController().isGlowing(hider.getUniqueId())) {
                    cancel();
                    return;
                }

                Location pulse = hider.getLocation().add(0, 1, 0);
                if (tacticalTablet) {
                    hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, pulse, 3, 0.22, 0.28, 0.22, 0.01);
                    if (ticks % 20 == 0) {
                        hider.getWorld().spawnParticle(Particle.END_ROD, pulse, 4, 0.28, 0.35, 0.28, 0.01);
                    }
                } else if (oracleEye) {
                    hider.getWorld().spawnParticle(Particle.ENCHANT, pulse, 3, 0.2, 0.28, 0.2, 0.01);
                    if (ticks % 20 == 0) {
                        hider.getWorld().spawnParticle(Particle.PORTAL, pulse, 6, 0.25, 0.32, 0.25, 0.05);
                    }
                } else if (dowsingRod) {
                    hider.getWorld().spawnParticle(Particle.WAX_ON, pulse, 3, 0.2, 0.28, 0.2, 0.01);
                    if (ticks % 20 == 0) {
                        hider.getWorld().spawnParticle(Particle.CRIT, pulse, 4, 0.24, 0.3, 0.24, 0.02);
                    }
                }

                ticks += 5;
            }
        }.runTaskTimer(plugin, 5L, 5L);


        Bukkit.getScheduler().runTaskLater(plugin, () -> removeGlowEffect(hider, isBlockMode, plugin), duration * 20L);
    }

    private static void removeGlowEffect(Player hider, boolean isBlockMode, HideAndSeek plugin) {
        UUID hiderId = hider.getUniqueId();
        HideAndSeek.getDataController().removeGlowing(hiderId);

        if (isBlockMode) {
            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
            if (display != null && display.isValid()) {

                String tempGlowId = display.getPersistentDataContainer().get(new NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING);
                if (tempGlowId != null && tempGlowId.equals(hiderId.toString())) {

                    display.remove();
                } else {

                    display.setGlowing(false);
                    display.setGlowColorOverride(null);
                }
            }
        } else {

            hider.setGlowing(false);
        }
    }

    private static Color textColorToColor(TextColor chatColor) {
        return chatColor != null ? Color.fromRGB(chatColor.red(), chatColor.green(), chatColor.blue()) : Color.WHITE;
    }

    private static void spawnScanTrail(Player seeker, Player target, boolean tacticalTablet, boolean oracleEye, boolean dowsingRod) {
        Location start = seeker.getEyeLocation();
        Location end = target.getLocation().add(0, 1, 0);
        Vector3f dir = new Vector3f((float) (end.getX() - start.getX()), (float) (end.getY() - start.getY()), (float) (end.getZ() - start.getZ()));
        float len = dir.length();
        if (len < 0.1f) {
            return;
        }
        dir.div(len);
        World world = seeker.getWorld();
        int steps = Math.max(4, (int) (len / 0.7f));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / (float) steps;
            Location point = start.clone().add(dir.x * len * t, dir.y * len * t, dir.z * len * t);
            if (tacticalTablet) {
                world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.02, 0.02, 0.02, 0.0);
            } else if (oracleEye) {
                world.spawnParticle(Particle.PORTAL, point, 1, 0.03, 0.03, 0.03, 0.01);
            } else if (dowsingRod) {
                world.spawnParticle(Particle.WAX_ON, point, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }
}
