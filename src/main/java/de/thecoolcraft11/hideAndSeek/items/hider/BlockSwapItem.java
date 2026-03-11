package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.seeker.CurseSpellItem.isHiderCursed;

public class BlockSwapItem implements GameItem {
    public static final String ID = "has_hider_block_swap";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Block Swap", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to swap blocks with another hider", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Swap disguise blocks with the nearest other hider.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int blockSwapCooldown = plugin.getSettingRegistry().get("hider-items.block-swap.cooldown", 15);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> blockSwap(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> blockSwap(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(blockSwapCooldown * 20)
                .withCustomCooldown(blockSwapCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.block-swap.cooldown");
    }

    private static void blockSwap(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (!"BLOCK".equals(String.valueOf(plugin.getSettingService().getSetting("game.gametype").getValue()))) {
            player.sendMessage(Component.text("Block swap is only available in BLOCK mode.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot swap blocks!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        blockSwap(player, plugin);
    }

    private static void blockSwap(Player player, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("hider-items.block-swap.range", 50.0);
        Player target = null;
        double closest = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            if (hiderId.equals(player.getUniqueId())) {
                continue;
            }
            Player other = Bukkit.getPlayer(hiderId);
            if (other == null || !other.isOnline()) {
                continue;
            }
            double dist = other.getLocation().distance(player.getLocation());
            if (dist <= closest) {
                closest = dist;
                target = other;
            }
        }

        if (target == null) {
            player.sendMessage(Component.text("No hider nearby to swap with!", NamedTextColor.RED));
            return;
        }

        final Player finalTarget = target;

        Material playerMat = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        Material targetMat = HideAndSeek.getDataController().getChosenBlock(finalTarget.getUniqueId());
        BlockData playerData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        BlockData targetData = HideAndSeek.getDataController().getChosenBlockData(finalTarget.getUniqueId());

        if (playerMat == null || targetMat == null || playerData == null || targetData == null) {
            player.sendMessage(Component.text("Swap failed (missing block data)", NamedTextColor.RED));
            return;
        }

        boolean playerWasHidden = HideAndSeek.getDataController().isHidden(player.getUniqueId());
        boolean targetWasHidden = HideAndSeek.getDataController().isHidden(finalTarget.getUniqueId());

        if (playerWasHidden) {
            plugin.getBlockModeListener().forceUnhide(player);
        }
        if (targetWasHidden) {
            plugin.getBlockModeListener().forceUnhide(finalTarget);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.getInventory().remove(Material.COMPARATOR);
            finalTarget.getInventory().remove(Material.COMPARATOR);

            HideAndSeek.getDataController().setChosenBlock(player.getUniqueId(), targetMat);
            HideAndSeek.getDataController().setChosenBlock(finalTarget.getUniqueId(), playerMat);
            HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), targetData);
            HideAndSeek.getDataController().setChosenBlockData(finalTarget.getUniqueId(), playerData);

            var playerDisplay = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (playerDisplay != null && playerDisplay.isValid()) {
                playerDisplay.setBlock(targetData);
                playerDisplay.setRotation(player.getLocation().getYaw(), 0f);
            }
            var targetDisplay = HideAndSeek.getDataController().getBlockDisplay(finalTarget.getUniqueId());
            if (targetDisplay != null && targetDisplay.isValid()) {
                targetDisplay.setBlock(playerData);
                targetDisplay.setRotation(finalTarget.getLocation().getYaw(), 0f);
            }

            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20, 0.2, 0.5, 0.2, 1.0);
            finalTarget.getWorld().spawnParticle(Particle.PORTAL, finalTarget.getLocation(), 20, 0.2, 0.5, 0.2, 1.0);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
            finalTarget.getWorld().spawnParticle(Particle.DRAGON_BREATH, finalTarget.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);

            player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 1, 0), 15, 0.25, 0.4, 0.25, 0.08);
            finalTarget.getWorld().spawnParticle(Particle.GLOW, finalTarget.getLocation().add(0, 1, 0), 15, 0.25, 0.4, 0.25, 0.08);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            finalTarget.getWorld().playSound(finalTarget.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            HiderItemUtil.updateAppearanceItem(player, plugin);
            HiderItemUtil.updateAppearanceItem(finalTarget, plugin);

            player.sendMessage(Component.text("Swapped blocks with " + finalTarget.getName() + "!", NamedTextColor.GREEN));
            finalTarget.sendMessage(Component.text("Swapped blocks with " + player.getName() + "!", NamedTextColor.GREEN));
        }, 2L);
    }
}
