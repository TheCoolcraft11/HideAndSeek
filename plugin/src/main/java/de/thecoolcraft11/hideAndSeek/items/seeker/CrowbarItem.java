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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class CrowbarItem implements GameItem {
    public static final String ID = "has_seeker_crowbar";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Crowbar", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Left Click to destroy some blocks", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            NamespacedKey key = new NamespacedKey(plugin, "crowbar_mining_penalty");

            AttributeModifier modifier = new AttributeModifier(
                    key,
                    -100.0,
                    AttributeModifier.Operation.ADD_NUMBER,
                    EquipmentSlotGroup.HAND
            );
            meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, modifier);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Breaks Blocks of the map to help you reach small seekers.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int crowBarCooldown = plugin.getSettingRegistry().get("seeker-items.crowbar.cooldown", 30);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withDescription(getDescription(plugin))
                .withAction(ItemActionType.LEFT_CLICK_BLOCK, context -> breakBlock(plugin, context))
                .withAction(ItemActionType.SHIFT_LEFT_CLICK_BLOCK, context -> breakBlock(plugin, context))
                .withDropPrevention(true)
                .withVanillaCooldown(crowBarCooldown * 20)
                .withCustomCooldown(crowBarCooldown * 1000L)
                .cancelDefaultAction(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .build());
    }

    private void breakBlock(HideAndSeek plugin, ItemInteractionContext context) {
        Block block = context.getBlock();
        int maxTicks = plugin.getSettingRegistry().get("seeker-items.crowbar.breaking-ticks", 100);
        boolean enabled = plugin.getSettingRegistry().get("seeker-items.crowbar.enabled", true);
        if (!enabled) return;
        if (block != null && block.getType() != Material.AIR) {
            if (isUnsafeToBreak(block)) {
                context.skipCooldown();
                return;
            }
            new BukkitRunnable() {
                int lastStage = -1;
                int ticks;

                @Override
                public void run() {
                    if (ticks > maxTicks) {
                        playBreakParticles(block);
                        block.getWorld().playSound(
                                block.getLocation(),
                                block.getBlockData().getSoundGroup().getBreakSound(),
                                1.0f,
                                1.0f
                        );
                        block.breakNaturally();
                        cancel();
                        return;
                    }

                    float progress = Math.clamp((float) ticks / maxTicks, 0f, 1f);
                    int stage = (int) (progress * 10);

                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendBlockDamage(block.getLocation(), progress);
                    }
                    if (stage != lastStage) {
                        float pitch = (float) (0.3 + Math.random() * 0.85);
                        block.getWorld().playSound(block.getLocation(),
                                Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR,
                                0.8f,
                                pitch);
                        lastStage = stage;
                    }
                    ticks++;
                }
            }.runTaskTimer(plugin, 0L, 1L);
        }
    }

    private void playBreakParticles(Block block) {
        block.getWorld().spawnParticle(
                Particle.BLOCK,
                block.getLocation().add(0.5, 0.5, 0.5),
                30,
                0.3, 0.3, 0.3,
                block.getBlockData()
        );
    }

    private boolean isUnsafeToBreak(Block block) {
        int maxDrop = 10;

        if (block.getY() <= block.getWorld().getMinHeight()) {
            return true;
        }

        Block current = block.getRelative(BlockFace.DOWN);

        for (int i = 0; i < maxDrop; i++) {
            if (!current.getType().isAir()) {
                return false;
            }
            current = current.getRelative(BlockFace.DOWN);
        }

        return true;
    }

}
