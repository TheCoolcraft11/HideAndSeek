package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.hider.RandomBlockItem.randomizeBlockFor;

public class BlockRandomizerItem implements GameItem {
    public static final String ID = "has_seeker_block_randomizer";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Block Randomizer", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to randomize all blocks", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Force all hiders to reroll their disguise blocks.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int randCooldown = plugin.getSettingRegistry().get("seeker-items.block-randomizer.cooldown", 45);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> randomizeAllBlocks(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> randomizeAllBlocks(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(randCooldown * 20)
                .withCustomCooldown(randCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void randomizeAllBlocks(Player seeker, HideAndSeek plugin) {
        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            seeker.sendMessage(Component.text("Block Randomizer is only available in BLOCK mode.", NamedTextColor.RED));
            return;
        }

        boolean glitchCore = ItemSkinSelectionService.isSelected(seeker, ID, "skin_glitch_core");
        boolean chaosMagic = ItemSkinSelectionService.isSelected(seeker, ID, "skin_chaos_magic");

        if (chaosMagic) {
            seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1, 0), 10, 0.25, 0.3, 0.25, 0.02);
            seeker.playSound(seeker.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.55f);
        }

        int count = 0;
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;
            randomizeBlockFor(hider, plugin, true);

            if (glitchCore) {
                hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1, 0), 18, 0.4, 0.4, 0.4, 0.08);
                hider.getWorld().spawnParticle(Particle.PORTAL, hider.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.1);
                hider.playSound(hider.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.9f, 1.4f);
            } else if (chaosMagic) {
                hider.getWorld().spawnParticle(Particle.WITCH, hider.getLocation().add(0, 1, 0), 16, 0.4, 0.4, 0.4, 0.08);
                hider.getWorld().spawnParticle(Particle.ENCHANT, hider.getLocation().add(0, 1, 0), 10, 0.4, 0.4, 0.4, 0.1);
                hider.playSound(hider.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.3f);
            } else {
                hider.getWorld().spawnParticle(Particle.ENCHANT, hider.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.1);
                hider.playSound(hider.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.5f);
            }
            count++;
        }
        seeker.sendMessage(Component.text("Blocks randomized! (" + count + " hiders)", NamedTextColor.GREEN));
    }
}
