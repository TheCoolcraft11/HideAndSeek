package de.thecoolcraft11.hideAndSeek;

import de.thecoolcraft11.hideAndSeek.command.*;
import de.thecoolcraft11.hideAndSeek.gui.*;
import de.thecoolcraft11.hideAndSeek.items.*;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectManager;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectSkins;
import de.thecoolcraft11.hideAndSeek.listener.game.*;
import de.thecoolcraft11.hideAndSeek.listener.item.*;
import de.thecoolcraft11.hideAndSeek.listener.perk.GlideListener;
import de.thecoolcraft11.hideAndSeek.listener.perk.PlaceholderItemProtectionListener;
import de.thecoolcraft11.hideAndSeek.listener.player.*;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutDataService;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
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
import de.thecoolcraft11.hideAndSeek.setting.SettingChangeListener;
import de.thecoolcraft11.hideAndSeek.setting.SettingRegistrar;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.SeekingBossBarService;
import de.thecoolcraft11.hideAndSeek.util.map.MapManager;
import de.thecoolcraft11.hideAndSeek.util.points.PointService;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommandRegistry;
import de.thecoolcraft11.timer.Timer;
import de.thecoolcraft11.timer.api.TimerAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public final class HideAndSeek extends MinigameFramework {
    private MapManager mapManager;
    private BlockSelectorGUI blockSelectorGUI;
    private Timer timerPlugin;
    private TimerAPI api;
    private BlockModeListener blockModeListener;
    private LoadoutManager loadoutManager;
    private LoadoutGUI loadoutGUI;
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
    private SeekingBossBarService seekingBossBarService;
    private PerkService perkService;

    @Override
    protected void onGameEnable() {

        DataController.getInstance().setup();
        ItemSkinSelectionService.initialize(this);
        LoadoutDataService.initialize(this);
        mapManager = new MapManager(this);
        blockSelectorGUI = new BlockSelectorGUI(this);
        loadoutManager = new LoadoutManager(this);
        loadoutGUI = new LoadoutGUI(loadoutManager, this);
        mapGUI = new MapGUI(this);
        skinGUI = new SkinGUI(this);
        pointService = new PointService(this);
        perkService = new PerkService(this);
        voteManager = new VoteManager(this);
        voteGUI = new VoteGUI(this);
        readyGUI = new ReadyGUI(this);
        seekingBossBarService = new SeekingBossBarService(this);

        nmsAdapter = NmsLoader.load(getLogger(), getConfig().getBoolean("nms.enabled", true));

        mapManager.loadDisallowedBlockStates();

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

        blockModeListener = new BlockModeListener(this);
        playerHitListener = new PlayerHitListener(this);
        antiCheatVisibilityListener = new AntiCheatVisibilityListener(this);
        hiderCampingListener = new HiderCampingListener(this, playerHitListener);

        Bukkit.getPluginManager().registerEvents(playerHitListener, this);
        Bukkit.getPluginManager().registerEvents(new EnvironmentalDeathMessageListener(playerHitListener), this);
        Bukkit.getPluginManager().registerEvents(new GameStateListener(this), this);
        Bukkit.getPluginManager().registerEvents(blockModeListener, this);
        Bukkit.getPluginManager().registerEvents(antiCheatVisibilityListener, this);
        Bukkit.getPluginManager().registerEvents(hiderCampingListener, this);
        Bukkit.getPluginManager().registerEvents(new HiderEquipmentChangeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CrossbowTrackerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CameraViewListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AssistantProjectileListener(this), this);

        Bukkit.getPluginManager().registerEvents(new SeekerKillModeListener(this), this);
        Bukkit.getPluginManager().registerEvents(new HiderTotemListener(), this);
        Bukkit.getPluginManager().registerEvents(new LightningListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SlownessBallListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SmokeBombListener(this), this);
        Bukkit.getPluginManager().registerEvents(new SetPhaseReadinessGuardListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerSpectateListener(), this);
        Bukkit.getPluginManager().registerEvents(new TrapMovementListener(), this);
        Bukkit.getPluginManager().registerEvents(new PlaceholderItemProtectionListener(), this);
        Bukkit.getPluginManager().registerEvents(new GlideListener(), this);


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

        timerPlugin = (Timer) Bukkit.getPluginManager().getPlugin("Timer");
        if (timerPlugin != null) {
            api = timerPlugin.getAPI();
        }

        updateWorldIconsForAllMaps();

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
        if (seekingBossBarService != null) {
            seekingBossBarService.stopSeekingSession();
        }
        if (perkService != null) {
            perkService.shutdown();
        }
        KillEffectManager.clear();
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
