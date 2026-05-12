package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectSkins;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageManager;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkin;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkins;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkinSkins;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkinGUI {
    private static final String ITEMS_TITLE = "gui.skin.titles.selector";
    private static final String VARIANTS_TITLE_PREFIX = "gui.skin.titles.variants";

    private final HideAndSeek plugin;

    public SkinGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    private static String humanize(String id) {
        String shortId = id
                .replace("has_hider_", "")
                .replace("has_seeker_", "")
                .replace('_', ' ');
        String[] parts = shortId.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public void open(Player player) {
        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_selector_" + player.getUniqueId())
                .title(plugin.trText(player, ITEMS_TITLE))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        List<String> logicalItems = getLogicalItems();
        int slot = 0;
        for (String logicalItemId : logicalItems) {
            if (slot >= 45) {
                break;
            }
            InventoryItem skinItem = new InventoryItem(createLogicalItemButton(player, logicalItemId));
            skinItem.setClickHandler((p, item, event, s) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
                openVariants(p, logicalItemId);
                event.setCancelled(true);
            });
            skinItem.setAllowTakeout(false);
            skinItem.setAllowInsert(false);
            skinItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, skinItem);
        }

        InventoryItem backItem = new InventoryItem(createBackHint(player));
        backItem.setClickHandler((p, item, event, s) -> {
            p.closeInventory();
            event.setCancelled(true);
        });
        backItem.setAllowTakeout(false);
        backItem.setAllowInsert(false);
        inventory.setItem(49, backItem);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(50, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openVariants(Player player, String logicalItemId) {
        if (KillEffectSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            openKillEffectVariants(player, logicalItemId);
            return;
        }
        if (WinSkinSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            openWinSkinVariants(player, logicalItemId);
            return;
        }
        if (DeathMessageSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            openDeathMessageVariants(player, logicalItemId);
            return;
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        List<ItemVariant> variants = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_variants_" + player.getUniqueId() + "_" + logicalItemId)
                .title(plugin.trText(player, VARIANTS_TITLE_PREFIX, Map.of("item", humanize(logicalItemId))))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }
        for (ItemVariant variant : variants) {
            if (slot >= 45) {
                break;
            }
            String variantId = variant.getId();
            InventoryItem variantItem = new InventoryItem(createVariantButton(player, logicalItemId, variant, selected));
            variantItem.setClickHandler((p, item, event, s) -> {
                handleVariantClick(p, logicalItemId, variantId, event.getClick());
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_id", variantId);
            variantItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, variantItem);
        }

        InventoryItem backBtn = new InventoryItem(createUtility(Material.ARROW,
                plugin.tr(player, "gui.skin.buttons.back"),
                List.of(plugin.tr(player, "gui.skin.item.return_to_list").decoration(TextDecoration.ITALIC, false))));
        backBtn.setClickHandler((p, item, event, s) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(p);
            event.setCancelled(true);
        });
        backBtn.setAllowTakeout(false);
        backBtn.setAllowInsert(false);
        inventory.setItem(45, backBtn);

        InventoryItem clearBtn = new InventoryItem(createUtility(Material.BARRIER,
                plugin.tr(player, "gui.skin.buttons.clear_selection"),
                List.of(plugin.tr(player, "gui.skin.item.remove_saved_skin").decoration(TextDecoration.ITALIC,
                        false))));
        clearBtn.setClickHandler((p, item, event, s) -> {
            ItemSkinSelectionService.clearSelectedVariant(p.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, p.getUniqueId());
            p.sendMessage(plugin.tr(p, "gui.skin.item.cleared_for_item", Map.of("item", humanize(logicalItemId))));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openVariants(p, logicalItemId);
            event.setCancelled(true);
        });
        clearBtn.setAllowTakeout(false);
        clearBtn.setAllowInsert(false);
        inventory.setItem(53, clearBtn);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(49, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openKillEffectVariants(Player player, String logicalItemId) {
        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_variants_" + player.getUniqueId() + "_" + logicalItemId)
                .title(plugin.trText(player, VARIANTS_TITLE_PREFIX, Map.of("item", humanize(logicalItemId))))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }

        for (KillEffectSkins.Definition definition : KillEffectSkins.getDefinitions()) {
            if (slot >= 45) {
                break;
            }

            String variantId = definition.id();
            InventoryItem variantItem = new InventoryItem(createKillEffectVariantButton(player, logicalItemId, definition, selected));
            variantItem.setClickHandler((p, item, event, s) -> {
                handleVariantClick(p, logicalItemId, variantId, event.getClick());
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_id", variantId);
            variantItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, variantItem);
        }

        InventoryItem backBtn = new InventoryItem(createUtility(Material.ARROW,
                plugin.tr(player, "gui.skin.buttons.back"),
                List.of(plugin.tr(player, "gui.skin.item.return_to_list").decoration(TextDecoration.ITALIC, false))));
        backBtn.setClickHandler((p, item, event, s) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(p);
            event.setCancelled(true);
        });
        backBtn.setAllowTakeout(false);
        backBtn.setAllowInsert(false);
        inventory.setItem(45, backBtn);

        InventoryItem clearBtn = new InventoryItem(createUtility(Material.BARRIER,
                plugin.tr(player, "gui.skin.buttons.clear_selection"),
                List.of(plugin.tr(player, "gui.skin.item.remove_saved_skin").decoration(TextDecoration.ITALIC,
                        false))));
        clearBtn.setClickHandler((p, item, event, s) -> {
            ItemSkinSelectionService.clearSelectedVariant(p.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, p.getUniqueId());
            p.sendMessage(plugin.tr(p, "gui.skin.item.cleared_for_item", Map.of("item", humanize(logicalItemId))));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openKillEffectVariants(p, logicalItemId);
            event.setCancelled(true);
        });
        clearBtn.setAllowTakeout(false);
        clearBtn.setAllowInsert(false);
        inventory.setItem(53, clearBtn);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(49, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openWinSkinVariants(Player player, String logicalItemId) {
        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_variants_" + player.getUniqueId() + "_" + logicalItemId)
                .title(plugin.trText(player, VARIANTS_TITLE_PREFIX, Map.of("item", humanize(logicalItemId))))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }

        for (WinSkinSkins.Definition definition : WinSkinSkins.getDefinitions()) {
            if (slot >= 45) {
                break;
            }

            String variantId = definition.id();
            InventoryItem variantItem = new InventoryItem(createWinSkinVariantButton(player, logicalItemId, definition, selected));
            variantItem.setClickHandler((p, item, event, s) -> {
                handleVariantClick(p, logicalItemId, variantId, event.getClick());
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_id", variantId);
            variantItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, variantItem);
        }

        InventoryItem backBtn = new InventoryItem(createUtility(Material.ARROW,
                plugin.tr(player, "gui.skin.buttons.back"),
                List.of(plugin.tr(player, "gui.skin.item.return_to_list").decoration(TextDecoration.ITALIC, false))));
        backBtn.setClickHandler((p, item, event, s) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(p);
            event.setCancelled(true);
        });
        backBtn.setAllowTakeout(false);
        backBtn.setAllowInsert(false);
        inventory.setItem(45, backBtn);

        InventoryItem clearBtn = new InventoryItem(createUtility(Material.BARRIER,
                plugin.tr(player, "gui.skin.buttons.clear_selection"),
                List.of(plugin.tr(player, "gui.skin.item.remove_saved_skin").decoration(TextDecoration.ITALIC,
                        false))));
        clearBtn.setClickHandler((p, item, event, s) -> {
            ItemSkinSelectionService.clearSelectedVariant(p.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, p.getUniqueId());
            p.sendMessage(plugin.tr(p, "gui.skin.item.cleared_for_item", Map.of("item", humanize(logicalItemId))));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openWinSkinVariants(p, logicalItemId);
            event.setCancelled(true);
        });
        clearBtn.setAllowTakeout(false);
        clearBtn.setAllowInsert(false);
        inventory.setItem(53, clearBtn);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(49, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openDeathMessageVariants(Player player, String logicalItemId) {
        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_variants_" + player.getUniqueId() + "_" + logicalItemId)
                .title(plugin.trText(player, VARIANTS_TITLE_PREFIX, Map.of("item", humanize(logicalItemId))))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }

        for (DeathMessageSkins.Definition definition : DeathMessageSkins.getDefinitions()) {
            if (slot >= 45) {
                break;
            }

            String variantId = definition.id();
            InventoryItem variantItem = new InventoryItem(createDeathMessageVariantButton(player, logicalItemId, definition, selected));
            variantItem.setClickHandler((p, item, event, s) -> {
                handleVariantClick(p, logicalItemId, variantId, event.getClick());
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_id", variantId);
            variantItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, variantItem);
        }

        InventoryItem backBtn = new InventoryItem(createUtility(Material.ARROW,
                plugin.tr(player, "gui.skin.buttons.back"),
                List.of(plugin.tr(player, "gui.skin.item.return_to_list").decoration(TextDecoration.ITALIC, false))));
        backBtn.setClickHandler((p, item, event, s) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(p);
            event.setCancelled(true);
        });
        backBtn.setAllowTakeout(false);
        backBtn.setAllowInsert(false);
        inventory.setItem(45, backBtn);

        InventoryItem clearBtn = new InventoryItem(createUtility(Material.BARRIER,
                plugin.tr(player, "gui.skin.buttons.clear_selection"),
                List.of(plugin.tr(player, "gui.skin.item.remove_saved_death_skin").decoration(TextDecoration.ITALIC,
                        false))));
        clearBtn.setClickHandler((p, item, event, s) -> {
            ItemSkinSelectionService.clearSelectedVariant(p.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, p.getUniqueId());
            p.sendMessage(plugin.tr(p, "gui.skin.item.cleared_for_item", Map.of("item", humanize(logicalItemId))));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openDeathMessageVariants(p, logicalItemId);
            event.setCancelled(true);
        });
        clearBtn.setAllowTakeout(false);
        clearBtn.setAllowInsert(false);
        inventory.setItem(53, clearBtn);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(49, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void handleVariantClick(Player player, String logicalItemId, String variantId, ClickType clickType) {
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variantId);
        if (!unlocked) {
            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variantId);
            if (!ItemSkinSelectionService.unlock(plugin, player.getUniqueId(), logicalItemId, variantId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
                player.sendMessage(
                        plugin.tr(player, "gui.skin.item.not_enough_coins", Map.of("cost", String.valueOf(cost))));
                openVariants(player, logicalItemId);
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
            player.sendMessage(plugin.tr(player, "gui.skin.item.unlocked_skin",
                    Map.of("variant", variantId, "cost", String.valueOf(cost))));
        }

        ItemSkinSelectionService.setSelectedVariant(player.getUniqueId(), logicalItemId, variantId);
        ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        player.sendMessage(plugin.tr(player, "gui.skin.item.selected_skin",
                Map.of("variant", variantId, "item", humanize(logicalItemId))));

        if (clickType == ClickType.RIGHT) {
            player.closeInventory();
            return;
        }

        openVariants(player, logicalItemId);
    }

    private ItemStack createLogicalItemButton(Player player, String logicalItemId) {
        if (KillEffectSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            return createKillEffectCategoryButton(player, logicalItemId);
        }
        if (WinSkinSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            return createWinSkinCategoryButton(player, logicalItemId);
        }
        if (DeathMessageSkins.LOGICAL_ITEM_ID.equals(logicalItemId)) {
            return createDeathMessageCategoryButton(player, logicalItemId);
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        var customItem = plugin.getCustomItemManager().getItem(runtimeItemId);

        ItemStack stack = customItem != null ? customItem.getItemStack().clone() : new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        int unlockedCount = (int) plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).stream()
                .filter(variant -> ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variant.getId()))
                .count();
        int variantCount = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).size();


        meta.displayName(plugin.tr(player, "gui.skin.item.name", Map.of("name", humanize(logicalItemId))).decoration(
                TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.unlocked_count",
                Map.of("unlocked", unlockedCount, "total", variantCount)).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.selected_variant", Map.of("selected",
                (selected == null || selected.isBlank()) ? plugin.trText(player,
                        "common.state.default") : selected)).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.left_click_open").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skin_item_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                logicalItemId
        );
        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, runtimeItemId, selected);
        return stack;
    }

    private ItemStack createKillEffectCategoryButton(Player player, String logicalItemId) {
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        KillEffectSkins.Definition selectedDef = selected == null ? null : KillEffectSkins.getDefinition(selected);
        ItemStack stack = new ItemStack(selectedDef == null ? Material.FIREWORK_STAR : selectedDef.icon());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        int variantCount = KillEffectSkins.getDefinitions().size();
        int unlockedCount = (int) KillEffectSkins.getDefinitions().stream()
                .filter(def -> ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, def.id()))
                .count();

        meta.displayName(
                plugin.tr(player, "gui.skin.category.kill_effects").decoration(TextDecoration.BOLD, true).decoration(
                        TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.unlocked_count",
                Map.of("unlocked", unlockedCount, "total", variantCount)).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.selected_variant", Map.of("selected",
                (selectedDef == null ? plugin.trText(player,
                        "common.state.default") : selectedDef.displayName()))).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.left_click_open").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, KillEffectSkins.LOGICAL_ITEM_ID, selected);
        return stack;
    }

    private ItemStack createWinSkinCategoryButton(Player player, String logicalItemId) {
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        WinSkinSkins.Definition selectedDef = selected == null ? null : WinSkinSkins.getDefinition(selected);
        ItemStack stack = new ItemStack(selectedDef == null ? Material.NETHER_STAR : selectedDef.icon());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        int variantCount = WinSkinSkins.getDefinitions().size();
        int unlockedCount = (int) WinSkinSkins.getDefinitions().stream()
                .filter(def -> ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, def.id()))
                .count();

        meta.displayName(
                plugin.tr(player, "gui.skin.category.win_skins").decoration(TextDecoration.BOLD, true).decoration(
                        TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.unlocked_count",
                Map.of("unlocked", unlockedCount, "total", variantCount)).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.selected_variant", Map.of("selected",
                (selectedDef == null ? plugin.trText(player,
                        "common.state.default") : selectedDef.displayName()))).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.left_click_open").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, WinSkinSkins.LOGICAL_ITEM_ID, selected);
        return stack;
    }

    private ItemStack createDeathMessageCategoryButton(Player player, String logicalItemId) {
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        DeathMessageSkins.Definition selectedDef = selected == null ? null : DeathMessageSkins.getDefinition(selected);
        ItemStack stack = new ItemStack(selectedDef == null ? Material.WRITABLE_BOOK : selectedDef.icon());

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        int variantCount = DeathMessageSkins.getDefinitions().size();
        int unlockedCount = (int) DeathMessageSkins.getDefinitions().stream()
                .filter(def -> ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, def.id()))
                .count();

        meta.displayName(
                plugin.tr(player, "gui.skin.category.death_messages").decoration(TextDecoration.BOLD, true).decoration(
                        TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.unlocked_count",
                Map.of("unlocked", unlockedCount, "total", variantCount)).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.selected_variant", Map.of("selected",
                (selectedDef == null ? plugin.trText(player,
                        "common.state.default") : selectedDef.displayName()))).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.left_click_open").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, DeathMessageSkins.LOGICAL_ITEM_ID, selected);
        return stack;
    }

    private ItemStack createVariantButton(Player player, String logicalItemId, ItemVariant variant, String selectedVariant) {
        ItemStack stack = variant.getItemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String label = variant.getDisplayName() == null || variant.getDisplayName().isBlank()
                ? variant.getId()
                : variant.getDisplayName();
        boolean selected = variant.getId().equals(selectedVariant);
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variant.getId());
        int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variant.getId());
        ItemRarity rarity = ItemSkinSelectionService.getRarity(logicalItemId, variant.getId());

        String stateColor = selected ? "green" : (unlocked ? "aqua" : "red");
        Component variantTitle = plugin.tr(player, "gui.skin.item.variant_title",
                Map.of("label", label, "color", stateColor));
        meta.displayName(variantTitle.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.id", Map.of("id", variant.getId())).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.rarity", Map.of("rarity", rarity.name())).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player,
                selected ? "gui.skin.item.currently_selected" : (unlocked ? "gui.skin.item.click_to_select" : "gui.skin.item.click_to_unlock_select")).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.right_click_close").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skin_variant_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                variant.getId()
        );
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createKillEffectVariantButton(Player player, String logicalItemId, KillEffectSkins.Definition definition, String selectedVariant) {
        ItemStack stack = new ItemStack(definition.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        boolean selected = definition.id().equals(selectedVariant);
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, definition.id());
        int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, definition.id());

        String stateColor = selected ? "green" : (unlocked ? "aqua" : "red");
        Component title = plugin.tr(player, "gui.skin.item.variant_title",
                Map.of("label", definition.displayName(), "color", stateColor));
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.id", Map.of("id", definition.id())).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.rarity",
                Map.of("rarity", definition.rarity().name(), "color", getRarityTag(definition.rarity()))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player,
                selected ? "gui.skin.item.currently_selected" : (unlocked ? "gui.skin.item.click_to_select" : "gui.skin.item.click_to_unlock_select")).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.right_click_close").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, KillEffectSkins.LOGICAL_ITEM_ID, definition.id());
        return stack;
    }

    private ItemStack createWinSkinVariantButton(Player player, String logicalItemId, WinSkinSkins.Definition definition, String selectedVariant) {
        ItemStack stack = new ItemStack(definition.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        boolean selected = definition.id().equals(selectedVariant);
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, definition.id());
        int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, definition.id());

        String stateColor = selected ? "green" : (unlocked ? "aqua" : "red");
        Component title = plugin.tr(player, "gui.skin.item.variant_title",
                Map.of("label", definition.displayName(), "color", stateColor));
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.id", Map.of("id", definition.id())).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.rarity",
                Map.of("rarity", definition.rarity().name(), "color", getRarityTag(definition.rarity()))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player,
                selected ? "gui.skin.item.currently_selected" : (unlocked ? "gui.skin.item.click_to_select" : "gui.skin.item.click_to_unlock_select")).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.right_click_close").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, WinSkinSkins.LOGICAL_ITEM_ID, definition.id());
        return stack;
    }

    private ItemStack createDeathMessageVariantButton(Player player, String logicalItemId, DeathMessageSkins.Definition definition, String selectedVariant) {
        ItemStack stack = new ItemStack(definition.icon());
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        boolean selected = definition.id().equals(selectedVariant);
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, definition.id());
        int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, definition.id());

        String stateColor = selected ? "green" : (unlocked ? "aqua" : "red");
        Component title = plugin.tr(player, "gui.skin.item.variant_title",
                Map.of("label", definition.displayName(), "color", stateColor));
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.skin.item.id", Map.of("id", definition.id())).decoration(TextDecoration.ITALIC,
                false));
        lore.add(plugin.tr(player, "gui.skin.item.rarity",
                Map.of("rarity", definition.rarity().name(), "color", getRarityTag(definition.rarity()))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));

        DeathMessageSkin skin = DeathMessageManager.getDeathMessageSkin(definition.id());
        if (skin != null) {
            Component envPreview = skin.getEnvironmentalDeathMessage(player.getName(), "CAMPING");
            Component killPreview = skin.getKillMessage(player.getName(), "Victim");
            lore.add(plugin.tr(player, "gui.skin.item.preview_env").decoration(TextDecoration.ITALIC, false));
            lore.add((envPreview == null ? plugin.tr(player, "gui.skin.item.preview_env_default",
                    Map.of("player", player.getName())) : envPreview)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(plugin.tr(player, "gui.skin.item.preview_kill").decoration(TextDecoration.ITALIC, false));
            lore.add((killPreview == null ? plugin.tr(player, "gui.skin.item.preview_kill_default",
                    Map.of("player", player.getName(), "victim",
                            plugin.trText(player, "gui.skin.preview.sample.victim"))) : killPreview)
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(plugin.tr(player,
                selected ? "gui.skin.item.currently_selected" : (unlocked ? "gui.skin.item.click_to_select" : "gui.skin.item.click_to_unlock_select")).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.skin.item.right_click_close").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        stack.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(stack, DeathMessageSkins.LOGICAL_ITEM_ID, definition.id());
        return stack;
    }

    private ItemStack createBackHint(Player player) {
        return createUtility(Material.BOOK, plugin.tr(player, "gui.skin.buttons.skin_selection"),
                List.of(
                        plugin.tr(player, "gui.skin.item.return_to_list").decoration(TextDecoration.ITALIC, false),
                        plugin.tr(player, "gui.skin.item.remove_saved_skin").decoration(TextDecoration.ITALIC, false)
                ));
    }

    private ItemStack createCoinsHint(Player player) {
        int coins = ItemSkinSelectionService.getCoins(player.getUniqueId());
        return createUtility(Material.GOLD_NUGGET, plugin.tr(player, "gui.skin.buttons.coins", Map.of("coins", coins)),
                List.of(plugin.tr(player, "gui.skin.item.earned_coins_hint").decoration(TextDecoration.ITALIC, false)));
    }


    private String getRarityTag(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> "white";
            case UNCOMMON -> "green";
            case RARE -> "blue";
            case EPIC -> "dark_purple";
            case LEGENDARY -> "gold";
        };
    }

    private ItemStack createUtility(Material material, Component title, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(title.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getLogicalItems() {
        Set<String> ids = new TreeSet<>();
        for (String itemId : plugin.getCustomItemManager().getVariantManager().getAllVariants().keySet()) {
            ids.add(ItemSkinSelectionService.normalizeLogicalItemId(itemId));
        }
        ids.add(KillEffectSkins.LOGICAL_ITEM_ID);
        ids.add(WinSkinSkins.LOGICAL_ITEM_ID);
        ids.add(DeathMessageSkins.LOGICAL_ITEM_ID);
        return new ArrayList<>(ids);
    }
}
