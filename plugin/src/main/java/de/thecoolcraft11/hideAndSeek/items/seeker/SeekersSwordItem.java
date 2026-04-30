package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.AutoAimPerk;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.HitDisplayPerk;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.*;

@SuppressWarnings("UnstableApiUsage")
public class SeekersSwordItem implements GameItem {
    public static final String ID = "has_seeker_sword";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Seeker's Blade", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Hunt down the hiders!", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            sword.setItemMeta(meta);
            sword.setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks().addDamageReduction(DamageReduction.damageReduction().base(0).factor(0).horizontalBlockingAngle(0.1f).build()).build());
        }

        return sword;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Main weapon: hold block to charge and throw your sword.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int swordCooldown = plugin.getSettingRegistry().get("seeker-items.sword-of-seeking.cooldown", 5);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withCraftPrevention(true)
                .withDropPrevention(true)
                .cancelDefaultAction(false)
                .withVanillaCooldown(swordCooldown * 20)
                .withCustomCooldown(swordCooldown * 1000L)
                .cancelAttackOnCooldown(false)
                .withAction(ItemActionType.BLOCK_START, context -> startSwordCharge(context, plugin))
                .withAction(ItemActionType.BLOCK_RELEASE, context -> releaseSwordCharge(context, plugin))
                .build());
    }

    private static boolean isSwordTarget(Player seeker, Entity entity) {
        return entity instanceof Player target
                && !target.getUniqueId().equals(seeker.getUniqueId())
                && HideAndSeek.getDataController().getHiders().contains(target.getUniqueId());
    }

    private void releaseSwordCharge(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        int maxChargeSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-charge-seconds", 5));
        long maxChargeMs = maxChargeSeconds * 1000L;

        long measuredHoldMs = Math.max(0L, context.getHoldDurationMs());

        clearSwordCharge(seeker);

        double chargeRatio = Math.min(1.0, measuredHoldMs / (double) maxChargeMs);
        throwChargedSword(seeker, plugin, chargeRatio);
    }

    private static void clearSwordCharge(Player player) {
        BukkitTask existingTask = swordChargeTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        BukkitTask xpTask = swordChargeXpTasks.remove(player.getUniqueId());
        XpProgressHelper.SavedXp savedXp = swordChargeXp.remove(player.getUniqueId());
        XpProgressHelper.stopAndRestore(player, xpTask, savedXp);
    }

    public static void cleanupSwordCharge(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            clearSwordCharge(player);
            return;
        }

        BukkitTask existingTask = swordChargeTasks.remove(playerId);
        if (existingTask != null) existingTask.cancel();

        BukkitTask xpTask = swordChargeXpTasks.remove(playerId);
        if (xpTask != null) xpTask.cancel();

        swordChargeXp.remove(playerId);
    }

    private static Entity raycastHiderHit(Player seeker, HideAndSeek plugin, Location previous, Vector direction, double distance, double hitbox) {
        World world = previous.getWorld();
        if (world == null) {
            return null;
        }

        if (plugin.getNmsAdapter().hasCapability(NmsCapabilities.PROJECTILE_ENTITY_RAYCAST)) {
            Entity hitEntity = plugin.getNmsAdapter().raycastEntityHit(
                    seeker,
                    previous,
                    direction,
                    distance,
                    hitbox,
                    entity -> isSwordTarget(seeker, entity)
            );
            if (hitEntity != null) {
                return hitEntity;
            }
        }

        RayTraceResult entityTrace = world.rayTraceEntities(
                previous,
                direction,
                distance,
                hitbox,
                entity -> isSwordTarget(seeker, entity)
        );
        return entityTrace == null ? null : entityTrace.getHitEntity();
    }

    private static Vector steerVelocityTowardHider(Location from, Vector velocity, HideAndSeek plugin, double hitbox) {
        if (velocity.lengthSquared() <= 1.0E-6) {
            return velocity;
        }

        Player target = findAutoAimTarget(from, velocity, 32.0 + (hitbox * 8.0));
        if (target == null) {
            return velocity;
        }

        Vector toTarget = target.getLocation().add(0.0, 1.0, 0.0).toVector().subtract(from.toVector());
        if (toTarget.lengthSquared() <= 1.0E-6) {
            return velocity;
        }

        double steeringStrength = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.auto-aim-strength",
                0.22);
        steeringStrength = Math.clamp(steeringStrength, 0.01, 0.6);

        double speed = velocity.length();
        Vector blended = velocity.clone().normalize().multiply(1.0 - steeringStrength)
                .add(toTarget.normalize().multiply(steeringStrength));
        if (blended.lengthSquared() <= 1.0E-6) {
            return velocity;
        }
        return blended.normalize().multiply(speed);
    }

    private static Player findAutoAimTarget(Location from, Vector velocity, double maxRange) {
        if (velocity.lengthSquared() <= 1.0E-6) {
            return null;
        }

        Vector forward = velocity.clone().normalize();
        Player best = null;
        double bestScore = Double.MAX_VALUE;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || hider.isDead() || hider.getGameMode() == GameMode.SPECTATOR) {
                continue;
            }
            hider.getWorld();
            if (!hider.getWorld().equals(from.getWorld())) {
                continue;
            }

            Vector toTarget = hider.getLocation().add(0.0, 1.0, 0.0).toVector().subtract(from.toVector());
            double distance = toTarget.length();
            if (distance <= 0.001 || distance > maxRange) {
                continue;
            }

            double angle = Math.toDegrees(forward.angle(toTarget));
            if (Double.isNaN(angle) || angle > 75.0) {
                continue;
            }

            double score = distance + (angle * 0.75);
            if (score < bestScore) {
                bestScore = score;
                best = hider;
            }
        }

        return best;
    }

    private void startSwordCharge(ItemInteractionContext context, HideAndSeek plugin) {
        context.skipCooldown();
        Player seeker = context.getPlayer();
        plugin.getLogger().info("Test");
        int maxChargeSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-charge-seconds", 5));
        long maxChargeMs = maxChargeSeconds * 1000L;
        double minSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.min-speed", 0.8);
        double maxSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-speed", 2.4);
        double gravity = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.gravity", 0.035);
        double hitbox = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.hitbox", 0.4);
        int maxFlightSeconds = Math.max(1,
                plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-flight-seconds", 6));

        clearSwordCharge(seeker);

        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(seeker);
        swordChargeXp.put(seeker.getUniqueId(), savedXp);

        BukkitTask xpTask = XpProgressHelper.start(plugin, seeker, maxChargeSeconds * 20L, XpProgressHelper.Mode.COUNTUP, 10);
        swordChargeXpTasks.put(seeker.getUniqueId(), xpTask);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            final long startTime = System.currentTimeMillis();
            long nextSoundAtMs = startTime;
            long lastPreviewBucket = -1L;

            @Override
            public void run() {
                if (!seeker.isOnline() || !plugin.getCustomItemManager().hasItemInMainHand(seeker, getId())) {
                    clearSwordCharge(seeker);
                    return;
                }

                long now = System.currentTimeMillis();
                long elapsed = now - startTime;
                double progress = Math.min(1.0, (double) elapsed / maxChargeMs);

                if (now >= nextSoundAtMs && progress < 1.0) {
                    float pitch = 0.5f + ((float) progress * 1.5f);
                    seeker.playSound(seeker.getLocation(), Sound.BLOCK_COMPARATOR_CLICK, 0.8f, pitch);

                    long delay = (long) (1000 - (progress * 900));
                    nextSoundAtMs = now + Math.max(100, delay);
                }

                if (plugin.getPerkStateManager().hasPurchased(seeker.getUniqueId(), HitDisplayPerk.ID)) {
                    long previewBucket = elapsed / 100L;
                    if (previewBucket != lastPreviewBucket) {
                        lastPreviewBucket = previewBucket;
                        renderSwordPreview(seeker, plugin, progress, minSpeed, maxSpeed, gravity, hitbox,
                                maxFlightSeconds);
                    }
                }
            }
        }, 1L, 1L);

        swordChargeTasks.put(seeker.getUniqueId(), task);
    }

    private void throwChargedSword(Player seeker, HideAndSeek plugin, double chargeRatio) {
        if (!seeker.isOnline()) {
            return;
        }

        double minSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.min-speed", 0.8);
        double maxSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-speed", 2.4);
        double gravity = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.gravity", 0.035);
        int maxFlightSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-flight-seconds", 6));
        int stuckSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.stuck-seconds", 12));
        double hitbox = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.hitbox", 0.4);
        boolean autoAim = plugin.getPerkStateManager().hasPurchased(seeker.getUniqueId(), AutoAimPerk.ID);

        double speed = minSpeed + (maxSpeed - minSpeed) * chargeRatio;
        boolean energyBlade = ItemSkinSelectionService.isSelected(seeker, ID, "skin_energy_blade");
        boolean banHammer = ItemSkinSelectionService.isSelected(seeker, ID, "skin_the_ban_hammer");
        boolean giantSpatula = ItemSkinSelectionService.isSelected(seeker, ID, "skin_giant_spatula");
        Location eyeLocation = seeker.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        Location start = eyeLocation.clone().add(lookDirection.clone().multiply(0.6)).add(0, -0.2, 0);
        final Vector[] velocity = {lookDirection.multiply(speed)};

        ItemStack swordDisplayItem = getSwordDisplayItem(seeker, plugin);
        ItemDisplay swordDisplay = start.getWorld().spawn(start, ItemDisplay.class, display -> {
            display.setItemStack(swordDisplayItem);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setInterpolationDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.15f, -0.12f, -0.15f),
                    new AxisAngle4f((float) Math.toRadians(90), 1f, 0f, 0f),
                    new Vector3f(0.7f, 0.7f, 0.7f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
        });

        seeker.getWorld().playSound(seeker.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, (float) (0.8 + chargeRatio * 0.5));
        if (energyBlade) {
            seeker.getWorld().playSound(seeker.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.55f, 1.5f);
        } else if (banHammer) {
            seeker.getWorld().playSound(seeker.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.45f, 0.7f);
        } else if (giantSpatula) {
            seeker.getWorld().playSound(seeker.getLocation(), Sound.ITEM_ARMOR_EQUIP_CHAIN, 0.45f, 1.6f);
        }

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;
            float rotationAngle = 0f;

            @Override
            public void run() {
                if (!seeker.isOnline() || !swordDisplay.isValid()) {
                    removeDisplay();
                    cancel();
                    return;
                }

                if (ticks++ > maxFlightSeconds * 20) {
                    removeDisplay();
                    cancel();
                    return;
                }

                Location previous = current.clone();
                if (autoAim) {
                    velocity[0] = steerVelocityTowardHider(previous, velocity[0], plugin, hitbox);
                }
                velocity[0].setY(velocity[0].getY() - gravity);
                current = current.add(velocity[0]);

                World world = current.getWorld();
                Vector travel = current.toVector().subtract(previous.toVector());
                double distance = travel.length();
                if (distance <= 0.0001) {
                    return;
                }
                Vector direction = travel.clone().normalize();

                Entity hitEntity = raycastHiderHit(seeker, plugin, previous, direction, distance, hitbox);

                if (hitEntity instanceof Player target) {
                    double damage = getThrownSwordDamage(seeker);
                    target.damage(damage, seeker);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.25, 0.4, 0.25, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                    seeker.playSound(seeker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.1f);
                    if (energyBlade) {
                        target.getWorld().spawnParticle(Particle.END_ROD, target.getLocation().add(0, 1, 0), 10, 0.2, 0.3, 0.2, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.45f, 1.5f);
                    } else if (banHammer) {
                        target.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, target.getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.01);
                        target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.5f, 0.8f);
                    } else if (giantSpatula) {
                        target.getWorld().spawnParticle(Particle.CLOUD, target.getLocation().add(0, 1, 0), 10, 0.22, 0.3, 0.22, 0.02);
                        target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.45f, 1.25f);
                    }

                    removeDisplay();
                    cancel();
                    return;
                }

                RayTraceResult blockTrace = world.rayTraceBlocks(
                        previous,
                        direction,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                );

                if (blockTrace != null) {
                    Block hitBlock = blockTrace.getHitBlock();
                    if (hitBlock != null) {
                        boolean gazeKill = plugin.getSettingRegistry().get("game.seekers.kill-mode").equals("GAZE_KILL");
                        plugin.getBlockModeListener().damageHiddenPlayer(seeker, hitBlock, gazeKill);
                    }

                    Vector hitNormal = blockTrace.getHitBlockFace() != null ?
                            blockTrace.getHitBlockFace().getDirection() : new Vector(0, 1, 0);

                    Location impact = blockTrace.getHitPosition().toLocation(world);
                    impact.add(hitNormal.clone().multiply(0.175));
                    impact.setYaw(rotationAngle + 180f);
                    impact.setPitch(0);


                    Vector bladeDirection = hitNormal.clone().multiply(-1);


                    float targetPitch = (float) Math.toDegrees(Math.asin(-bladeDirection.getY()));
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-bladeDirection.getX(), bladeDirection.getZ()));


                    Quaternionf stuckRotation = new Quaternionf()
                            .rotateX((float) Math.toRadians(90))
                            .rotateY((float) Math.toRadians(targetYaw))
                            .rotateX((float) Math.toRadians(targetPitch))
                            .rotateZ((float) Math.toRadians(rotationAngle));

                    swordDisplay.setTransformation(new Transformation(
                            new Vector3f(-0.15f, -0.12f, -0.15f),
                            new AxisAngle4f(stuckRotation),
                            new Vector3f(0.7f, 0.7f, 0.7f),
                            new AxisAngle4f(0, 0, 0, 0)
                    ));

                    swordDisplay.teleport(impact);
                    swordDisplay.setInterpolationDelay(0);
                    swordDisplay.setInterpolationDuration(3);
                    world.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 0.9f);
                    if (energyBlade) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, impact.clone().add(0, 0.2, 0), 14, 0.18, 0.18, 0.18, 0.03);
                        world.playSound(impact, Sound.BLOCK_BEACON_ACTIVATE, 0.45f, 1.45f);
                    } else if (banHammer) {
                        world.spawnParticle(Particle.BLOCK, impact.clone().add(0, 0.2, 0), 14, 0.2, 0.2, 0.2,
                                Material.IRON_BLOCK.createBlockData());
                        world.playSound(impact, Sound.BLOCK_ANVIL_BREAK, 0.4f, 0.85f);
                    } else if (giantSpatula) {
                        world.spawnParticle(Particle.BUBBLE_POP, impact.clone().add(0, 0.2, 0), 14, 0.2, 0.2, 0.2, 0.04);
                        world.playSound(impact, Sound.ITEM_BUCKET_EMPTY, 0.35f, 1.4f);
                    }

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (swordDisplay.isValid()) {
                            swordDisplay.remove();
                        }
                    }, stuckSeconds * 20L);

                    cancel();
                    return;
                }

                rotationAngle += 30f;
                if (rotationAngle >= 360f) {
                    rotationAngle -= 360f;
                }

                float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));

                Quaternionf rotation = new Quaternionf()
                        .rotateY((float) Math.toRadians(yaw))
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateZ((float) Math.toRadians(rotationAngle));

                swordDisplay.setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.12f, -0.15f),
                        new AxisAngle4f(rotation),
                        new Vector3f(0.7f, 0.7f, 0.7f),
                        new AxisAngle4f(0, 0, 0, 0)
                ));

                swordDisplay.teleport(current);
                world.spawnParticle(Particle.SWEEP_ATTACK, current, 1, 0, 0, 0, 0);
                if (energyBlade) {
                    world.spawnParticle(Particle.END_ROD, current, 1, 0.02, 0.02, 0.02, 0.0);
                } else if (banHammer) {
                    world.spawnParticle(Particle.CRIT, current, 1, 0.03, 0.03, 0.03, 0.0);
                } else if (giantSpatula) {
                    world.spawnParticle(Particle.CLOUD, current, 1, 0.03, 0.03, 0.03, 0.0);
                }
            }

            private void removeDisplay() {
                if (swordDisplay.isValid()) {
                    swordDisplay.remove();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void renderSwordPreview(Player seeker, HideAndSeek plugin, double chargeRatio, double minSpeed, double maxSpeed,
                                    double gravity, double hitbox, int maxFlightSeconds) {
        World world = seeker.getWorld();

        double speed = minSpeed + (maxSpeed - minSpeed) * chargeRatio;
        Location eyeLocation = seeker.getEyeLocation();
        Vector lookDirection = eyeLocation.getDirection().normalize();
        Location current = eyeLocation.clone().add(lookDirection.clone().multiply(0.6)).add(0, -0.2, 0);
        Vector velocity = lookDirection.multiply(speed);
        boolean autoAim = plugin.getPerkStateManager().hasPurchased(seeker.getUniqueId(), AutoAimPerk.ID);

        for (int ticks = 0; ticks < maxFlightSeconds * 20; ticks++) {
            Location previous = current.clone();
            if (autoAim) {
                velocity = steerVelocityTowardHider(previous, velocity, plugin, hitbox);
            }
            velocity.setY(velocity.getY() - gravity);
            current = current.add(velocity);

            Vector travel = current.toVector().subtract(previous.toVector());
            double distance = travel.length();
            if (distance <= 0.0001) {
                continue;
            }

            Vector direction = travel.clone().normalize();
            Entity hitEntity = raycastHiderHit(seeker, plugin, previous, direction, distance, hitbox);
            if (hitEntity != null) {
                Location impact = hitEntity.getLocation().add(0, 1, 0);
                world.spawnParticle(Particle.ENCHANTED_HIT, impact, 8, 0.12, 0.12, 0.12, 0.01);
                world.spawnParticle(Particle.SWEEP_ATTACK, impact, 2, 0.08, 0.08, 0.08, 0.0);
                return;
            }

            RayTraceResult blockTrace = world.rayTraceBlocks(previous, direction, distance, FluidCollisionMode.NEVER,
                    true);
            if (blockTrace != null) {
                Location impact = blockTrace.getHitPosition().toLocation(world);
                world.spawnParticle(Particle.ENCHANTED_HIT, impact, 8, 0.12, 0.12, 0.12, 0.01);
                world.spawnParticle(Particle.CRIT, impact, 6, 0.15, 0.15, 0.15, 0.02);
                return;
            }

            for (double sample = 0.0; sample <= distance; sample += Math.max(0.18, distance / 4.0)) {
                Location point = previous.clone().add(direction.clone().multiply(sample));
                world.spawnParticle(Particle.ENCHANTED_HIT, point, 1, 0.01, 0.01, 0.01, 0.0);
                if (autoAim) {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, point, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
    }

    private ItemStack getSwordDisplayItem(Player seeker, HideAndSeek plugin) {
        String selected = ItemSkinSelectionService.getSelectedVariant(seeker, SeekersSwordItem.ID);
        if (selected != null) {
            var variant = plugin.getCustomItemManager().getVariantManager().getVariant(SeekersSwordItem.ID, selected);
            if (variant != null) {
                return variant.getItemStack().clone();
            }
        }
        return createItem(plugin);
    }

    private static double getThrownSwordDamage(Player seeker) {
        double damage = 6.0;
        var attackAttr = seeker.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) {
            damage = attackAttr.getValue();
        }

        ItemStack hand = seeker.getInventory().getItemInMainHand();
        int sharpnessLevel = hand.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpnessLevel > 0) {
            damage += sharpnessLevel * 0.5;
        }
        return damage;
    }
}
