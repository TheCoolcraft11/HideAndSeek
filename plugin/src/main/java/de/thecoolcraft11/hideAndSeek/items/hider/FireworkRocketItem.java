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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class FireworkRocketItem implements GameItem {
    public static final String ID = "has_hider_firework_rocket";

    @Override
    public String getId() {
        return ID;
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
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.FIREWORKS).build());
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.taunt.large", 75);
        return String.format("Launch a high-altitude firework taunt, granting %d points.", points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int fireworkCooldown = plugin.getSettingRegistry().get("hider-items.firework-rocket.cooldown", 10);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> launchFirework(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> launchFirework(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(fireworkCooldown * 20)
                .withCustomCooldown(fireworkCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.firework-rocket.cooldown");
    }

    private static void launchFirework(Player player, HideAndSeek plugin) {
        Location launchLocation = player.getLocation().clone().add(0, 1.5, 0);
        int targetY = plugin.getSettingRegistry().get("hider-items.firework-rocket.target-y", 128);
        int points = plugin.getPointService().award(player.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);
        double volume = plugin.getSettingRegistry().get("hider-items.firework-rocket.volume", 10.0);
        boolean spaceShuttle = ItemSkinSelectionService.isSelected(player, ID, "skin_space_shuttle");
        boolean signalFlare = ItemSkinSelectionService.isSelected(player, ID, "skin_signal_flare");

        Firework firework = (Firework) launchLocation.getWorld().spawnEntity(launchLocation, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(10);
        meta.addEffect(FireworkEffect.builder()
                .with(spaceShuttle ? FireworkEffect.Type.STAR : signalFlare ? FireworkEffect.Type.BURST : FireworkEffect.Type.BALL_LARGE)
                .withColor(spaceShuttle ? Color.WHITE : Color.RED,
                        spaceShuttle ? Color.AQUA : Color.YELLOW,
                        Color.ORANGE)
                .withFade(signalFlare ? Color.RED : Color.WHITE)
                .flicker(!spaceShuttle)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);

        player.sendMessage(Component.text("Firework launched! +" + points + " points", NamedTextColor.GOLD));
        if (signalFlare) {
            launchLocation.getWorld().spawnParticle(Particle.FLAME, launchLocation, 16, 0.2, 0.3, 0.2, 0.04);
            launchLocation.getWorld().spawnParticle(Particle.SMOKE, launchLocation, 12, 0.18, 0.28, 0.18, 0.02);
            player.playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.5f, 1.25f);
        }


        var nms = plugin.getNmsAdapter();
        boolean useNoClip = nms != null && nms.isAvailable();

        if (useNoClip) {
            nms.setNoClipForEntity(firework, true);
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!firework.isValid()) {
                    this.cancel();
                    return;
                }

                Location loc = firework.getLocation();
                if (spaceShuttle) {
                    loc.getWorld().spawnParticle(Particle.END_ROD, loc, 4, 0.08, 0.08, 0.08, 0.01);
                    loc.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.06, 0.06, 0.06, 0.01);
                } else if (signalFlare) {
                    loc.getWorld().spawnParticle(Particle.SMOKE, loc, 4, 0.08, 0.08, 0.08, 0.01);
                    loc.getWorld().spawnParticle(Particle.FLAME, loc, 2, 0.05, 0.05, 0.05, 0.02);
                    loc.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, loc, 2, 0.06, 0.06, 0.06, 0.003);
                }
                firework.setTicksToDetonate(2000);
                firework.setTicksLived(1);


                if (loc.getY() >= targetY) {
                    detonate(firework, volume, spaceShuttle, signalFlare);
                    this.cancel();
                    return;
                }

                if (useNoClip) {

                    firework.setVelocity(new Vector(0, 1.2, 0));
                } else {

                    World world = loc.getWorld();
                    Vector up = new Vector(0, 1, 0);
                    var rayResult = world.rayTraceBlocks(loc, up, 2.0);

                    if (rayResult != null && rayResult.getHitBlock() != null) {
                        Location safeLoc = world.getHighestBlockAt(rayResult.getHitBlock().getLocation())
                                .getLocation().add(0, 1, 0);
                        firework.teleport(safeLoc);
                    } else {
                        firework.setVelocity(new Vector(0, 1.2, 0));
                    }
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
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
            p.playSound(p.getLocation(), signalFlare ? Sound.BLOCK_FIRE_EXTINGUISH : Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, (float) volume, signalFlare ? 1.4f : 0.9f);
            if (signalFlare) {
                p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, Math.max(0.1f, (float) (volume * 0.35)), 1.45f);
            }
        }
    }
}
