package de.thecoolcraft11.hideAndSeek;

import de.thecoolcraft11.hideAndSeek.command.*;
import de.thecoolcraft11.hideAndSeek.gui.BlockSelectorGUI;
import de.thecoolcraft11.hideAndSeek.gui.LoadoutGUI;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.listener.*;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.phase.EndedPhase;
import de.thecoolcraft11.hideAndSeek.phase.HidingPhase;
import de.thecoolcraft11.hideAndSeek.phase.LobbyPhase;
import de.thecoolcraft11.hideAndSeek.phase.SeekingPhase;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.MapData;
import de.thecoolcraft11.hideAndSeek.util.MapManager;
import de.thecoolcraft11.hideAndSeek.util.setting.SettingChangeListener;
import de.thecoolcraft11.hideAndSeek.util.setting.SettingRegisterer;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommandRegistry;
import de.thecoolcraft11.timer.Timer;
import de.thecoolcraft11.timer.api.TimerAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class HideAndSeek extends MinigameFramework {
    private MapManager mapManager;
    private BlockSelectorGUI blockSelectorGUI;
    private Timer timerPlugin;
    private TimerAPI api;
    private BlockModeListener blockModeListener;
    private LoadoutManager loadoutManager;
    private LoadoutGUI loadoutGUI;

    @Override
    protected void onGameEnable() {

        DataController.getInstance().setup();
        mapManager = new MapManager(this);
        blockSelectorGUI = new BlockSelectorGUI(this);
        loadoutManager = new LoadoutManager(this);
        loadoutGUI = new LoadoutGUI(this, loadoutManager);


        mapManager.loadDisallowedBlockStates();

        getStateManager().registerPhases(
                new LobbyPhase(),
                new HidingPhase(),
                new SeekingPhase(),
                new EndedPhase()
        );

        getStateManager().setPhase("lobby");

        SettingRegisterer.registerSettings(this);
        SettingChangeListener.register(this);

        HiderItems.registerItems(this);
        SeekerItems.registerItems(this);

        blockModeListener = new BlockModeListener(this);

        Bukkit.getPluginManager().registerEvents(new PlayerHitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GameStateListener(this), this);
        Bukkit.getPluginManager().registerEvents(blockModeListener, this);
        Bukkit.getPluginManager().registerEvents(new HiderEquipmentChangeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrossbowTrackerListener(this), this);
        
        Bukkit.getPluginManager().registerEvents(new SeekerKillModeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HiderTotemListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LightningListener(this), this);
        Bukkit.getPluginManager().registerEvents(loadoutGUI, this);


        registerMapSelectionMenu();
        registerLoadoutMenu();

        MinigameSubcommandRegistry.register(new ChooseBlockCommand(this));
        MinigameSubcommandRegistry.register(new ChooseAppearanceCommand(this));
        MinigameSubcommandRegistry.register(new BlockStatsCommand(this));
        MinigameSubcommandRegistry.register(new MapCommand(this));
        MinigameSubcommandRegistry.register(new LoadoutCommand(this));

        timerPlugin = (Timer) Bukkit.getPluginManager().getPlugin("Timer");
        if (timerPlugin != null) {
            api = timerPlugin.getAPI();
        }

        getLogger().info("Hide and Seek enabled with all features!");
    }

    @Override
    protected void onGameDisable() {

        if (blockModeListener != null) {
            blockModeListener.cancelSneakTimerTask();
        }
    }

    @Override
    protected String getGameName() {
        return "HideAndSeek";
    }

    public static DataController getDataController() {
        return DataController.getInstance();
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public BlockSelectorGUI getBlockSelectorGUI() {
        return blockSelectorGUI;
    }

    public TimerAPI getTimerApi() {
        return api;
    }

    public Timer getTimerPlugin() {
        return timerPlugin;
    }

    public BlockModeListener getBlockModeListener() {
        return blockModeListener;
    }

    public LoadoutManager getLoadoutManager() {
        return loadoutManager;
    }

    public LoadoutGUI getLoadoutGUI() {
        return loadoutGUI;
    }

    private void registerLoadoutMenu() {
        ItemStack loadoutItem = createMapMenuItem(Material.CHEST, "Kit Selector", "Click to choose your loadout");
        getCustomMenuItemRegistry().register(
                "loadout_menu",
                19,
                new de.thecoolcraft11.minigameframework.inventory.InventoryItem(loadoutItem).onClick((p, type) -> {
                    if (type == org.bukkit.event.inventory.ClickType.LEFT) {
                        loadoutGUI.open(p);
                    }
                })
        );
    }

    private void registerMapSelectionMenu() {
        ItemStack mapSelectorItem = createMapMenuItem(Material.MAP, "Map Selector", "Click to open map selection");
        getCustomMenuItemRegistry().register(
                "map_selection_menu",
                18,
                new de.thecoolcraft11.minigameframework.inventory.InventoryItem(mapSelectorItem).onClick((p, type) -> {
                    if (type == org.bukkit.event.inventory.ClickType.LEFT) {
                        openMapSelectionGUI(p);
                    }
                })
        );
    }

    public void openMapSelectionGUI(org.bukkit.entity.Player player) {
        List<String> availableMaps = getMapManager().getAvailableMaps();
        int rows = Math.max(3, (availableMaps.size() + 9) / 9);

        var inventory = getInventoryFramework().create("Select Map", rows);
        inventory.setSetting("allow_outside_clicks", false);
        inventory.setSetting("allow_drag", false);
        inventory.setSetting("allow_player_inventory_interaction", false);


        String currentMapName = getDataController().getCurrentMapName();


        boolean isRandomSelected = (currentMapName == null || currentMapName.isEmpty());
        ItemStack randomMapItem = createMapMenuItem(
                Material.COMPASS,
                "Random Map",
                isRandomSelected ? "Currently selected" : "Randomly select a map",
                isRandomSelected
        );
        inventory.setItem(0, new de.thecoolcraft11.minigameframework.inventory.InventoryItem(randomMapItem).onClick((p, type) -> {
            if (type == org.bukkit.event.inventory.ClickType.LEFT) {
                selectRandomMap(p);
            }
        }));


        int slot = 1;
        for (String mapName : availableMaps) {
            boolean isCurrentMap = mapName.equals(currentMapName);


            MapData mapData = getMapManager().getMapData(mapName);
            String description = isCurrentMap ? "§a§lCurrently selected" : "Click to select";
            if (mapData != null && !mapData.getDescription().isEmpty()) {
                description = mapData.getDescription() + "\n" + description;
            }

            ItemStack mapItem = createMapMenuItem(
                    Material.GRASS_BLOCK,
                    mapName,
                    description,
                    isCurrentMap
            );


            final String selectedMap = mapName;
            inventory.setItem(slot, new de.thecoolcraft11.minigameframework.inventory.InventoryItem(mapItem).onClick((p, type) -> {
                if (type == org.bukkit.event.inventory.ClickType.LEFT) {
                    selectSpecificMap(p, selectedMap);
                }
            }));
            slot++;
        }

        getInventoryFramework().openInventory(player, inventory);
    }

    private void selectRandomMap(Player player) {

        getDataController().setCurrentMapName(null);

        player.sendMessage(net.kyori.adventure.text.Component.text("Map selection: ", net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .append(net.kyori.adventure.text.Component.text("Random", net.kyori.adventure.text.format.NamedTextColor.GOLD)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();


        org.bukkit.Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                openMapSelectionGUI(player);
            }
        }, 1L);
    }

    private void selectSpecificMap(Player player, String mapName) {
        org.bukkit.World sourceWorld = org.bukkit.Bukkit.getWorld(mapName);
        if (sourceWorld == null) {
            player.sendMessage(net.kyori.adventure.text.Component.text("Map '" + mapName + "' not found or not loaded!", net.kyori.adventure.text.format.NamedTextColor.RED));
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }


        getDataController().setCurrentMapName(mapName);

        player.sendMessage(net.kyori.adventure.text.Component.text("Map selected: ", net.kyori.adventure.text.format.NamedTextColor.GREEN)
                .append(net.kyori.adventure.text.Component.text(mapName, net.kyori.adventure.text.format.NamedTextColor.GOLD)));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();


        org.bukkit.Bukkit.getScheduler().runTaskLater(this, () -> {
            if (player.isOnline()) {
                openMapSelectionGUI(player);
            }
        }, 1L);
    }

    private ItemStack createMapMenuItem(org.bukkit.Material material, String name, String description) {
        return createMapMenuItem(material, name, description, false);
    }

    private ItemStack createMapMenuItem(org.bukkit.Material material, String name, String description, boolean highlight) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            net.kyori.adventure.text.format.NamedTextColor nameColor = highlight ?
                    net.kyori.adventure.text.format.NamedTextColor.GREEN :
                    net.kyori.adventure.text.format.NamedTextColor.AQUA;

            meta.displayName(net.kyori.adventure.text.Component.text(name, nameColor, net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text(description, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            if (highlight) {
                lore.add(net.kyori.adventure.text.Component.text("Selected", net.kyori.adventure.text.format.NamedTextColor.GREEN)
                        .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

                meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
