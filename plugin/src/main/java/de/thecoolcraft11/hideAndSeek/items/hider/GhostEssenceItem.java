package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.model.GhostEssenceParticleMode;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.ghostEssenceXpTasks;

public class GhostEssenceItem implements GameItem {
    public static final String ID = "has_hider_ghost_essence";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        float duration = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-duration", 1.5f);
        if (meta != null) {
            meta.displayName(Component.text("Ghostly Essence", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Pass through walls for " + duration + " seconds", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("You cannot descend while ghostly!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-duration", 1.5f);
        return String.format("Become ghostly to phase through blocks for %.1fs.", duration.floatValue());
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cooldown = plugin.getSettingRegistry().get("hider-items.ghost-essence.cooldown", 25);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, ctx -> useGhostEssence(ctx.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, ctx -> useGhostEssence(ctx.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(cooldown * 20)
                .withCustomCooldown(cooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void spawnGhostFlightAura(Player player, int ticks, boolean spectral, boolean digital) {
        Location aura = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.SOUL, aura, 3, 0.1, 0.1, 0.1, 0.02);
        if (spectral) {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, aura, 3, 0.14, 0.2, 0.14, 0.01);
            if (ticks % 10 == 0) {
                player.getWorld().spawnParticle(Particle.TRIAL_OMEN, aura, 2, 0.2, 0.25, 0.2, 0.01);
            }
        } else if (digital) {
            player.getWorld().spawnParticle(Particle.DUST, aura, 3, 0.12, 0.18, 0.12,
                    new Particle.DustOptions(Color.AQUA, 1.0f));
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, aura, 2, 0.12, 0.15, 0.12, 0.01);
            if (ticks % 10 == 0) {
                player.getWorld().spawnParticle(Particle.END_ROD, aura, 2, 0.2, 0.25, 0.2, 0.01);
            }
        }
    }

    private static void spawnGhostAuraBurst(Player player, Location loc, boolean spectral, boolean digital, boolean activation) {
        if (spectral) {
            player.getWorld().spawnParticle(Particle.SOUL, loc, activation ? 20 : 16, 0.35, 0.45, 0.35, 0.03);
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, activation ? 16 : 12, 0.28, 0.38, 0.28, 0.015);
            player.getWorld().spawnParticle(Particle.TRIAL_OMEN, loc, activation ? 6 : 4, 0.25, 0.3, 0.25, 0.01);
        } else if (digital) {
            player.getWorld().spawnParticle(Particle.END_ROD, loc, activation ? 18 : 14, 0.28, 0.38, 0.28, 0.02);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, activation ? 20 : 14, 0.3, 0.4, 0.3, 0.02);
            player.getWorld().spawnParticle(Particle.DUST, loc, activation ? 10 : 8, 0.22, 0.3, 0.22,
                    new Particle.DustOptions(Color.fromRGB(95, 250, 255), 1.1f));
        }
    }

    private static void spawnSnapLine(Location from, Location to) {
        if (!from.getWorld().equals(to.getWorld())) return;

        Vector direction = to.toVector().subtract(from.toVector());
        double length = direction.length();
        if (length < 0.001) {
            from.getWorld().spawnParticle(Particle.SOUL, from.clone().add(0, 1, 0), 8, 0.1, 0.1, 0.1, 0.02);
            return;
        }

        Vector step = direction.clone().normalize().multiply(0.25);
        int steps = (int) Math.ceil(length / 0.25);
        Location current = from.clone().add(0, 1, 0);

        for (int i = 0; i <= steps; i++) {
            from.getWorld().spawnParticle(Particle.SOUL, current, 1, 0.0, 0.0, 0.0, 0.0);
            current.add(step);
        }
    }

    private void cleanupGhostModeOffline(java.util.UUID playerId, org.bukkit.entity.Zombie ghost,
                                         boolean restoreBlockDisplayVisible) {
        if (ghost != null && ghost.isValid()) {
            ghost.remove();
        }

        var blockDisplay = HideAndSeek.getDataController().getBlockDisplay(playerId);
        if (blockDisplay != null && blockDisplay.isValid()) {
            blockDisplay.setVisibleByDefault(restoreBlockDisplayVisible);
        }

        HideAndSeek.getDataController().removeAllowedSpectator(playerId);
    }

    private void useGhostEssence(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) return;

        final Location startLoc = player.getLocation().clone();
        int maxRadius = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-radius", 15);
        int minLightBlock = plugin.getSettingRegistry().get("hider-items.ghost-essence.min-light-block", 1);
        int minLightSky = plugin.getSettingRegistry().get("hider-items.ghost-essence.min-light-sky", 1);
        float flyingSpeed = plugin.getSettingRegistry().get("hider-items.ghost-essence.flying-speed", 0.01f);
        float maxDurationSeconds = plugin.getSettingRegistry().get("hider-items.ghost-essence.max-duration", 1.5f);
        float boostPower = plugin.getSettingRegistry().get("hider-items.ghost-essence.boost-power", 1.5f);
        GhostEssenceParticleMode particleMode = plugin.getSettingRegistry().get(
                "hider-items.ghost-essence.particle-mode", GhostEssenceParticleMode.FLYING);
        boolean spectral = ItemSkinSelectionService.isSelected(player, ID, "skin_spectral_form");
        boolean digital = ItemSkinSelectionService.isSelected(player, ID, "skin_digital_phase");

        HideAndSeek.getDataController().addAllowedSpectator(player.getUniqueId());
        plugin.getNmsAdapter().setServerGameModeSpectator(player);
        plugin.getNmsAdapter().spoofClientGameMode(player, GameMode.SURVIVAL);

        player.setAllowFlight(true);
        player.setFlying(true);
        player.setFlySpeed(flyingSpeed);

        Vector rawDir = player.getLocation().getDirection();
        final Vector boostVector = new Vector(rawDir.getX(), Math.max(0, rawDir.getY()), rawDir.getZ())
                .normalize().multiply(boostPower);

        player.sendMessage(Component.text("You are now a Ghost! Phasing enabled for " + maxDurationSeconds + "s.", NamedTextColor.AQUA));
        player.playSound(player.getLocation(), Sound.ENTITY_GHAST_WARN, 1f, 1.2f);
        if (spectral) {
            player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, startLoc.clone().add(0, 1, 0), 22, 0.35, 0.5, 0.35, 0.01);
            player.playSound(startLoc, Sound.PARTICLE_SOUL_ESCAPE, 0.7f, 0.9f);
        } else if (digital) {
            player.getWorld().spawnParticle(Particle.END_ROD, startLoc.clone().add(0, 1, 0), 18, 0.3, 0.45, 0.3, 0.03);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, startLoc.clone().add(0, 1, 0), 16, 0.28, 0.4, 0.28, 0.02);
            player.playSound(startLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.65f, 1.5f);
        }
        spawnGhostAuraBurst(player, startLoc.clone().add(0, 1, 0), spectral, digital, true);

        org.bukkit.entity.Zombie ghost = startLoc.getWorld().spawn(startLoc, org.bukkit.entity.Zombie.class, s -> {
            s.setAI(true);
            s.setInvisible(true);
            s.setPersistent(true);
            s.setSilent(true);
            s.setCollidable(false);
            s.setInvulnerable(true);
        });
        Bukkit.getOnlinePlayers().forEach(p -> p.hideEntity(plugin, ghost));


        BukkitTask prevTask = ghostEssenceXpTasks.remove(player.getUniqueId());
        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(player);
        XpProgressHelper.stopAndClear(player, prevTask);
        long durationTicks = Math.max(1L, Math.round(maxDurationSeconds * 20f));
        BukkitTask xpTask = XpProgressHelper.start(plugin, player, durationTicks, XpProgressHelper.Mode.COUNTDOWN,
                (int) Math.ceil(maxDurationSeconds));
        ghostEssenceXpTasks.put(player.getUniqueId(), xpTask);


        boolean restoreBlockDisplayVisible = true;
        var modeResult = plugin.getSettingService().getSetting("game.mode");
        Object modeObj = modeResult.isSuccess() ? modeResult.getValue() : null;
        if ("BLOCK".equals(modeObj != null ? modeObj.toString() : "")) {
            var blockDisplay = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (blockDisplay != null && blockDisplay.isValid()) {
                restoreBlockDisplayVisible = blockDisplay.isVisibleByDefault();
                blockDisplay.setVisibleByDefault(false);
            }
        }
        final boolean finalRestoreBlockDisplayVisible = restoreBlockDisplayVisible;

        new BukkitRunnable() {
            int ticks = 0;
            Location lastValidLoc = player.getLocation().clone();

            final double maxMovePerTick = (flyingSpeed * 12.0) + 0.15;
            final double boostThresholdSq = Math.pow(boostPower + 0.2, 2);
            final double normalThresholdSq = Math.pow(maxMovePerTick, 2);

            @Override
            public void run() {
                if (!player.isOnline()) {
                    BukkitTask t = ghostEssenceXpTasks.remove(player.getUniqueId());
                    XpProgressHelper.stopAndRestore(player, t, savedXp);
                    cleanupGhostModeOffline(player.getUniqueId(), ghost, finalRestoreBlockDisplayVisible);
                    this.cancel();
                    return;
                }

                if (ticks >= durationTicks) {
                    BukkitTask t = ghostEssenceXpTasks.remove(player.getUniqueId());
                    XpProgressHelper.stopAndRestore(player, t, savedXp);
                    finalizeGhostMode(player, plugin, ghost, startLoc, maxRadius, minLightBlock, minLightSky,
                            finalRestoreBlockDisplayVisible, particleMode, spectral, digital);
                    this.cancel();
                    return;
                }

                if (ticks < 3) {
                    player.teleport(player.getLocation().add(boostVector));
                    lastValidLoc = player.getLocation().clone();
                }

                Location currentLoc = player.getLocation();
                double distSq = currentLoc.distanceSquared(lastValidLoc);
                double currentMaxSq = (ticks < 5) ? boostThresholdSq : normalThresholdSq;

                if (distSq > currentMaxSq) {
                    Vector dir = currentLoc.toVector().subtract(lastValidLoc.toVector());
                    if (dir.lengthSquared() > 0.001) {
                        dir.setY(Math.max(0, dir.getY()));
                        dir.normalize();

                        Location clampedLoc = lastValidLoc.clone().add(dir.multiply(Math.sqrt(currentMaxSq)));
                        clampedLoc.setYaw(currentLoc.getYaw());
                        clampedLoc.setPitch(currentLoc.getPitch());

                        player.teleport(clampedLoc);
                        lastValidLoc = clampedLoc;
                    }
                } else {
                    lastValidLoc = currentLoc.clone();
                }

                if (currentLoc.distanceSquared(startLoc) > (maxRadius * maxRadius)) {
                    Vector dirFromStart = currentLoc.toVector().subtract(startLoc.toVector()).normalize();
                    Location boundary = startLoc.clone().add(dirFromStart.multiply(maxRadius - 0.2));
                    boundary.setYaw(currentLoc.getYaw());
                    boundary.setPitch(currentLoc.getPitch());
                    player.teleport(boundary);
                }

                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.LEVITATION, 5, 255, false, false, false));


                if (particleMode == GhostEssenceParticleMode.FLYING && ticks % 5 == 0) {
                    spawnGhostFlightAura(player, ticks, spectral, digital);
                }

                if (ticks % 20 == 0) {
                    ghost.setTarget(player);
                    if (ticks < durationTicks - 20) ghost.teleport(startLoc);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void finalizeGhostMode(Player player, HideAndSeek plugin, org.bukkit.entity.Zombie ghost,
                                   Location startLoc, int maxRadius, int minLightB, int minLightS,
                                   boolean restoreBlockDisplayVisible, GhostEssenceParticleMode particleMode,
                                   boolean spectral, boolean digital) {

        player.setGameMode(GameMode.SURVIVAL);
        plugin.getNmsAdapter().spoofClientGameMode(player, GameMode.SURVIVAL);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.LEVITATION);

        Location adjustedLoc = findSafeMaterialization(player.getLocation());
        player.teleport(adjustedLoc);

        boolean isCheating = false;
        String reason = "";

        if (!player.getWorld().getWorldBorder().isInside(adjustedLoc)) {
            isCheating = true;
            reason = "You cannot materialize outside the world border!";
        } else if (adjustedLoc.distance(startLoc) > maxRadius + 1.5) {
            isCheating = true;
            reason = "You wandered too far from your physical body!";
        } else if (adjustedLoc.getBlock().getType().isSolid() || player.getEyeLocation().getBlock().getType().isSolid()) {
            isCheating = true;
            reason = "You materialized inside a wall!";
        } else if (adjustedLoc.getBlock().getLightLevel() < minLightB && adjustedLoc.getBlock().getLightFromSky() < minLightS) {
            isCheating = true;
            reason = "It's too dark to materialize here!";
        } else if (!canPathfindBack(plugin, ghost, startLoc, adjustedLoc)) {
            isCheating = true;
            reason = "There is no physical path to this location!";
        }

        if (isCheating) {
            player.teleport(startLoc);
            player.sendMessage(Component.text(reason, NamedTextColor.RED));
            player.playSound(startLoc, Sound.ENTITY_GHAST_HURT, 1f, 1f);
        } else {
            player.sendMessage(Component.text("You have successfully materialized!", NamedTextColor.GREEN));
            player.playSound(adjustedLoc, Sound.ENTITY_GHAST_DEATH, 1f, 1f);
        }

        Location endLoc = isCheating ? startLoc : adjustedLoc;
        if (spectral) {
            player.getWorld().spawnParticle(Particle.SOUL, endLoc.clone().add(0, 1, 0), 24, 0.35, 0.55, 0.35, 0.03);
            player.playSound(endLoc, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.45f, 0.8f);
        } else if (digital) {
            player.getWorld().spawnParticle(Particle.END_ROD, endLoc.clone().add(0, 1, 0), 22, 0.3, 0.45, 0.3, 0.02);
            player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, endLoc.clone().add(0, 1, 0), 18, 0.28, 0.4, 0.28, 0.03);
            player.playSound(endLoc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 1.5f);
        }
        spawnGhostAuraBurst(player, endLoc.clone().add(0, 1, 0), spectral, digital, false);


        if (particleMode == GhostEssenceParticleMode.SNAP) {
            spawnSnapLine(startLoc, endLoc);
        }

        if (ghost.isValid()) ghost.remove();


        var blockDisplay = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        if (blockDisplay != null && blockDisplay.isValid()) {
            blockDisplay.setVisibleByDefault(restoreBlockDisplayVisible);
        }

        HideAndSeek.getDataController().removeAllowedSpectator(player.getUniqueId());
    }

    private Location findSafeMaterialization(Location loc) {
        Location best = loc.clone();


        if (!best.getBlock().getType().isSolid()) {
            int dropLimit = 4;
            for (int i = 0; i < dropLimit; i++) {
                if (best.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) break;
                best.subtract(0, 1, 0);
            }
        }


        if (best.getBlock().getType().isSolid()) {
            for (int x = -1; x <= 1; x++) {
                for (int y = 0; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Location candidate = best.clone().add(x, y, z);

                        if (!candidate.getBlock().getType().isSolid() &&
                                !candidate.clone().add(0, 1, 0).getBlock().getType().isSolid()) {
                            return candidate.add(0.5, 0, 0.5);
                        }
                    }
                }
            }
        }

        return best;
    }


    private boolean canPathfindBack(HideAndSeek plugin, Mob ghost, Location start, Location end) {

        if (!start.getWorld().equals(end.getWorld())) return false;

        if (!plugin.getNmsAdapter().hasCapability(NmsCapabilities.MOB_PATHFINDING)) {
            return true;
        }

        return plugin.getNmsAdapter().canPathfind(ghost, start, end);
    }


    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "hider-items.ghost-essence.cooldown",
                "hider-items.ghost-essence.max-radius",
                "hider-items.ghost-essence.min-light-block",
                "hider-items.ghost-essence.min-light-sky",
                "hider-items.ghost-essence.flying-speed",
                "hider-items.ghost-essence.max-duration",
                "hider-items.ghost-essence.boost-power",
                "hider-items.ghost-essence.particle-mode"
        );
    }
}
