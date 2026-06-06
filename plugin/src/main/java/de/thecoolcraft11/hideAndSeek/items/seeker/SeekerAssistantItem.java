package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeekerAssistantItem implements GameItem {

    public static final String ID = "has_seeker_assistant";
    public static final String SKIN_STEEL_GOLEM = "skin_steel_golem";
    public static final String SKIN_GHOST_DRONE = "skin_ghost_drone";
    public static final String SKIN_BATTLE_MECH = "skin_battle_mech";
    public static final String PDC_SKIN_KEY = "assistant_skin_variant";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Seeker's Assistant")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>(List.of(
                Component.text("Summon a hunting assistant to track down hiders.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Max 2 active at the same time.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("LEGENDARY", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            lore.add(Component.text("Not available on this server version", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin, @Nullable Player player) {
        return plugin.trText(player, "item.assistant.description");
    }

    @Override
    public void register(HideAndSeek plugin) {
        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            return;
        }

        int cooldownSeconds = plugin.getSettingRegistry().get("seeker-items.assistant.cooldown", 120);

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> handleUse(context.getPlayer(), plugin))
                        .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> handleUse(context.getPlayer(), plugin))
                        .withDescription(getDescription(plugin, null))
                        .withNameKey("item.assistant.name")
                        .withLoreKey("item.assistant.lore")
                        .withNameKey("item.assistant.name")
                        .withLoreKey("item.assistant.lore")
                        .withDropPrevention(true)
                        .withCraftPrevention(true)
                        .withVanillaCooldown(cooldownSeconds * 20)
                        .withCustomCooldown((long) cooldownSeconds * 1000L)
                        .withVanillaCooldownDisplay(true)
                        .allowOffHand(false)
                        .allowArmor(false)
                        .cancelDefaultAction(true)
                        .build()
        );
    }

    private void handleUse(Player player, HideAndSeek plugin) {
        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            player.sendMessage(plugin.trText(player, "item.assistant.messages.nms_unavailable"));
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(plugin.trText(player, "item.assistant.messages.seeking_only"));
            return;
        }

        UUID seekerId = player.getUniqueId();
        ItemStateManager.pruneInvalidAssistants(seekerId);
        List<UUID> active = ItemStateManager.activeAssistants.computeIfAbsent(seekerId,
                ignored -> new CopyOnWriteArrayList<>());
        int max = plugin.getSettingRegistry().get("seeker-items.assistant.max-per-seeker", 2);
        if (active.size() >= max) {
            player.sendMessage(plugin.trText(player, "item.assistant.messages.max_active",
                    java.util.Map.of("max", String.valueOf(max))));
            return;
        }

        Location spawn = player.getLocation();
        String selectedSkin = resolveSelectedSkin(player);
        Entity assistant = plugin.getNmsAdapter().spawnSeekerAssistant(plugin, player, spawn, selectedSkin);
        if (assistant == null) {
            player.sendMessage(plugin.trText(player, "item.assistant.messages.summon_failed"));
            return;
        }
        if (selectedSkin != null) {
            assistant.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, PDC_SKIN_KEY),
                    org.bukkit.persistence.PersistentDataType.STRING,
                    selectedSkin
            );
        }

        UUID assistantId = assistant.getUniqueId();
        active.add(assistantId);
        ItemStateManager.assistantOrigins.put(assistantId, spawn.clone());
        ItemStateManager.assistantSpawnTimes.put(assistantId, System.currentTimeMillis());
        ItemStateManager.assistantHitCounts.put(assistantId, 0);

        int lifetimeSeconds = plugin.getSettingRegistry().get("seeker-items.assistant.lifetime", 90);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Entity e = plugin.getServer().getEntity(assistantId);
            if (e != null && e.isValid()) {
                e.remove();
            }
            ItemStateManager.removeAssistant(assistantId);
        }, lifetimeSeconds * 20L);

        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.5f);

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 20, 0.4, 0.6, 0.4, 0.03);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 16, 0.3, 0.4, 0.3, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 10, 0.3, 0.5, 0.3, 0.01);

        if (SKIN_STEEL_GOLEM.equals(selectedSkin)) {
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation(), 14, 0.35, 0.45, 0.35,
                    Material.IRON_BLOCK.createBlockData());
        } else if (SKIN_GHOST_DRONE.equals(selectedSkin)) {
            player.getWorld().spawnParticle(Particle.SOUL, player.getLocation(), 18, 0.35, 0.55, 0.35, 0.03);
        } else if (SKIN_BATTLE_MECH.equals(selectedSkin)) {
            player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 24, 0.4, 0.6, 0.4, 0.04);
            player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.55f, 1.4f);
        }

        player.sendActionBar(Component.text()
                .append(Component.text("Assistant summoned! ", NamedTextColor.RED))
                .append(Component.text("(" + lifetimeSeconds + "s lifetime)", NamedTextColor.GRAY))
                .build());
    }

    private String resolveSelectedSkin(Player player) {
        String variant = ItemSkinSelectionService.getSelectedVariant(player, ID);
        if (variant == null || variant.isBlank()) {
            return null;
        }
        return ItemSkinSelectionService.isUnlocked(player.getUniqueId(), ID, variant) ? variant : null;
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "seeker-items.assistant.cooldown",
                "seeker-items.assistant.lifetime",
                "seeker-items.assistant.health",
                "seeker-items.assistant.speed",
                "seeker-items.assistant.max-per-seeker"
        );
    }
}



