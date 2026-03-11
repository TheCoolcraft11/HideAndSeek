package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;

public class GrapplingHookItem implements GameItem {
    public static final String ID = "has_seeker_grappling_hook";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Grappling Hook", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to launch yourself forward", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("toward where you're looking", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Reel your hook to launch yourself toward the cast point.";
    }


    @Override
    public void register(HideAndSeek plugin) {
        int grapplingHookCooldown = plugin.getSettingRegistry().get("seeker-items.grappling-hook.cooldown", 2);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.FISHING_REEL, context -> {
                    FishHook hook = context.getFishHook();
                    if (hook != null && hook.isValid()) {
                        pullGrapplingHook(context, plugin);
                    }
                })
                .withVanillaCooldown(grapplingHookCooldown * 20)
                .withCustomCooldown(grapplingHookCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(false)
                .build());
    }


    private void pullGrapplingHook(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        FishHook hook = context.getFishHook();
        if (seeker == null || !hook.isValid()) return;


        seeker.setCooldown(Material.FISHING_ROD, 100);

        Location playerLoc = seeker.getEyeLocation();
        Location hookLoc = hook.getLocation();
        Vector travelVec = hookLoc.toVector().subtract(playerLoc.toVector());
        double distance = travelVec.length();

        if (distance < 1.5) return;


        Vector direction = travelVec.normalize();
        double baseSpeed = plugin.getSettingRegistry().get("seeker-items.grappling-hook.speed", 1.3);


        double distanceFactor = Math.min(Math.sqrt(distance) * 0.25, 2.0);
        double gravityCompensation = (direction.getY() > 0) ? (1.0 + direction.getY() * 0.8) : 1.0;
        double speed = (baseSpeed + distanceFactor) * gravityCompensation;

        Vector finalVel = direction.multiply(speed);


        if (seeker.isOnGround()) {
            finalVel.setY(finalVel.getY() + 0.25);
        }

        seeker.setVelocity(seeker.getVelocity().multiply(0.3).add(finalVel.multiply(0.7)));

        World world = seeker.getWorld();
        world.spawnParticle(Particle.GUST_EMITTER_LARGE, seeker.getLocation(), 2);
        world.spawnParticle(Particle.SONIC_BOOM, seeker.getLocation(), 1, 0, 0, 0, 0);
        world.playSound(seeker.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.2f, 1.2f);


        drawGrappleLine(seeker, hookLoc);


        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (!seeker.isOnline() || !seeker.getWorld().equals(hookLoc.getWorld()) || (ticks > 5 && seeker.isOnGround()) || ticks > 30) {

                    if (seeker.isOnline() && seeker.getWorld().equals(hookLoc.getWorld()) && seeker.getLocation().distance(hookLoc) < 3) {
                        seeker.getWorld().playSound(seeker.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1f, 0.5f);
                    }
                    this.cancel();
                    return;
                }


                seeker.getWorld().spawnParticle(Particle.CLOUD, seeker.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                if (ticks % 2 == 0) {
                    seeker.getWorld().spawnParticle(Particle.GUST, seeker.getLocation(), 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        hook.remove();
    }

    private static void drawGrappleLine(Player seeker, Location hookLoc) {
        Location start = seeker.getEyeLocation().subtract(0, 0.3, 0);
        Vector direction = hookLoc.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();


        for (double i = 0; i < distance; i += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(i));

            seeker.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, point, 1, 0, 0, 0, 0);
            if (i % 0.8 == 0) {
                seeker.getWorld().spawnParticle(Particle.WHITE_SMOKE, point, 1, 0.01, 0.01, 0.01, 0);
            }
        }
    }

}
