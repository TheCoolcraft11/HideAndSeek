package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
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


    private static void drawGrappleLine(Player seeker, Location hookLoc, boolean techno, boolean vine, boolean ghost) {
        Location start = seeker.getEyeLocation().subtract(0, 0.3, 0);
        Vector direction = hookLoc.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();


        for (double i = 0; i < distance; i += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(i));

            Particle lineParticle = techno ? Particle.ELECTRIC_SPARK : vine ? Particle.HAPPY_VILLAGER : ghost ? Particle.SOUL : Particle.TRIAL_SPAWNER_DETECTION;
            seeker.getWorld().spawnParticle(lineParticle, point, 1, 0, 0, 0, 0);
            if (i % 0.8 == 0) {
                seeker.getWorld().spawnParticle(ghost ? Particle.SMOKE : Particle.WHITE_SMOKE, point, 1, 0.01, 0.01, 0.01, 0);
            }
        }
    }

    private void pullGrapplingHook(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        FishHook hook = context.getFishHook();
        if (seeker == null || !hook.isValid()) return;
        boolean techno = ItemSkinSelectionService.isSelected(seeker, ID, "skin_techno_tether");
        boolean vine = ItemSkinSelectionService.isSelected(seeker, ID, "skin_jungle_vine");
        boolean ghost = ItemSkinSelectionService.isSelected(seeker, ID, "skin_ghostly_chain");


        seeker.setCooldown(Material.FISHING_ROD, 100);

        if (ghost) {
            seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1, 0), 10, 0.25, 0.3, 0.25, 0.02);
            seeker.playSound(seeker.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.55f);
        }

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
        if (techno) {
            world.spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation(), 12, 0.25, 0.25, 0.25, 0.03);
            world.spawnParticle(Particle.GLOW, seeker.getLocation(), 8, 0.2, 0.2, 0.2, 0.02);
            world.playSound(seeker.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.9f, 1.4f);
        } else if (vine) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, seeker.getLocation(), 8, 0.25, 0.25, 0.25, 0.02);
            world.spawnParticle(Particle.CHERRY_LEAVES, seeker.getLocation(), 6, 0.2, 0.2, 0.2, 0.02);
            world.playSound(seeker.getLocation(), Sound.BLOCK_VINE_STEP, 0.9f, 1.0f);
        } else if (ghost) {
            world.spawnParticle(Particle.SOUL, seeker.getLocation(), 10, 0.2, 0.2, 0.2, 0.02);
            world.spawnParticle(Particle.SMOKE, seeker.getLocation(), 6, 0.2, 0.2, 0.2, 0.01);
            world.playSound(seeker.getLocation(), Sound.BLOCK_CHAIN_PLACE, 0.9f, 0.8f);
        } else {
            world.spawnParticle(Particle.GUST_EMITTER_LARGE, seeker.getLocation(), 2);
            world.spawnParticle(Particle.SONIC_BOOM, seeker.getLocation(), 1, 0, 0, 0, 0);
            world.playSound(seeker.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.2f, 1.2f);
        }

        if (ghost) {
            world.spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1, 0), 8, 0.2, 0.2, 0.2, 0.02);
            world.playSound(seeker.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.6f);
        }


        drawGrappleLine(seeker, hookLoc, techno, vine, ghost);


        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (!seeker.isOnline() || !seeker.getWorld().equals(hookLoc.getWorld()) || (ticks > 5 && seeker.isOnGround()) || ticks > 30) {

                    if (seeker.isOnline() && seeker.getWorld().equals(hookLoc.getWorld()) && seeker.getLocation().distance(hookLoc) < 3) {
                        seeker.getWorld().playSound(seeker.getLocation(), ghost ? Sound.BLOCK_CHAIN_HIT : Sound.ENTITY_WIND_CHARGE_THROW, 1f, ghost ? 0.8f : 0.5f);
                    }
                    this.cancel();
                    return;
                }


                if (vine) {
                    seeker.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, seeker.getLocation(), 3, 0.2, 0.2, 0.2, 0.02);
                } else if (ghost) {
                    seeker.getWorld().spawnParticle(Particle.SOUL, seeker.getLocation(), 3, 0.2, 0.2, 0.2, 0.02);
                    seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation(), 1, 0.03, 0.03, 0.03, 0.0);
                } else {
                    seeker.getWorld().spawnParticle(Particle.CLOUD, seeker.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                }
                if (ticks % 2 == 0) {
                    seeker.getWorld().spawnParticle(techno ? Particle.ELECTRIC_SPARK : Particle.GUST, seeker.getLocation(), 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        hook.remove();
    }

}
