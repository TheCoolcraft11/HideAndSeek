package de.thecoolcraft11.hideAndSeek;

import de.thecoolcraft11.hideAndSeek.command.*;
import de.thecoolcraft11.hideAndSeek.gui.*;
import de.thecoolcraft11.hideAndSeek.items.*;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectManager;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectSkins;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageManager;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageSkins;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkinManager;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkinSkins;
import de.thecoolcraft11.hideAndSeek.items.hider.RemoteGatewayItem;
import de.thecoolcraft11.hideAndSeek.listener.game.*;
import de.thecoolcraft11.hideAndSeek.listener.item.*;
import de.thecoolcraft11.hideAndSeek.listener.perk.PerkListener;
import de.thecoolcraft11.hideAndSeek.listener.perk.PlaceholderItemProtectionListener;
import de.thecoolcraft11.hideAndSeek.listener.player.*;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutDataService;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsLoader;
import de.thecoolcraft11.hideAndSeek.perk.PerkRegistry;
import de.thecoolcraft11.hideAndSeek.perk.PerkService;
import de.thecoolcraft11.hideAndSeek.perk.PerkShopUI;
import de.thecoolcraft11.hideAndSeek.perk.PerkStateManager;
import de.thecoolcraft11.hideAndSeek.phase.EndedPhase;
import de.thecoolcraft11.hideAndSeek.phase.HidingPhase;
import de.thecoolcraft11.hideAndSeek.phase.LobbyPhase;
import de.thecoolcraft11.hideAndSeek.phase.SeekingPhase;
import de.thecoolcraft11.hideAndSeek.placeholder.HASExpansion;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerDataStore;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerDataStoreFactory;
import de.thecoolcraft11.hideAndSeek.setting.SettingChangeListener;
import de.thecoolcraft11.hideAndSeek.setting.SettingRegistrar;
import de.thecoolcraft11.hideAndSeek.tab.CustomScoreboardProvider;
import de.thecoolcraft11.hideAndSeek.tab.CustomTabProvider;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.SeekingBossBarService;
import de.thecoolcraft11.hideAndSeek.util.UnstuckManager;
import de.thecoolcraft11.hideAndSeek.util.map.MapManager;
import de.thecoolcraft11.hideAndSeek.util.points.PointService;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommandRegistry;
import de.thecoolcraft11.timer.Timer;
import de.thecoolcraft11.timer.api.TimerAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;

public final class HideAndSeek extends MinigameFramework {
    private MapManager mapManager;
    private BlockSelectorGUI blockSelectorGUI;
    private Timer timerPlugin;
    private TimerAPI api;
    private BlockModeListener blockModeListener;
    private LoadoutManager loadoutManager;
    private LoadoutGUI loadoutGUI;
    private AdminLoadoutManagementGUI adminLoadoutManagementGUI;
    private MapGUI mapGUI;
    private SkinGUI skinGUI;
    private PointService pointService;
    private NmsAdapter nmsAdapter;
    private PlayerHitListener playerHitListener;
    private int worldBorderCheckTaskId = -1;
    private VoteManager voteManager;
    private VoteGUI voteGUI;
    private ReadyGUI readyGUI;
    private AntiCheatVisibilityListener antiCheatVisibilityListener;
    private HiderCampingListener hiderCampingListener;
    private EnvironmentalDamageListener environmentalDamageListener;
    private SeekingBossBarService seekingBossBarService;
    private PerkService perkService;
    private UnstuckManager unstuckManager;
    private CustomTabProvider tabProvider;
    private CustomScoreboardProvider scoreboardProvider;
    private PlayerDataStore playerDataStore;

    @Override
    protected void onGameEnable() {

        if (getResource("maps.yml") != null && !(new File(getDataFolder(), "maps.yml").exists())) {
            saveResource("maps.yml", false);
        }

        DataController.getInstance().setup();
        playerDataStore = PlayerDataStoreFactory.create(this);
        ItemSkinSelectionService.initialize(this);
        LoadoutDataService.initialize(this);
        mapManager = new MapManager(this);
        blockSelectorGUI = new BlockSelectorGUI(this);
        loadoutManager = new LoadoutManager(this);
        loadoutGUI = new LoadoutGUI(loadoutManager, this);
        adminLoadoutManagementGUI = new AdminLoadoutManagementGUI(this);
        mapGUI = new MapGUI(this);
        skinGUI = new SkinGUI(this);
        pointService = new PointService(this);
        perkService = new PerkService(this);
        voteManager = new VoteManager(this);
        voteGUI = new VoteGUI(this);
        readyGUI = new ReadyGUI(this);
        seekingBossBarService = new SeekingBossBarService(this);
        unstuckManager = new UnstuckManager(this);

        nmsAdapter = NmsLoader.load(getLogger(), getConfig().getBoolean("nms.enabled", true));

        nmsAdapter.setCameraSessionChecker(ItemStateManager.activeCameraSessions::containsKey);

        mapManager.loadDisallowedBlockStates();

        timerPlugin = (Timer) Bukkit.getPluginManager().getPlugin("Timer");
        if (timerPlugin != null) {
            api = timerPlugin.getAPI();
        }

        getStateManager().registerPhases(
                new LobbyPhase(),
                new HidingPhase(),
                new SeekingPhase(),
                new EndedPhase()
        );

        getStateManager().setPhase("lobby");

        SettingRegistrar.registerAll(this);
        SettingChangeListener.register(this);

        HiderItems.registerItems(this);
        SeekerItems.registerItems(this);

        HiderItemSkins.registerAll(this);
        SeekerItemSkins.registerAll(this);
        KillEffectSkins.registerAll();
        WinSkinSkins.registerAll();
        DeathMessageSkins.registerAll();

        blockModeListener = new BlockModeListener(this);
        playerHitListener = new PlayerHitListener(this);
        antiCheatVisibilityListener = new AntiCheatVisibilityListener(this);
        hiderCampingListener = new HiderCampingListener(this, playerHitListener);
        environmentalDamageListener = new EnvironmentalDamageListener(this, playerHitListener);
        tabProvider = new CustomTabProvider(this, getConfig());
        scoreboardProvider = new CustomScoreboardProvider(this);

        Bukkit.getPluginManager().registerEvents(playerHitListener, this);
        Bukkit.getPluginManager().registerEvents(new EnvironmentalDeathMessageListener(playerHitListener, playerHitListener.getDeathMessageService()), this);
        Bukkit.getPluginManager().registerEvents(new GameStateListener(this), this);
        Bukkit.getPluginManager().registerEvents(blockModeListener, this);
        Bukkit.getPluginManager().registerEvents(antiCheatVisibilityListener, this);
        Bukkit.getPluginManager().registerEvents(hiderCampingListener, this);
        Bukkit.getPluginManager().registerEvents(environmentalDamageListener, this);
        Bukkit.getPluginManager().registerEvents(new HiderEquipmentChangeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrossbowTrackerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CameraViewListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AssistantProjectileListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ScoreboardStatusListener(this), this);

        Bukkit.getPluginManager().registerEvents(new SeekerKillModeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HiderTotemListener(), this);
        Bukkit.getPluginManager().registerEvents(new LightningListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SlownessBallListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SmokeBombListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PhantomViewerMapListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SetPhaseReadinessGuardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerSpectateListener(), this);
        Bukkit.getPluginManager().registerEvents(new TrapMovementListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlaceholderItemProtectionListener(perkService.getShopUI()), this);
        Bukkit.getPluginManager().registerEvents(new PerkListener(this, perkService), this);
        Bukkit.getPluginManager().registerEvents(new PlayerTeamListener(this), this);


        worldBorderCheckTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this,
                () -> playerHitListener.checkWorldBorderDamage(),
                0L,
                5L
        );

        registerMapSelectionMenu();
        registerLoadoutMenu();
        registerSkinMenu();

        MinigameSubcommandRegistry.register(new MapCommand(this));
        MinigameSubcommandRegistry.register(new LoadoutCommand(this));
        MinigameSubcommandRegistry.register(new ItemSkinCommand(this));
        MinigameSubcommandRegistry.register(new VoteCommand(this));
        MinigameSubcommandRegistry.register(new ReadyCommand(this));
        MinigameSubcommandRegistry.register(new DebugCommand(this));
        MinigameSubcommandRegistry.register(new UnstuckCommand(this, unstuckManager));

        unstuckManager.startTrackingTask();

        getWikiRegistry().addPlaceholderResolver((player, key) -> {
            if (key.startsWith("setting-")) {
                return Component.text(getSettingRegistry().get(key.substring("setting-".length())).toString());
            }
            if (key.startsWith("rarity-")) {
                if (key.startsWith("item-")) {
                    LoadoutItemType itemType = LoadoutItemType.fromID("has_hider_" + key.substring("rarity-".length()));
                    if (itemType == null) {
                        itemType = LoadoutItemType.fromID("has_seeker_" + key.substring("rarity-".length()));
                    }
                    if (itemType != null) {
                        return Component.text(itemType.getRarity().name(), switch (itemType.getRarity()) {
                            case COMMON -> NamedTextColor.WHITE;
                            case UNCOMMON -> NamedTextColor.GREEN;
                            case RARE -> NamedTextColor.BLUE;
                            case EPIC -> NamedTextColor.LIGHT_PURPLE;
                            case LEGENDARY -> NamedTextColor.GOLD;
                        });
                    }
                }
            }
            return Component.empty();
        });

        getWikiRegistry().addItemPlaceholderResolver((player, key) -> {
            if (key.startsWith("item-")) {
                GameItem gameItem = SeekerItems.getItem("has_seeker_" + key.substring("item-".length()));
                if (gameItem == null) {
                    gameItem = HiderItems.getItem("has_hider_" + key.substring("item-".length()));
                }
                if (gameItem == null) return new ItemStack(Material.BARRIER);
                return gameItem.createItem(this);
            }
            return new ItemStack(Material.AIR);
        });


        updateWorldIconsForAllMaps();

        new BukkitRunnable() {

            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(tabProvider::updateTab);
            }
        }.runTaskTimer(this, 0L, 1L);
        new BukkitRunnable() {

            @Override
            public void run() {
                Bukkit.getOnlinePlayers().forEach(scoreboardProvider::updateTitle);
            }
        }.runTaskTimer(this, 0L, 1L);


        scoreboardProvider.startAutoUpdate(20);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new HASExpansion().register();
        }

        getLogger().info("Hide and Seek enabled with all features!");
    }

    @Override
    protected void onGameDisable() {

        if (blockModeListener != null) {
            blockModeListener.cancelSneakTimerTask();
        }
        if (worldBorderCheckTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(worldBorderCheckTaskId);
        }
        if (antiCheatVisibilityListener != null) {
            antiCheatVisibilityListener.shutdown();
        }
        if (hiderCampingListener != null) {
            hiderCampingListener.shutdown();
        }
        if (environmentalDamageListener != null) {
            environmentalDamageListener.shutdown();
        }
        if (seekingBossBarService != null) {
            seekingBossBarService.stopSeekingSession();
        }
        if (perkService != null) {
            perkService.shutdown();
        }
        if (unstuckManager != null) {
            unstuckManager.shutdown();
        }
        KillEffectManager.clear();
        WinSkinManager.clear();
        DeathMessageManager.clear();
        RemoteGatewayItem.clearAllGateways();
        ItemSkinSelectionService.shutdown(this);
        LoadoutDataService.shutdown(this);
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

    public AdminLoadoutManagementGUI getAdminLoadoutManagementGUI() {
        return adminLoadoutManagementGUI;
    }

    public MapGUI getMapGUI() {
        return mapGUI;
    }

    public SkinGUI getSkinGUI() {
        return skinGUI;
    }

    public PointService getPointService() {
        return pointService;
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
                        mapGUI.open(p);
                    }
                })
        );
    }

    private void registerSkinMenu() {
        ItemStack skinItem = createMapMenuItem(Material.ARMOR_STAND, "Skin Selector", "Click to choose item skins");
        getCustomMenuItemRegistry().register(
                "skin_selection_menu",
                20,
                new de.thecoolcraft11.minigameframework.inventory.InventoryItem(skinItem).onClick((p, type) -> {
                    if (type == org.bukkit.event.inventory.ClickType.LEFT) {
                        skinGUI.open(p);
                    }
                })
        );
    }

    private ItemStack createMapMenuItem(org.bukkit.Material material, String name, String description) {
        ItemStack item = new ItemStack(material);
        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            net.kyori.adventure.text.format.NamedTextColor nameColor =
                    net.kyori.adventure.text.format.NamedTextColor.AQUA;

            meta.displayName(net.kyori.adventure.text.Component.text(name, nameColor, net.kyori.adventure.text.format.TextDecoration.BOLD)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            java.util.List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
            lore.add(net.kyori.adventure.text.Component.text(description, net.kyori.adventure.text.format.NamedTextColor.GRAY)
                    .decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    public NmsAdapter getNmsAdapter() {
        return nmsAdapter;
    }

    public VoteManager getVoteManager() {
        return voteManager;
    }

    public VoteGUI getVoteGUI() {
        return voteGUI;
    }

    public ReadyGUI getReadyGUI() {
        return readyGUI;
    }

    public AntiCheatVisibilityListener getAntiCheatVisibilityListener() {
        return antiCheatVisibilityListener;
    }

    public PlayerHitListener getPlayerHitListener() {
        return playerHitListener;
    }

    public SeekingBossBarService getSeekingBossBarService() {
        return seekingBossBarService;
    }

    public PerkService getPerkService() {
        return perkService;
    }

    public PerkRegistry getPerkRegistry() {
        return perkService.getRegistry();
    }

    public PerkStateManager getPerkStateManager() {
        return perkService.getStateManager();
    }

    public PerkShopUI getPerkShopUI() {
        return perkService.getShopUI();
    }

    public UnstuckManager getUnstuckManager() {
        return unstuckManager;
    }

    public CustomScoreboardProvider getScoreboardProvider() {
        return scoreboardProvider;
    }

    public PlayerDataStore getPlayerDataStore() {
        return playerDataStore;
    }

    public void updateWorldIconsForAllMaps() {
        if (mapManager == null) {
            return;
        }

        for (String mapName : mapManager.getAvailableMaps()) {
            if (mapName != null && !mapName.isBlank()) {
                Material icon = mapManager.getMapIconMaterial(mapName, Material.GRASS_BLOCK);
                getWorldManager().setWorldIcon(mapName, icon);
            }
        }
    }
}
