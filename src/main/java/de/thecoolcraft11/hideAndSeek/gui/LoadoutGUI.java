package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class LoadoutGUI implements Listener {
    private final LoadoutManager loadoutManager;
    private final Map<UUID, Boolean> viewMode = new HashMap<>();

    public LoadoutGUI(LoadoutManager loadoutManager) {
        this.loadoutManager = loadoutManager;
    }

    public void open(Player player) {
        if (!loadoutManager.canModifyLoadout()) {
            player.sendMessage(Component.text("Loadouts can only be modified in the lobby!", NamedTextColor.RED));
            return;
        }

        viewMode.putIfAbsent(player.getUniqueId(), true);
        openView(player, viewMode.get(player.getUniqueId()));
    }

    private void openView(Player player, boolean hiderView) {
        Inventory inv = Bukkit.createInventory(null, 54,
                Component.text(hiderView ? "Hider Loadout" : "Seeker Loadout", NamedTextColor.GOLD));

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        int maxItems = hiderView ? loadoutManager.getMaxHiderItems() : loadoutManager.getMaxSeekerItems();
        int maxTokens = hiderView ? loadoutManager.getMaxHiderTokens() : loadoutManager.getMaxSeekerTokens();
        int usedTokens = hiderView ? loadout.getHiderTokensUsed() : loadout.getSeekerTokensUsed();
        Set<LoadoutItemType> selected = hiderView ? loadout.getHiderItems() : loadout.getSeekerItems();


        inv.setItem(4, createInfoItem(selected.size(), maxItems, usedTokens, maxTokens));


        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (hiderView && !item.isForHiders()) continue;
            if (!hiderView && !item.isForSeekers()) continue;

            int cost = loadoutManager.getItemCost(item);
            boolean isSelected = selected.contains(item);
            inv.setItem(slot++, createItemStack(item, cost, isSelected, usedTokens, maxTokens, selected.size(), maxItems));
        }


        int selectedSlot = 45;
        int displayedCount = 0;
        for (LoadoutItemType item : selected) {
            if (displayedCount >= 7) break;
            int cost = loadoutManager.getItemCost(item);
            inv.setItem(selectedSlot++, createSelectedItemDisplay(item, cost));
            displayedCount++;
        }


        inv.setItem(52, createToggleButton(hiderView));

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);

        if (!titleStr.equals("Hider Loadout") && !titleStr.equals("Seeker Loadout")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getRawSlot();
        boolean isHiderView = titleStr.equals("Hider Loadout");


        if (slot == 52) {
            viewMode.put(player.getUniqueId(), !isHiderView);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            openView(player, !isHiderView);
            return;
        }


        if (slot >= 45 && slot <= 51) {
            PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
            Set<LoadoutItemType> selected = isHiderView ? loadout.getHiderItems() : loadout.getSeekerItems();


            List<LoadoutItemType> selectedList = new ArrayList<>(selected);
            int itemIndex = slot - 45;

            if (itemIndex < selectedList.size()) {
                LoadoutItemType itemToRemove = selectedList.get(itemIndex);

                if (isHiderView) {
                    loadout.removeHiderItem(itemToRemove);
                } else {
                    loadout.removeSeekerItem(itemToRemove);
                }

                int cost = loadoutManager.getItemCost(itemToRemove);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(formatName(itemToRemove.name()), getRarityColor(itemToRemove.getRarity())))
                        .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));

                openView(player, isHiderView);
            }
            return;
        }


        if (slot < 9 || slot >= 45) return;

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        LoadoutItemType clickedItem = getItemTypeFromSlot(slot - 9, isHiderView);

        if (clickedItem == null) return;

        int cost = loadoutManager.getItemCost(clickedItem);

        if (isHiderView) {
            if (loadout.getHiderItems().contains(clickedItem)) {
                loadout.removeHiderItem(clickedItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                        .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));
            } else {
                if (loadout.addHiderItem(clickedItem, loadoutManager.getMaxHiderItems(),
                        loadoutManager.getMaxHiderTokens(), cost)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                            .append(Component.text(" (-" + cost + " tokens)", NamedTextColor.GOLD)));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getHiderItems().size() >= loadoutManager.getMaxHiderItems()) {
                        player.sendMessage(Component.text("Maximum items reached!", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Not enough tokens!", NamedTextColor.RED));
                    }
                }
            }
        } else {
            if (loadout.getSeekerItems().contains(clickedItem)) {
                loadout.removeSeekerItem(clickedItem);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                        .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));
            } else {
                if (loadout.addSeekerItem(clickedItem, loadoutManager.getMaxSeekerItems(),
                        loadoutManager.getMaxSeekerTokens(), cost)) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                            .append(Component.text(" (-" + cost + " tokens)", NamedTextColor.GOLD)));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getSeekerItems().size() >= loadoutManager.getMaxSeekerItems()) {
                        player.sendMessage(Component.text("Maximum items reached!", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Not enough tokens!", NamedTextColor.RED));
                    }
                }
            }
        }

        openView(player, isHiderView);
    }

    private LoadoutItemType getItemTypeFromSlot(int index, boolean isHider) {
        int currentIndex = 0;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (isHider && !item.isForHiders()) continue;
            if (!isHider && !item.isForSeekers()) continue;

            if (currentIndex == index) return item;
            currentIndex++;
        }
        return null;
    }

    private ItemStack createInfoItem(int usedSlots, int maxSlots, int usedTokens, int maxTokens) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Loadout Info", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("Items: " + usedSlots + "/" + maxSlots, usedSlots >= maxSlots ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Tokens: " + usedTokens + "/" + maxTokens, usedTokens >= maxTokens ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createToggleButton(boolean isHiderView) {
        ItemStack item = new ItemStack(isHiderView ? Material.BLUE_CONCRETE : Material.RED_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Switch to " + (isHiderView ? "Seeker" : "Hider") + " Loadout",
                NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemStack(LoadoutItemType type, int cost, boolean selected, int usedTokens, int maxTokens, int usedSlots, int maxSlots) {
        Material material = getMaterialForItem(type);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor rarityColor = getRarityColor(type.getRarity());
        meta.displayName(Component.text(formatName(type.name()), rarityColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();


        String description = getItemDescription(type);
        if (!description.isEmpty()) {
            lore.add(Component.text(description, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Cost: " + cost + " tokens", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rarity: " + type.getRarity().name(), rarityColor)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (selected) {
            lore.add(Component.text("Selected", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to remove", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            boolean canAfford = usedTokens + cost <= maxTokens;
            boolean hasSlot = usedSlots < maxSlots;

            if (canAfford && hasSlot) {
                lore.add(Component.text("Click to select", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else if (!canAfford) {
                lore.add(Component.text("Not enough tokens", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("No slots available", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String getItemDescription(LoadoutItemType type) {
        return switch (type) {
            case FIRECRACKER -> "Creates a loud sound to scare seekers";
            case CAT_SOUND -> "Plays a cat sound for misdirection";
            case RANDOM_BLOCK -> "Randomize your block appearance";
            case SPEED_BOOST -> "Gain temporary speed boost";
            case TRACKER_CROSSBOW -> "Track seekers with arrows";
            case BLOCK_SWAP -> "Swap blocks with other hiders";
            case BIG_FIRECRACKER -> "Large explosion for distraction";
            case FIREWORK_ROCKET -> "Launch fireworks as escape";
            case KNOCKBACK_STICK -> "Knockback tool for combat";
            case MEDKIT -> "Heal yourself in battle";
            case TOTEM_OF_UNDYING -> "Revive from death once";
            case INVISIBILITY_CLOAK -> "Become invisible temporarily";
            case SLOWNESS_BALL -> "Slow down pursuing seekers";
            case SMOKE_BOMB -> "Create a smoke cloud for cover";
            case GRAPPLING_HOOK -> "Launch towards your target";
            case INK_SPLASH -> "Blind nearby hiders";
            case LIGHTNING_FREEZE -> "Freeze all hiders briefly";
            case GLOWING_COMPASS -> "Make nearest hider glow";
            case CURSE_SPELL -> "Curse hiders when hitting them";
            case BLOCK_RANDOMIZER -> "Force all hiders to change blocks";
            case CHAIN_PULL -> "Pull hiders towards you";
            case PROXIMITY_SENSOR -> "Place sensor to detect hiders";
            case CAGE_TRAP -> "Trap hiders in an invisible cage";
        };
    }

    private ItemStack createSelectedItemDisplay(LoadoutItemType type, int cost) {
        Material material = getMaterialForItem(type);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor rarityColor = getRarityColor(type.getRarity());
        meta.displayName(Component.text(formatName(type.name()), rarityColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Cost: " + cost + " tokens", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rarity: " + type.getRarity().name(), rarityColor)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to remove", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Material getMaterialForItem(LoadoutItemType type) {
        return switch (type) {
            case FIRECRACKER -> Material.RED_CANDLE;
            case CAT_SOUND -> Material.CAT_SPAWN_EGG;
            case RANDOM_BLOCK -> Material.BLAZE_POWDER;
            case SPEED_BOOST -> Material.WOODEN_HOE;
            case TRACKER_CROSSBOW -> Material.CROSSBOW;
            case BLOCK_SWAP -> Material.ENDER_PEARL;
            case BIG_FIRECRACKER -> Material.TNT;
            case FIREWORK_ROCKET -> Material.FIREWORK_ROCKET;
            case KNOCKBACK_STICK -> Material.STICK;
            case MEDKIT -> Material.GOLDEN_APPLE;
            case TOTEM_OF_UNDYING -> Material.TOTEM_OF_UNDYING;
            case INVISIBILITY_CLOAK -> Material.PHANTOM_MEMBRANE;
            case SLOWNESS_BALL -> Material.SNOWBALL;
            case SMOKE_BOMB -> Material.GRAY_DYE;
            case GRAPPLING_HOOK -> Material.FISHING_ROD;
            case INK_SPLASH -> Material.INK_SAC;
            case LIGHTNING_FREEZE -> Material.LIGHTNING_ROD;
            case GLOWING_COMPASS -> Material.COMPASS;
            case CURSE_SPELL -> Material.ENCHANTED_BOOK;
            case BLOCK_RANDOMIZER -> Material.BLAZE_POWDER;
            case CHAIN_PULL -> Material.LEAD;
            case PROXIMITY_SENSOR -> Material.REDSTONE_TORCH;
            case CAGE_TRAP -> Material.IRON_BARS;
        };
    }

    private NamedTextColor getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> NamedTextColor.WHITE;
            case UNCOMMON -> NamedTextColor.GREEN;
            case RARE -> NamedTextColor.BLUE;
            case EPIC -> NamedTextColor.LIGHT_PURPLE;
            case LEGENDARY -> NamedTextColor.GOLD;
        };
    }

    private String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}



