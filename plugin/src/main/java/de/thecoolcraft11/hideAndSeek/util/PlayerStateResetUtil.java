package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem;
import de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

public final class PlayerStateResetUtil {

    private static final double DEFAULT_HEALTH = 20.0;
    private static final int DEFAULT_FOOD_LEVEL = 20;
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;
    private static final double DEFAULT_SCALE = 1.0;
    private static final float DEFAULT_EXP = 0.0f;
    private static final int DEFAULT_LEVEL = 0;


    public static void resetPlayerCompletely(Player player, boolean clearInventory) {
        resetAttributes(player);
        resetGameState(player, clearInventory);
        resetStatus(player);
        resetCosmetics(player);
        resetCooldowns(player);
        resetPermissions(player);
        resetUses(player);
    }

    public static void resetPlayerForSpectator(Player player, boolean clearInventory) {
        resetPlayerCompletely(player, clearInventory);
        player.setGameMode(GameMode.SPECTATOR);
        player.setAllowFlight(true);
        player.setFlying(true);
        resetPermissions(player).setPermission("has.spectator", true);
    }

    private static void resetGameState(Player player, boolean clearInventory) {
        player.setGameMode(GameMode.SURVIVAL);

        player.setFoodLevel(DEFAULT_FOOD_LEVEL);
        player.setSaturation(20f);
        player.setExhaustion(0f);

        player.setExp(DEFAULT_EXP);
        player.setLevel(DEFAULT_LEVEL);

        double maxHealth = getAttributeValue(player, Attribute.MAX_HEALTH, DEFAULT_HEALTH);
        player.setHealth(maxHealth);
        player.setAbsorptionAmount(0);

        if (clearInventory) {
            player.getInventory().clear();
        }
    }


    private static void resetAttributes(Player player) {
        setAttribute(player, Attribute.MAX_HEALTH, DEFAULT_HEALTH);
        setAttribute(player, Attribute.SCALE, DEFAULT_SCALE);


        player.setAllowFlight(false);
        player.setFlying(false);
        player.setGliding(false);
        player.setInvulnerable(false);
        player.setCollidable(true);
        player.setSneaking(false);
        player.setSprinting(false);

        player.setWalkSpeed(DEFAULT_WALK_SPEED);
        player.setFlySpeed(DEFAULT_FLY_SPEED);

        player.clearActivePotionEffects();

        player.setVelocity(new Vector(0, 0, 0));
    }

    private static void setAttribute(Player player, Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null) {
            instance.setBaseValue(value);
        }
    }

    private static double getAttributeValue(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        return instance != null ? instance.getValue() : fallback;
    }


    private static void resetStatus(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }

        player.setFireTicks(0);
        player.setFallDistance(0f);
        player.setRemainingAir(player.getMaximumAir());
        player.setFreezeTicks(0);

        player.setHealthScale(20.0);
        player.setHealthScaled(false);
    }


    private static void resetCosmetics(Player player) {
        player.setGlowing(false);
        player.setInvisible(false);
        player.setSilent(false);
    }

    private static PermissionAttachment resetPermissions(Player player) {
        PermissionAttachment attachment = HideAndSeek.getDataController().getAttachment(player);
        attachment.unsetPermission("hideandseek.role.hider");
        attachment.unsetPermission("hideandseek.role.seeker");
        attachment.unsetPermission("hideandseek.role.spectator");
        return attachment;
    }

    private static void resetCooldowns(Player player) {
        MinigameFramework.getActiveInstance().getCustomItemManager().getAllItems().forEach(item -> {
            item.resetPlayerCooldowns(player.getUniqueId());
            item.resetPlayerUses(player.getUniqueId());
        });
    }

    private static void resetUses(Player player) {
        SpeedBoostItem.resetSpeedLevel(player.getUniqueId());
        KnockbackStickItem.resetKnockbackLevel(player.getUniqueId());
    }
}
