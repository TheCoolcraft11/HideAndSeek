package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public class ChainPullItem implements GameItem {
    public static final String ID = "has_seeker_chain_pull";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.LEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Chain Pull", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to pull hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Pull the hider in front of you to your position.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int chainCooldown = plugin.getSettingRegistry().get("seeker-items.chain-pull.cooldown", 12);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> chainPull(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> chainPull(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(chainCooldown * 20)
                .withCustomCooldown(chainCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .build());
    }

    private static void chainPull(Player seeker, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("seeker-items.chain-pull.range", 30.0);
        double power = plugin.getSettingRegistry().get("seeker-items.chain-pull.pull-power", 2.0);
        double cosThreshold = Math.cos(Math.toRadians(30));

        Vector dir = seeker.getLocation().getDirection().normalize();
        Player target = null;
        double closest = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;

            Vector toHider = hider.getLocation().toVector().subtract(seeker.getLocation().toVector());
            double dist = toHider.length();
            if (dist > range) continue;

            double cos = dir.dot(toHider.normalize());
            if (cos < cosThreshold) continue;

            if (dist < closest) {
                closest = dist;
                target = hider;
            }
        }

        if (target == null) {
            seeker.sendMessage(Component.text("No hider in range.", NamedTextColor.RED));
            return;
        }

        final Player finalTarget = target;

        Vector direction = seeker.getLocation().getDirection().normalize();
        Location targetLocation = seeker.getLocation().add(direction.multiply(1.5));
        targetLocation.setYaw(seeker.getLocation().getYaw());
        targetLocation.setPitch(0);


        final Location finalTargetLocation = findSafeLandingLocation(targetLocation, seeker.getWorld());

        int slownessDuration = plugin.getSettingRegistry().get("seeker-items.chain-pull.slowness-duration", 3);
        int pullTicks = 8;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                try {
                    if (!finalTarget.isOnline() || !finalTarget.getWorld().equals(seeker.getWorld())) {
                        cancel();
                        return;
                    }


                    drawChainParticles(seeker, finalTarget);

                    Location current = finalTarget.getLocation();
                    Vector toTarget = finalTargetLocation.toVector().subtract(current.toVector());
                    double distance = toTarget.length();

                    if (distance < 0.6 || ticks >= pullTicks) {
                        Location safeLanding = findSafeLandingLocation(finalTarget.getLocation(), seeker.getWorld());
                        finalTarget.teleport(safeLanding);
                        finalTarget.setVelocity(new Vector(0, 0, 0));
                        finalTarget.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS,
                                slownessDuration * 20,
                                2,
                                false,
                                false,
                                false
                        ));
                        seeker.sendMessage(Component.text("Pulled " + finalTarget.getName() + "!", NamedTextColor.GREEN));
                        finalTarget.sendMessage(Component.text("You've been pulled by a chain!", NamedTextColor.DARK_GRAY));
                        cancel();
                        return;
                    }

                    Vector velocity = toTarget.normalize().multiply(Math.min(power, distance));
                    finalTarget.setVelocity(velocity);
                    ticks++;
                } catch (Exception e) {

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void drawChainParticles(Player seeker, Player hider) {
        Location start = seeker.getEyeLocation().subtract(0, 0.3, 0);
        Location end = hider.getEyeLocation().subtract(0, 0.3, 0);

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1) return;

        World world = start.getWorld();
        int steps = (int) (distance / 0.35);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;

            Location point = new Location(world,
                    start.getX() + dx * t,
                    start.getY() + dy * t,
                    start.getZ() + dz * t);


            int r = (int) (60 + (255 - 60) * t);
            int g = (int) (140 + (80 - 140) * t);
            int b = (int) (255 + (10 - 255) * t);
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(r, g, b), 0.75f));


            if (i % 2 == 0) {
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.3f));
            }


            if (Math.random() < 0.07) {
                world.spawnParticle(Particle.CRIT, point, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }


        world.spawnParticle(Particle.DUST, start, 4, 0.08, 0.08, 0.08, 0,
                new Particle.DustOptions(Color.fromRGB(60, 140, 255), 1.1f));
        world.spawnParticle(Particle.END_ROD, start, 1, 0.04, 0.04, 0.04, 0.003);

        world.spawnParticle(Particle.DUST, end, 4, 0.08, 0.08, 0.08, 0,
                new Particle.DustOptions(Color.fromRGB(255, 80, 10), 1.1f));
        world.spawnParticle(Particle.END_ROD, end, 1, 0.04, 0.04, 0.04, 0.003);
    }

    private static Location findSafeLandingLocation(Location loc, World world) {
        Location checkLoc = loc.clone();


        Block block;


        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            block = world.getBlockAt(checkLoc);
            if (!block.getType().isSolid() && (i == 0 || world.getBlockAt(checkLoc.clone().add(0, -1, 0)).getType().isSolid())) {
                return checkLoc;
            }
            checkLoc.add(0, 1, 0);
        }


        checkLoc = loc.clone();
        Block below = world.getBlockAt(checkLoc.clone().add(0, -0.5, 0));
        if (!below.getType().isSolid()) {

            while (checkLoc.getY() > world.getMinHeight()) {
                below = world.getBlockAt(checkLoc.clone().add(0, -1, 0));
                if (below.getType().isSolid()) {
                    checkLoc.add(0, 1, 0);
                    break;
                }
                checkLoc.add(0, -1, 0);
            }
        }

        return checkLoc;
    }
}
