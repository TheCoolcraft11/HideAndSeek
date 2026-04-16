package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class FireworkRocketItem implements GameItem {
    public static final String ID = "has_hider_firework_rocket";

    @Override
    public String getId() {
        return ID;
    }

    private static void launchFirework(Player player, Location baseLocation, HideAndSeek plugin) {


        BlockDisplay display = (BlockDisplay) player.getWorld().spawnEntity(
                baseLocation,
                EntityType.BLOCK_DISPLAY
        );
        display.setBlock(Bukkit.createBlockData(Material.STRIPPED_BAMBOO_BLOCK));
        display.setTransformation(new Transformation(
                        new Vector3f(0, 0, 0),
                        new Quaternionf(),
                        new Vector3f(0.5f, 0.75f, 0.5f),
                        new Quaternionf()
                )
        );

        int targetY = plugin.getSettingRegistry()
                .get("hider-items.firework-rocket.target-y", 128);

        int points = plugin.getPointService()
                .award(player.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);

        double volume = plugin.getSettingRegistry()
                .get("hider-items.firework-rocket.volume", 10.0);

        boolean spaceShuttle = ItemSkinSelectionService.isSelected(player, ID, "skin_space_shuttle");
        boolean signalFlare = ItemSkinSelectionService.isSelected(player, ID, "skin_signal_flare");

        player.sendMessage(Component.text("Firework sequence started! +" + points + " points",
                NamedTextColor.GOLD));

        var nms = plugin.getNmsAdapter();
        boolean useNoClip = nms != null && nms.isAvailable();

        new BukkitRunnable() {

            int count = 0;
            Firework activeFirework = null;

            @Override
            public void run() {


                if (display.isDead() || !display.isValid()) {
                    cancel();
                    return;
                }


                if (activeFirework != null && activeFirework.isValid()) {
                    return;
                }

                if (count >= 6) {
                    display.remove();
                    cancel();
                    return;
                }

                boolean isFirstOrLast = (count == 0 || count == 5);

                Location spawnLoc = display.getLocation().clone().add(0, 0.5, 0);

                Firework firework = (Firework) spawnLoc.getWorld()
                        .spawnEntity(spawnLoc, EntityType.FIREWORK_ROCKET);

                FireworkMeta meta = firework.getFireworkMeta();
                meta.setPower(10);
                meta.addEffect(FireworkEffect.builder()
                        .with(spaceShuttle ? FireworkEffect.Type.STAR
                                : signalFlare ? FireworkEffect.Type.BURST
                                  : FireworkEffect.Type.BALL_LARGE)
                        .withColor(
                                spaceShuttle ? Color.WHITE : Color.RED,
                                spaceShuttle ? Color.AQUA : Color.YELLOW,
                                Color.ORANGE
                        )
                        .withFade(signalFlare ? Color.RED : Color.WHITE)
                        .flicker(!spaceShuttle)
                        .trail(true)
                        .build());

                firework.setFireworkMeta(meta);

                if (useNoClip) {
                    nms.setNoClipForEntity(firework, true);
                }

                Vector direction;

                if (isFirstOrLast) {
                    direction = new Vector(0, 1.2, 0);
                } else {
                    double angleX = (Math.random() - 0.5) * 0.02;
                    double angleZ = (Math.random() - 0.5) * 0.02;
                    direction = new Vector(angleX, 1.2, angleZ);
                }

                firework.setVelocity(direction);

                activeFirework = firework;


                new BukkitRunnable() {
                    @Override
                    public void run() {

                        if (!firework.isValid()) {
                            activeFirework = null;
                            cancel();
                            return;
                        }

                        Location loc = firework.getLocation();

                        if (spaceShuttle) {
                            loc.getWorld().spawnParticle(Particle.END_ROD, loc, 4, 0.08, 0.08, 0.08, 0.01);
                            loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.06, 0.06, 0.06, 0.01);
                        } else if (signalFlare) {
                            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 4, 0.08, 0.08, 0.08, 0.01);
                            loc.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.05, 0.05, 0.05, 0.02);
                            loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 2, 0.06, 0.06, 0.06,
                                    0.003);
                        }

                        firework.setTicksToDetonate(2000);
                        firework.setTicksLived(1);

                        if (loc.getY() >= targetY) {
                            detonate(firework, volume, spaceShuttle, signalFlare);
                            activeFirework = null;
                            this.cancel();
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);

                count++;
            }
        }.runTaskTimer(plugin, 0L, 40L);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.taunt.large", 75);
        return String.format("Launch a high-altitude firework taunt, granting %d points.", points);
    }

    private static void detonate(Firework firework, double volume, boolean spaceShuttle, boolean signalFlare) {
        firework.detonate();
        Location loc = firework.getLocation();

        if (spaceShuttle) {
            loc.getWorld().spawnParticle(Particle.FIREWORK, loc, 18, 0.5, 0.5, 0.5, 0.04);
        } else if (signalFlare) {
            loc.getWorld().spawnParticle(Particle.FLAME, loc, 24, 0.4, 0.4, 0.4, 0.03);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 18, 0.4, 0.4, 0.4, 0.03);
            loc.getWorld().spawnParticle(Particle.LAVA, loc, 12, 0.32, 0.32, 0.32, 0.01);
            loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 14, 0.35, 0.35, 0.35, 0.01);
        }

        for (Player p : loc.getNearbyPlayers(200)) {
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, (float) volume, 0.9f);
            p.playSound(p.getLocation(),
                    signalFlare ? Sound.BLOCK_FIRE_EXTINGUISH
                            : Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                    (float) volume,
                    signalFlare ? 1.4f : 0.9f);

            if (signalFlare) {
                p.playSound(p.getLocation(),
                        Sound.ENTITY_BLAZE_SHOOT,
                        Math.max(0.1f, (float) (volume * 0.35)),
                        1.45f);
            }
        }
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.firework-rocket.cooldown");
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Firework Rocket", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to launch a firework", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay()
                            .addHiddenComponents(DataComponentTypes.FIREWORKS)
                            .build());
        }
        return item;
    }

    @Override
    public void register(HideAndSeek plugin) {
        int fireworkCooldown = plugin.getSettingRegistry()
                .get("hider-items.firework-rocket.cooldown", 30);

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withAction(ItemActionType.RIGHT_CLICK_AIR,
                                context -> launchFirework(context.getPlayer(),
                                        context.getBlock().getLocation().clone().add(0, 1, 0), plugin))
                        .withAction(ItemActionType.RIGHT_CLICK_BLOCK,
                                context -> launchFirework(context.getPlayer(),
                                        context.getBlock().getLocation().clone().add(0, 1, 0), plugin))
                        .withDescription(getDescription(plugin))
                        .withDropPrevention(true)
                        .withCraftPrevention(true)
                        .withVanillaCooldown(fireworkCooldown * 20)
                        .withCustomCooldown(fireworkCooldown * 1000L)
                        .withVanillaCooldownDisplay(true)
                        .allowOffHand(false)
                        .allowArmor(false)
                        .cancelDefaultAction(true)
                        .build()
        );
    }
}