package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.loadout.*;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryClickHandler;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class AdminLoadoutManagementGUI {

    private static final String ADMIN_PERMISSION = "admin.loadout";

    private final HideAndSeek plugin;
    private final LoadoutManager loadoutManager;
    private final Map<UUID, Tab> tabByAdmin = new HashMap<>();
    private final Map<UUID, UUID> selectedTargetByAdmin = new HashMap<>();
    private final Map<UUID, LoadoutRole> presetRoleByAdmin = new HashMap<>();

    public AdminLoadoutManagementGUI(HideAndSeek plugin) {
        this.plugin = plugin;
        this.loadoutManager = plugin.getLoadoutManager();
    }

    public void open(Player admin) {
        if (!admin.hasPermission(ADMIN_PERMISSION)) {
            admin.sendMessage(plugin.tr(admin, "gui.admin.permission_denied"));
            return;
        }
        Tab tab = tabByAdmin.getOrDefault(admin.getUniqueId(), Tab.HIDER);
        openTab(admin, tab);
    }

    private void openTab(Player admin, Tab tab) {
        tabByAdmin.put(admin.getUniqueId(), tab);

        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_loadout_" + admin.getUniqueId() + "_" + tab.name().toLowerCase())
                .title(plugin.trText(admin, "gui.admin.title"))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        switch (tab) {
            case HIDER -> fillRoleTab(admin, inv, LoadoutRole.HIDER);
            case SEEKER -> fillRoleTab(admin, inv, LoadoutRole.SEEKER);
            case PERKS -> fillPerkTab(admin, inv);
            case PLAYERS -> fillPlayersTab(admin, inv);
            case PRESETS -> fillPresetTab(admin, inv);
        }

        setTabButtons(admin, inv, tab);
        plugin.getInventoryFramework().openInventory(admin, inv);
    }

    private void fillRoleTab(Player admin, FrameworkInventory inv, LoadoutRole role) {
        LoadoutFilterMode mode = loadoutManager.getFilterMode(role);
        Set<LoadoutItemType> filterItems = loadoutManager.getFilterItems(role);

        inv.setItem(0,
                clickable(createInfoItem(admin, role, mode, filterItems.size()), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1, clickable(
                createUtility(GUIItems.ADMIN_MODE_TOGGLE,
                        plugin.tr(admin, "gui.admin.loadout.mode", Map.of("mode", mode.name())),
                        List.of(plugin.tr(admin, "gui.admin.loadout.mode.hint"))), (p, i, e, s) -> {
            LoadoutFilterMode next = mode == LoadoutFilterMode.BLACKLIST ? LoadoutFilterMode.WHITELIST : LoadoutFilterMode.BLACKLIST;
            loadoutManager.setFilterMode(role, next);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.admin.loadout.filter.mode.changed",
                            Map.of("role", role.name().toLowerCase(), "mode", next.name(), "count",
                                    String.valueOf(affected))));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        inv.setItem(2,
                clickable(createUtility(GUIItems.ADMIN_CLEAR_ENTRIES,
                        plugin.tr(admin, "gui.admin.loadout.clear_entries.title"),
                        List.of(plugin.tr(admin, "gui.admin.loadout.clear_entries.hint"))), (p, i, e, s) -> {
            loadoutManager.clearRoleFilter(role);
            int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(plugin.tr(p, "gui.admin.loadout.clear_entries.done",
                            Map.of("role", role.name().toLowerCase(), "count", String.valueOf(affected))));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        inv.setItem(3, clickable(createUtility(GUIItems.ADMIN_RESET_ALL,
                plugin.tr(admin, "gui.admin.loadout.reset_all.title", Map.of("role", role.name())),
                List.of(plugin.tr(admin, "gui.admin.loadout.reset_all.hint"))), (p, i, e, s) -> {
            int changed = loadoutManager.resetAllLoadouts(role);
            loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(plugin.tr(p, "gui.admin.loadout.reset_all.done",
                    Map.of("role", role.name().toLowerCase(), "count", String.valueOf(changed))));
            openTab(p, tabByRole(role));
            e.setCancelled(true);
        }));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (role == LoadoutRole.HIDER && !item.isForHiders()) {
                continue;
            }
            if (role == LoadoutRole.SEEKER && !item.isForSeekers()) {
                continue;
            }
            if (slot >= 45) {
                break;
            }

            boolean inFilter = filterItems.contains(item);
            boolean allowed = loadoutManager.isItemAvailableForRole(role, item);
            ItemStack stack = createLoadoutPolicyItem(admin, item, role, inFilter,
                    List.of(
                            plugin.tr(admin, "gui.admin.loadout.item.filter_entry",
                                    Map.of("value", inFilter ? "YES" : "NO")),
                            plugin.tr(admin, "gui.admin.loadout.item.mode", Map.of("mode", mode.name())),
                            plugin.tr(admin, "gui.admin.loadout.item.status",
                                    Map.of("status", allowed ? "ALLOWED" : "BLOCKED", "color",
                                            allowed ? "green" : "red")),
                            plugin.tr(admin, "gui.admin.loadout.item.click_toggle")
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleRoleFilterItem(role, item);
                int affected = loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(plugin.tr(p, "gui.admin.loadout.item.updated",
                        Map.of("item", humanize(item.name()), "count", String.valueOf(affected))));
                openTab(p, tabByRole(role));
                e.setCancelled(true);
            }));
        }
    }

    private void fillPerkTab(Player admin, FrameworkInventory inv) {
        inv.setItem(0, clickable(createUtility(GUIItems.ADMIN_PERKS_TITLE, plugin.tr(admin, "gui.admin.perks.title"),
                List.of(plugin.tr(admin, "gui.admin.perks.hint"))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1,
                clickable(createUtility(GUIItems.ADMIN_PERKS_REFRESH, plugin.tr(admin, "gui.admin.perks.refresh.title"),
                List.of(plugin.tr(admin, "gui.admin.perks.refresh.hint"))), (p, i, e, s) -> {
            loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(plugin.tr(p, "gui.admin.perks.refresh.done"));
            openTab(p, Tab.PERKS);
            e.setCancelled(true);
        }));

        int slot = 9;
        for (PerkDefinition perk : plugin.getPerkRegistry().getAllPerks()) {
            if (perk.getTarget() != PerkTarget.HIDER && perk.getTarget() != PerkTarget.SEEKER) {
                continue;
            }
            if (slot >= 45) {
                break;
            }

            LoadoutRole role = perk.getTarget() == PerkTarget.HIDER ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
            boolean disabled = loadoutManager.getDisabledPerks(role).contains(perk.getId());
            ItemStack stack = createPerkPolicyItem(admin, perk, role, disabled,
                    List.of(
                            plugin.tr(admin, "gui.admin.perks.role", Map.of("role", role.name())),
                            plugin.tr(admin, "gui.admin.perks.status",
                                    Map.of("state", disabled ? "DISABLED" : "ENABLED")),
                            perk.getDescription(),
                            plugin.tr(admin, "gui.admin.perks.click_toggle")
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleDisabledPerk(role, perk.getId());
                loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(plugin.tr(p, "gui.admin.perks.updated",
                        Map.of("perk", perk.getId(), "role", role.name().toLowerCase())));
                openTab(p, Tab.PERKS);
                e.setCancelled(true);
            }));
        }
    }

    private void fillPlayersTab(Player admin, FrameworkInventory inv) {
        UUID selectedTargetId = selectedTargetByAdmin.get(admin.getUniqueId());
        Player selectedTarget = selectedTargetId == null ? null : Bukkit.getPlayer(selectedTargetId);

        inv.setItem(0,
                clickable(createUtility(GUIItems.ADMIN_PLAYERS_TITLE, plugin.tr(admin, "gui.admin.players.title"),
                List.of(plugin.tr(admin, "gui.admin.players.left_click"),
                        plugin.tr(admin, "gui.admin.players.right_click"))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(36, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.toggle_hider"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_TOGGLE_HIDER, LoadoutRole.HIDER), (p, i, e, s) -> {
            if (selectedTarget == null) {
                e.setCancelled(true);
                return;
            }
            boolean locked = !loadoutManager.isRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.HIDER);
            loadoutManager.setRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.HIDER, locked);
                    p.sendMessage(plugin.tr(p, "gui.admin.players.lock_changed",
                            Map.of("player", selectedTarget.getName(), "role", "hider", "locked",
                                    String.valueOf(locked))));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(37, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.toggle_seeker"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_TOGGLE_SEEKER, LoadoutRole.SEEKER), (p, i, e, s) -> {
            if (selectedTarget == null) {
                e.setCancelled(true);
                return;
            }
            boolean locked = !loadoutManager.isRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.SEEKER);
            loadoutManager.setRoleLocked(selectedTarget.getUniqueId(), LoadoutRole.SEEKER, locked);
                    p.sendMessage(plugin.tr(p, "gui.admin.players.lock_changed",
                            Map.of("player", selectedTarget.getName(), "role", "seeker", "locked",
                                    String.valueOf(locked))));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(38, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.reset_selected_hider"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_RESET_HIDER, null), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId(), LoadoutRole.HIDER);
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(
                        plugin.tr(p, "gui.admin.players.reset_hider", Map.of("player", selectedTarget.getName())));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(39, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.reset_selected_seeker"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_RESET_SEEKER, null), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId(), LoadoutRole.SEEKER);
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(
                        plugin.tr(p, "gui.admin.players.reset_seeker", Map.of("player", selectedTarget.getName())));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(40, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.reset_selected_all"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_RESET_ALL, null), (p, i, e, s) -> {
            if (selectedTarget != null) {
                loadoutManager.resetPlayerLoadout(selectedTarget.getUniqueId());
                loadoutManager.refreshRoleInventory(selectedTarget);
                p.sendMessage(plugin.tr(p, "gui.admin.players.reset_all", Map.of("player", selectedTarget.getName())));
            }
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(41, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.edit_selected_hider"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_EDIT_HIDER, null), (p, i, e, s) -> {
            plugin.getLogger().info("Test1");
            if (selectedTarget != null) {
                plugin.getLogger().info("Test2");
                openPlayerEditor(p, selectedTarget, LoadoutRole.HIDER);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                p.sendMessage(plugin.tr(p, "gui.admin.players.select_first"));
            }
            e.setCancelled(true);
        }));

        inv.setItem(42, clickable(
                createActionButton(admin, plugin.tr(admin, "gui.admin.players.edit_selected_seeker"), selectedTarget,
                        GUIItems.ADMIN_PLAYERS_EDIT_SEEKER, null), (p, i, e, s) -> {
            if (selectedTarget != null) {
                openPlayerEditor(p, selectedTarget, LoadoutRole.SEEKER);
            } else {
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                p.sendMessage(plugin.tr(p, "gui.admin.players.select_first"));
            }
            e.setCancelled(true);
        }));

        inv.setItem(43,
                clickable(createUtility(GUIItems.ADMIN_PLAYERS_RESET_EVERYONE,
                        plugin.tr(admin, "gui.admin.players.reset_everyone_all"),
                        List.of(plugin.tr(admin, "gui.admin.players.bulk_reset_hint"))), (p, i, e, s) -> {
            int h = loadoutManager.resetAllLoadouts(LoadoutRole.HIDER);
            int sk = loadoutManager.resetAllLoadouts(LoadoutRole.SEEKER);
            int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(plugin.tr(p, "gui.admin.players.bulk_reset_complete",
                            Map.of("hider", String.valueOf(h), "seeker", String.valueOf(sk), "affected",
                                    String.valueOf(affected))));
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        onlinePlayers.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int slot = 9;
        for (Player target : onlinePlayers) {
            if (slot >= 36) {
                break;
            }

            PlayerLoadout loadout = loadoutManager.getLoadout(target.getUniqueId());
            target.getUniqueId();
            ItemStack head = createPlayerHead(admin, target, loadout);
            inv.setItem(slot++, clickable(head, (p, i, e, s) -> {
                selectedTargetByAdmin.put(p.getUniqueId(), target.getUniqueId());
                if (e.getClick() == ClickType.RIGHT) {
                    openPlayerEditor(p, target, LoadoutRole.HIDER);
                } else {
                    p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                    openTab(p, Tab.PLAYERS);
                }
                e.setCancelled(true);
            }));
        }
    }

    private void openPlayerEditor(Player admin, Player target, LoadoutRole role) {
        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_loadout_editor_" + admin.getUniqueId() + "_" + target.getUniqueId() + "_" + role.name().toLowerCase())
                .title(plugin.trText(admin, "gui.admin.editor.title",
                        Map.of("player", target.getName(), "role", role.name())))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(target.getUniqueId());
        Set<LoadoutItemType> selectedItems = role == LoadoutRole.HIDER ? loadout.getHiderItems() : loadout.getSeekerItems();

        inv.setItem(0, clickable(createUtility(GUIItems.ADMIN_EDITOR_BACK, plugin.tr(admin, "gui.admin.editor.back"),
                List.of(plugin.tr(admin, "gui.admin.editor.back_hint"))), (p, i, e, s) -> {
            openTab(p, Tab.PLAYERS);
            e.setCancelled(true);
        }));

        inv.setItem(1, clickable(
                createUtility(GUIItems.ADMIN_EDITOR_SWITCH_ROLE, plugin.tr(admin, "gui.admin.editor.switch_role"),
                List.of(plugin.tr(admin, "gui.admin.editor.current", Map.of("role", role.name())))), (p, i, e, s) -> {
            openPlayerEditor(p, target, role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER);
            e.setCancelled(true);
        }));

        inv.setItem(4, clickable(
                createUtility(GUIItems.ADMIN_EDITOR_SUMMARY,
                        plugin.tr(admin, "gui.admin.editor.summary", Map.of("player", target.getName())),
                        List.of(
                                plugin.tr(admin, "gui.admin.editor.role", Map.of("role", role.name())),
                                plugin.tr(admin, "gui.admin.editor.items",
                                        Map.of("count", String.valueOf(selectedItems.size()))),
                                plugin.tr(admin, "gui.admin.editor.click_hint")
                        )
                ), (p, i, e, s) -> e.setCancelled(true)
        ));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if (role == LoadoutRole.HIDER && !item.isForHiders()) {
                continue;
            }
            if (role == LoadoutRole.SEEKER && !item.isForSeekers()) {
                continue;
            }
            if (slot >= 45) {
                break;
            }
            boolean allowed = loadoutManager.isItemAvailableForRole(role, item);
            boolean selected = selectedItems.contains(item);

            ItemStack stack = createLoadoutPolicyItem(admin, item, role, selected,
                    List.of(
                            plugin.tr(admin, "gui.admin.loadout.item.status", Map.of("status",
                                    selected ? plugin.trText(admin, "common.state.selected") : plugin.trText(admin,
                                            "common.state.not_selected"), "color", selected ? "green" : "gray")),
                            plugin.tr(admin, "gui.admin.loadout.item.allowed", Map.of("state",
                                    allowed ? plugin.trText(admin, "common.state.yes") : plugin.trText(admin,
                                            "common.state.no"))),
                            plugin.tr(admin,
                                    allowed ? "gui.admin.loadout.item.click_toggle" : "gui.admin.loadout.item.blocked_notice")
                    ));

            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                if (!loadoutManager.isItemAvailableForRole(role, item)) {
                    p.sendMessage(plugin.tr(p, "gui.admin.editor.item.blocked_by_policy"));
                    openPlayerEditor(p, target, role);
                    e.setCancelled(true);
                    return;
                }

                PlayerLoadout targetLoadout = loadoutManager.getLoadout(target.getUniqueId());
                if (role == LoadoutRole.HIDER) {
                    if (targetLoadout.getHiderItems().contains(item)) {
                        targetLoadout.removeHiderItem(item);
                    } else {
                        targetLoadout.addHiderItemForced(item, loadoutManager.getItemCost(item));
                    }
                } else {
                    if (targetLoadout.getSeekerItems().contains(item)) {
                        targetLoadout.removeSeekerItem(item);
                    } else {
                        targetLoadout.addSeekerItemForced(item, loadoutManager.getItemCost(item));
                    }
                }
                loadoutManager.saveLoadout(target.getUniqueId());
                loadoutManager.sanitizePlayerLoadout(target.getUniqueId());
                loadoutManager.refreshRoleInventory(target);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
                openPlayerEditor(p, target, role);
                e.setCancelled(true);
            }));
        }
        inv.openForPlayer(admin);
    }

    private void fillPresetTab(Player admin, FrameworkInventory inv) {
        LoadoutRole role = presetRoleByAdmin.getOrDefault(admin.getUniqueId(), LoadoutRole.HIDER);
        boolean restricted = loadoutManager.isRoleRestrictedToAdminPresets(role);
        int forcedSlot = loadoutManager.getForcedRolePresetSlot(role);

        inv.setItem(0,
                clickable(createUtility(GUIItems.ADMIN_PRESETS_TITLE, plugin.tr(admin, "gui.admin.presets.title"),
                List.of(plugin.tr(admin, "gui.admin.presets.hint"))), (p, i, e, s) -> e.setCancelled(true)));

        inv.setItem(1, clickable(
                createUtility(GUIItems.ADMIN_PRESETS_ROLE,
                        plugin.tr(admin, "gui.admin.presets.role", Map.of("role", role.name())),
                        List.of(plugin.tr(admin, "gui.admin.presets.click_to_switch"))), (p, i, e, s) -> {
            LoadoutRole next = role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER;
            presetRoleByAdmin.put(p.getUniqueId(), next);
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(2, clickable(
                createUtility(restricted ? GUIItems.ADMIN_PRESETS_RESTRICT_ON : GUIItems.ADMIN_PRESETS_RESTRICT_OFF,
                plugin.tr(admin, "gui.admin.presets.restrict_players", Map.of("state", plugin.trText(admin, "common.state" + (restricted ? "enabled" : "disabled")))),
                List.of(plugin.tr(admin, "gui.admin.presets.restrict_players_hint"))), (p, i, e, s) -> {
            loadoutManager.setRoleRestrictedToAdminPresets(role, !restricted);
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(plugin.tr(p, "gui.admin.presets.restrict_toggled",
                    Map.of("role", role.name().toLowerCase(), "state", plugin.trText(admin, "common.state" + (restricted ? "enabled" : "disabled")), "count",
                            String.valueOf(affected))));
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(3, clickable(
                createUtility(forcedSlot > 0 ? GUIItems.ADMIN_PRESETS_FORCED_ON : GUIItems.ADMIN_PRESETS_FORCED_OFF,
                plugin.tr(admin, "gui.admin.presets.forced_display",
                        Map.of("slot", forcedSlot > 0 ? String.valueOf(forcedSlot) : "NONE")),
                List.of(plugin.tr(admin, "gui.admin.presets.forced.hint1"),
                        plugin.tr(admin, "gui.admin.presets.forced.hint2"))), (p, i, e, s) -> {
            if (forcedSlot > 0) {
                loadoutManager.setForcedRolePresetSlot(role, 0);
                int affected = loadoutManager.enforcePoliciesAndNotify();
                p.sendMessage(plugin.tr(p, "gui.admin.presets.cleared_forced",
                        Map.of("role", role.name().toLowerCase(), "count", String.valueOf(affected))));
            }
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        int[] presetSlots = {19, 20, 22, 24, 25};
        for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
            int guiSlot = presetSlots[slot - 1];
            AdminRolePreset preset = loadoutManager.getAdminPreset(role, slot);
            boolean enabled = loadoutManager.isAdminPresetEnabled(role, slot);
            boolean forced = forcedSlot == slot;

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.tr(admin, "gui.admin.presets.items",
                    Map.of("count", String.valueOf(preset.getItems().size()))));
            lore.add(plugin.tr(admin, "gui.admin.presets.status", Map.of("state", enabled ? "ENABLED" : "DISABLED")));
            if (!preset.getItems().isEmpty()) {
                lore.add(plugin.tr(admin, "gui.admin.presets.preview_label"));
                int shown = 0;
                for (LoadoutItemType previewItem : preset.getItems()) {
                    lore.add(plugin.tr(admin, "gui.admin.presets.preview_item",
                            Map.of("item", humanize(previewItem.name()))));
                    shown++;
                    if (shown >= 3) break;
                }
                if (preset.getItems().size() > 3) lore.add(plugin.tr(admin, "gui.admin.presets.more",
                        Map.of("count", String.valueOf(preset.getItems().size() - 3))));
            }
            if (forced) lore.add(plugin.tr(admin, "gui.admin.presets.forced_note"));
            lore.add(Component.empty());
            lore.add(plugin.tr(admin, "gui.admin.presets.left_edit"));
            lore.add(plugin.tr(admin, "gui.admin.presets.shift_left_set_forced"));
            lore.add(plugin.tr(admin, "gui.admin.presets.shift_right_toggle"));
            lore.add(plugin.tr(admin, "gui.admin.presets.drop_delete"));

            LoadoutItemType preview = preset.getItems().stream().findFirst().orElse(null);
            ItemStack stack = preview == null
                    ? createUtility(GUIItems.ADMIN_PRESET_ITEM_FALLBACK,
                    plugin.tr(admin, "gui.admin.presets.slot_title", Map.of("slot", String.valueOf(slot))),
                    new ArrayList<>())
                    : getPreviewItemStack(preview);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component titleComp = plugin.tr(admin,
                        forced ? "gui.admin.presets.slot_forced" : (enabled ? "gui.admin.presets.slot_enabled" : "gui.admin.presets.slot_title"),
                        Map.of("slot", String.valueOf(slot)));
                meta.displayName(titleComp.decoration(TextDecoration.ITALIC, false));
                meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());

                meta.setEnchantmentGlintOverride(enabled || forced);

                stack.setItemMeta(meta);
            }
            if (preview != null) {
                CustomModelDataUtil.setCustomModelData(stack, resolveRuntimeItemId(preview), null);
            }

            int targetSlot = slot;
            inv.setItem(guiSlot, clickable(stack, (p, i, e, s) -> {
                if (e.getClick() == ClickType.DROP || e.getClick() == ClickType.CONTROL_DROP) {
                    loadoutManager.deleteAdminPreset(role, targetSlot);
                    int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(plugin.tr(p, "gui.admin.presets.deleted",
                            Map.of("slot", String.valueOf(targetSlot), "count", String.valueOf(affected))));
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                if (e.getClick() == ClickType.SHIFT_RIGHT) {
                    loadoutManager.setAdminPresetEnabled(role, targetSlot, !enabled);
                    int affected = loadoutManager.enforcePoliciesAndNotify();
                    p.sendMessage(plugin.tr(p, "gui.admin.presets.toggled",
                            Map.of("slot", String.valueOf(targetSlot), "state", !enabled ? "enabled" : "disabled",
                                    "count", String.valueOf(affected))));
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                if (e.getClick() == ClickType.SHIFT_LEFT) {
                    if (!loadoutManager.isAdminPresetEnabled(role, targetSlot)) {
                        p.sendMessage(plugin.tr(p, "gui.admin.presets.enable_before_forcing"));
                    } else {
                        loadoutManager.setForcedRolePresetSlot(role, targetSlot);
                        int affected = loadoutManager.enforcePoliciesAndNotify();
                        p.sendMessage(plugin.tr(p, "gui.admin.presets.forced_assigned",
                                Map.of("slot", String.valueOf(targetSlot), "role", role.name().toLowerCase(), "count",
                                        String.valueOf(affected))));
                    }
                    openTab(p, Tab.PRESETS);
                    e.setCancelled(true);
                    return;
                }
                openPresetEditor(p, role, targetSlot);
                e.setCancelled(true);
            }));
        }
    }

    private void openPresetEditor(Player admin, LoadoutRole role, int presetSlot) {
        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("admin_role_preset_editor_" + admin.getUniqueId() + "_" + role.name().toLowerCase() + "_" + presetSlot + "_items")
                .title(plugin.trText(admin, "gui.admin.presets.editor_title",
                        Map.of("slot", String.valueOf(presetSlot), "role", role.name())))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        AdminRolePreset preset = loadoutManager.getAdminPreset(role, presetSlot);

        inv.setItem(0, clickable(createUtility(GUIItems.ADMIN_EDITOR_BACK, plugin.tr(admin, "gui.admin.editor.back"),
                List.of(plugin.tr(admin, "gui.admin.editor.back_hint"))), (p, i, e, s) -> {
            openTab(p, Tab.PRESETS);
            e.setCancelled(true);
        }));

        inv.setItem(1, clickable(
                createUtility(GUIItems.ADMIN_EDITOR_SWITCH_ROLE, plugin.tr(admin, "gui.admin.editor.switch_role"),
                List.of(plugin.tr(admin, "gui.admin.editor.current", Map.of("role", role.name())))), (p, i, e, s) -> {
            openPresetEditor(p, role == LoadoutRole.HIDER ? LoadoutRole.SEEKER : LoadoutRole.HIDER, presetSlot);
            e.setCancelled(true);
        }));

        inv.setItem(4, clickable(
                createUtility(GUIItems.ADMIN_EDITOR_SUMMARY,
                        plugin.tr(admin, "gui.admin.editor.summary", Map.of("slot", String.valueOf(presetSlot))),
                        List.of(
                                plugin.tr(admin, "gui.admin.editor.items",
                                        Map.of("count", String.valueOf(preset.getItems().size()))),
                                plugin.tr(admin, "gui.admin.presets.enabled_for_players", Map.of("enabled",
                                        String.valueOf(loadoutManager.isAdminPresetEnabled(role, presetSlot))))
                        )
                ), (p, i, e, s) -> e.setCancelled(true)
        ));

        int slot = 9;
        for (LoadoutItemType item : LoadoutItemType.values()) {
            if ((role == LoadoutRole.HIDER && !item.isForHiders()) || (role == LoadoutRole.SEEKER && !item.isForSeekers())) {
                continue;
            }
            if (slot >= 45) {
                break;
            }
            boolean selected = preset.getItems().contains(item);
            ItemStack stack = createLoadoutPolicyItem(admin, item, role, selected,
                    List.of(plugin.tr(admin,
                                    selected ? "gui.admin.editor.in_preset" : "gui.admin.editor.not_in_preset"),
                            plugin.tr(admin, "gui.admin.editor.click_to_toggle")));
            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> {
                loadoutManager.toggleAdminPresetItem(role, presetSlot, item);
                loadoutManager.enforcePoliciesAndNotify();
                openPresetEditor(p, role, presetSlot);
                e.setCancelled(true);
            }));
        }

        renderPresetItemPreviewRow(admin, inv, preset);

        plugin.getInventoryFramework().openInventory(admin, inv);
    }

    private void renderPresetItemPreviewRow(Player admin, FrameworkInventory inv, AdminRolePreset preset) {
        int slot = 45;
        int shown = 0;
        for (LoadoutItemType itemType : new ArrayList<>(preset.getItems())) {
            if (shown >= 5) {
                break;
            }
            ItemStack stack = getPreviewItemStack(itemType);
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                Component title = plugin.tr(admin, "gui.admin.item.name", Map.of("name", humanize(itemType.name())));
                meta.displayName(title.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
                meta.lore(List.of(plugin.tr(admin, "gui.admin.presets.forced_note").decoration(TextDecoration.ITALIC,
                        false)));
                stack.setItemMeta(meta);
            }
            CustomModelDataUtil.setCustomModelData(stack, resolveRuntimeItemId(itemType), null);
            inv.setItem(slot++, clickable(stack, (p, i, e, s) -> e.setCancelled(true)));
            shown++;
        }

        while (shown < 5) {
            ItemStack filler = createUtility(GUIItems.ADMIN_PRESETS_EMPTY_PREVIEW,
                    plugin.tr(admin, "gui.admin.presets.empty_preview"),
                    List.of(plugin.tr(admin, "gui.admin.presets.no_item")));
            inv.setItem(slot++, clickable(filler, (p, i, e, s) -> e.setCancelled(true)));
            shown++;
        }
    }

    private void setTabButtons(Player admin, FrameworkInventory inv, Tab activeTab) {
        inv.setItem(45, tabButton(admin, activeTab, Tab.HIDER, GUIItems.ADMIN_TAB_HIDER,
                plugin.tr(admin, "gui.admin.tabs.hider")));
        inv.setItem(46, tabButton(admin, activeTab, Tab.SEEKER, GUIItems.ADMIN_TAB_SEEKER,
                plugin.tr(admin, "gui.admin.tabs.seeker")));
        inv.setItem(47, tabButton(admin, activeTab, Tab.PERKS, GUIItems.ADMIN_TAB_PERKS,
                plugin.tr(admin, "gui.admin.tabs.perks")));
        inv.setItem(48, tabButton(admin, activeTab, Tab.PLAYERS, GUIItems.ADMIN_TAB_PLAYERS,
                plugin.tr(admin, "gui.admin.tabs.players")));
        inv.setItem(49, tabButton(admin, activeTab, Tab.PRESETS, GUIItems.ADMIN_TAB_PRESETS,
                plugin.tr(admin, "gui.admin.tabs.presets")));

        boolean globalLocked = loadoutManager.isGlobalLoadoutLocked();
        inv.setItem(50, clickable(createUtility(
                globalLocked ? GUIItems.ADMIN_GLOBAL_LOCK_ON : GUIItems.ADMIN_GLOBAL_LOCK_OFF,
                plugin.tr(admin, "gui.admin.global_lock.title", Map.of("state", globalLocked ? "ON" : "OFF")),
                List.of(plugin.tr(admin, "gui.admin.global_lock.hint"))), (p, i, e, s) -> {
            loadoutManager.setGlobalLoadoutLocked(!globalLocked);
            String status = !globalLocked ? "locked" : "unlocked";
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, !globalLocked ? 1.0f : 0.8f);
            p.sendMessage(plugin.tr(p, "gui.admin.global_lock.toggled", Map.of("state", status)));
            openTab(p, activeTab);
            e.setCancelled(true);
        }));

        inv.setItem(52, clickable(createUtility(GUIItems.ADMIN_APPLY_CHANGES,
                plugin.tr(admin, "gui.admin.apply_changes.title"),
                List.of(plugin.tr(admin, "gui.admin.apply_changes.hint"))), (p, i, e, s) -> {
            int affected = loadoutManager.enforcePoliciesAndNotify();
            p.sendMessage(plugin.tr(p, "gui.admin.apply_changes.done", Map.of("count", String.valueOf(affected))));
            openTab(p, activeTab);
            e.setCancelled(true);
        }));

        inv.setItem(53, clickable(createUtility(GUIItems.ADMIN_CLOSE,
                        plugin.tr(admin, "gui.admin.close"), List.of()),
                (p, i, e, s) -> {
            p.closeInventory();
            e.setCancelled(true);
        }));
    }

    private InventoryItem tabButton(Player admin, Tab activeTab, Tab tab, String key, Component name) {
        boolean active = activeTab == tab;
        ItemStack stack = createUtility(key, name,
                List.of(plugin.tr(admin, active ? "gui.admin.tabs.current" : "gui.admin.tabs.click_to_switch")));

        if (active) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                stack.setItemMeta(meta);
            }
        }

        return clickable(stack, (p, i, e, s) -> {
            openTab(admin, tab);
            e.setCancelled(true);
        });
    }

    private ItemStack createInfoItem(Player viewer, LoadoutRole role, LoadoutFilterMode mode, int entries) {
        return createUtility(GUIItems.ADMIN_INFO,
                plugin.tr(viewer, "gui.admin.loadout.info.title", Map.of("role", role.name())),
                List.of(
                        plugin.tr(viewer, "gui.admin.loadout.info.mode", Map.of("mode", mode.name())),
                        plugin.tr(viewer, "gui.admin.loadout.info.entries", Map.of("entries", String.valueOf(entries))),
                        plugin.tr(viewer, "gui.admin.loadout.info.changes")
                ));
    }

    private ItemStack createPlayerHead(Player viewer, Player player, PlayerLoadout loadout
    ) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) {
            return item;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(player.getUniqueId());
        skullMeta.setOwningPlayer(offline);
        skullMeta.displayName(plugin.tr(viewer, "gui.admin.players.head_name", Map.of("player", player.getName())));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(viewer, "gui.admin.players.hider_items",
                Map.of("count", String.valueOf(loadout.getHiderItems().size()))));
        lore.add(plugin.tr(viewer, "gui.admin.players.seeker_items",
                Map.of("count", String.valueOf(loadout.getSeekerItems().size()))));

        lore.add(plugin.tr(viewer, "gui.admin.players.hider_locked",
                Map.of("state", String.valueOf(loadout.isHiderLocked()))));
        lore.add(plugin.tr(viewer, "gui.admin.players.seeker_locked",
                Map.of("state", String.valueOf(loadout.isSeekerLocked()))));
        lore.add(plugin.tr(viewer, "gui.admin.players.left_right_hint"));
        skullMeta.lore(lore);
        item.setItemMeta(skullMeta);
        return item;
    }

    private ItemStack createActionButton(Player viewer, Component title, Player target, String key, LoadoutRole role) {
        List<Component> lore = new ArrayList<>();
        lore.add(target == null ? plugin.tr(viewer, "gui.admin.players.select_first") : plugin.tr(viewer,
                "gui.admin.players.target", Map.of("player", target.getName())));
        ItemStack item = createUtility(key, title, lore);
        if (target != null && role != null) {
            boolean lockActive = loadoutManager.isRoleLocked(target.getUniqueId(), role);
            lore.add(plugin.tr(viewer, "gui.admin.players.status",
                    Map.of("status", lockActive ? "LOCKED" : "UNLOCKED")));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
                if (lockActive) meta.setEnchantmentGlintOverride(true);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createLoadoutPolicyItem(Player viewer, LoadoutItemType type, LoadoutRole role, boolean highlighted, List<Component> extraLore) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(plugin.tr(viewer, "gui.admin.item.name", Map.of("name", humanize(type.name()))));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(viewer, "gui.admin.loadout.item.role", Map.of("role", role.name())).decoration(
                TextDecoration.ITALIC, false));
        lore.addAll(extraLore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.lore(lore);


        meta.setEnchantmentGlintOverride(highlighted);


        item.setItemMeta(meta);

        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(false).build());
        return item;
    }

    private ItemStack createPerkPolicyItem(Player viewer, PerkDefinition perk, LoadoutRole role, boolean disabled, List<Component> extraLore) {
        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        Component display = disabled
                ? perk.getDisplayName().decorate(TextDecoration.STRIKETHROUGH)
                : perk.getDisplayName().decoration(TextDecoration.BOLD, true);
        meta.displayName(display.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(viewer, "gui.admin.perks.perk_id", Map.of("id", perk.getId())));
        lore.add(plugin.tr(viewer, "gui.admin.perks.role", Map.of("role", role.name())));
        lore.addAll(extraLore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getPreviewItemStack(LoadoutItemType type) {
        GameItem gameItem = SeekerItems.getItem(type.getItemId());
        if (gameItem == null) {
            gameItem = HiderItems.getItem(type.getItemId());
        }
        if (gameItem == null) {
            return new ItemStack(Material.BARRIER);
        }

        ItemStack stack = gameItem.createItem(plugin);
        return stack == null ? new ItemStack(Material.BARRIER) : stack.clone();
    }

    private String resolveRuntimeItemId(LoadoutItemType type) {
        if (type == LoadoutItemType.SPEED_BOOST) {
            return de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem.ID + "_0";
        }
        if (type == LoadoutItemType.KNOCKBACK_STICK) {
            return de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem.ID + "_1";
        }
        return type.getItemId();
    }

    private ItemStack createUtility(String key, Component title, List<Component> lore) {
        ItemStack item = item(key, new ItemStack(Material.DIRT));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.displayName(title.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.ADMIN_LOADOUT, key, fallback);
    }

    private InventoryItem clickable(ItemStack item, InventoryClickHandler clickHandler) {
        InventoryItem inventoryItem = new InventoryItem(item);
        inventoryItem.setClickHandler(clickHandler);
        inventoryItem.setAllowTakeout(false);
        inventoryItem.setAllowInsert(false);
        return inventoryItem;
    }

    private Tab tabByRole(LoadoutRole role) {
        return role == LoadoutRole.HIDER ? Tab.HIDER : Tab.SEEKER;
    }

    private String humanize(String value) {
        StringBuilder result = new StringBuilder();
        for (String part : value.split("_")) {
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }

    private enum Tab {
        HIDER,
        SEEKER,
        PERKS,
        PLAYERS,
        PRESETS
    }

}

