package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockListParser;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.seeker.SeekersSwordItem;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.model.MapInfoDisplayMode;
import de.thecoolcraft11.hideAndSeek.util.TimerManager;
import de.thecoolcraft11.hideAndSeek.util.map.MapConfigHelper;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class HidingPhase implements GamePhase {
    private BukkitTask pointsTask;
    private final Map<BukkitTask, Location> blockTasks = new HashMap<>();
    private final Set<Material> allowedMaterials = new HashSet<>();
    private final Set<Material> blockInteractionExceptions = new LinkedHashSet<>();
    private final Set<Material> blockPhysicsExceptions = new LinkedHashSet<>();


    @Override
    public String getId() {
        return "hiding";
    }

    @Override
    public String getDisplayName() {
        return "Hiding";
    }

    @Override
    public void onStart(MinigameFramework plugin) {

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        hideAndSeekPlugin.getAntiCheatVisibilityListener().resetNow();
        hideAndSeekPlugin.getPointService().resetRoundState();

        TimerManager.cleanupTimers(hideAndSeekPlugin);
        HideAndSeek.getDataController().setRoundStartTime(System.currentTimeMillis());

        String selectedMapName = HideAndSeek.getDataController().getCurrentMapName();
        World gameWorld;

        if (selectedMapName != null && !selectedMapName.isEmpty()) {

            if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Using selected map: " + selectedMapName);
            }
            gameWorld = hideAndSeekPlugin.getMapManager().copyMapToWorkingWorld(selectedMapName);
        } else {

            if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("No specific map selected, choosing random map");
            }
            gameWorld = hideAndSeekPlugin.getMapManager().selectAndCopyMap(Bukkit.getOnlinePlayers().size());


            if (gameWorld != null) {
                String[] parts = gameWorld.getName().split("_", 2);
                if (parts.length > 1) {
                    HideAndSeek.getDataController().setCurrentMapName(parts[1]);
                }
            }
        }

        if (gameWorld != null) {
            hideAndSeekPlugin.getMapManager().teleportPlayersToMap(gameWorld);
        } else {
            plugin.getLogger().severe("Failed to create game world!");
        }


        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();
        MapData currentMapData = null;
        if (currentMapName != null && !currentMapName.isEmpty()) {
            currentMapData = hideAndSeekPlugin.getMapManager().getMapData(currentMapName);
        }

        hideAndSeekPlugin.getMapManager().applySettingOverridesForMap(currentMapName);
        generateAllowedBreakBlocks(hideAndSeekPlugin, currentMapName);
        generateBlockExceptionMaterials(hideAndSeekPlugin, currentMapName);


        int timeRemaining = MapConfigHelper.getHidingTime(plugin, currentMapData);
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Hiding phase started - " + timeRemaining + " seconds");
        }

        sendRoundStartAnnouncementChat(plugin, currentMapData, currentMapName);
        final MapData mapDataForAnnouncement = currentMapData;
        final String mapNameForAnnouncement = currentMapName;
        Bukkit.getScheduler().runTaskLater(plugin, () -> showRoundStartMapInfoTitle(plugin, mapDataForAnnouncement, mapNameForAnnouncement), 90L);


        var invisibilityResult = plugin.getSettingService().getSetting("game.hider-invisibility");
        Object invisibilityObj = invisibilityResult.isSuccess() ? invisibilityResult.getValue() : false;
        boolean grantInvisibility = (invisibilityObj instanceof Boolean) ? (Boolean) invisibilityObj : false;
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Invisibility setting: " + grantInvisibility);
        }


        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum gameMode = (gameModeObj instanceof GameModeEnum) ?
                (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;
        plugin.getLogger().info("Game mode: " + gameMode);

        var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
        if (statsService != null) {
            statsService.onRoundStarted(
                    currentMapName,
                    gameMode.name(),
                    HideAndSeek.getDataController().getHiders(),
                    HideAndSeek.getDataController().getSeekers()
            );
        }


        if (gameMode == GameModeEnum.SMALL) {
            var seekerSizeResult = plugin.getSettingService().getSetting("game.small-mode.seeker-size");
            Object seekerSizeObj = seekerSizeResult.isSuccess() ? seekerSizeResult.getValue() : 1.0;
            if ((seekerSizeObj instanceof Number)) {
                ((Number) seekerSizeObj).doubleValue();
            }
        }


        if (gameMode == GameModeEnum.BLOCK) {

            for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                if (HideAndSeek.getDataController().getChosenBlock(hiderId) == null) {

                    Material defaultBlock = Material.STONE;
                    if (currentMapName != null && !currentMapName.isEmpty()) {
                        defaultBlock = hideAndSeekPlugin.getMapManager().getDefaultAllowedBlock(currentMapName);
                    }

                    HideAndSeek.getDataController().setChosenBlock(hiderId, defaultBlock);
                    HideAndSeek.getDataController().setChosenBlockData(hiderId, defaultBlock.createBlockData());
                    Player hider = Bukkit.getPlayer(hiderId);
                    if (hider != null) {
                        HiderItems.updateAppearanceItem(hider, hideAndSeekPlugin);
                        hider.sendMessage(Component.text(
                                "Default block set to " + defaultBlock.name() + ". Use the Block Selector to change your block.",
                                NamedTextColor.YELLOW));
                    }
                }
            }
        }


        var sizeResult = plugin.getSettingService().getSetting("game.small-mode.hider-size");
        Object sizeObj = sizeResult.isSuccess() ? sizeResult.getValue() : 0.5;
        double sizeModifier = (sizeObj instanceof Double) ? (Double) sizeObj : 0.5;
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Size modifier: " + sizeModifier);
        }


        var hiderHealthResult = plugin.getSettingService().getSetting("game.hiders.health");
        Object hiderHealthObj = hiderHealthResult.isSuccess() ? hiderHealthResult.getValue() : 20;
        double hiderHealth = (hiderHealthObj instanceof Integer) ? (Integer) hiderHealthObj : 20;
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Hider health: " + hiderHealth);
        }


        for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker != null && seeker.isOnline()) {
                seeker.getInventory().clear();
                seeker.clearActivePotionEffects();
                seeker.setGameMode(GameMode.ADVENTURE);
                seeker.setWalkSpeed(0f);
                seeker.setAllowFlight(false);
                seeker.setFlying(false);
                seeker.sendMessage(Component.text("Wait for the hiders to hide...", NamedTextColor.RED));
                seeker.setGlowing(false);
                Objects.requireNonNull(seeker.getAttribute(Attribute.SCALE)).setBaseValue(1.0);


                seeker.addPotionEffect(new PotionEffect(
                        PotionEffectType.SATURATION,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false
                ));

                seeker.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS,
                        Integer.MAX_VALUE,
                        255,
                        false,
                        false
                ));

                SeekerItems.applyMask(seeker, hideAndSeekPlugin);

                BlockData blockData = Material.BLACK_CONCRETE.createBlockData();

                Location base = seeker.getLocation().clone().subtract(0, 1, 0).getBlock().getLocation();
                seeker.setAllowFlight(true);

                BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                    seeker.teleport(base.clone().add(0.5, 0, 0.5));
                    seeker.sendBlockChange(base, blockData);
                    seeker.sendBlockChange(base.clone().add(0, 1, 0), blockData);
                }, 2L, 4L);

                blockTasks.put(task, base);

            }
        }


        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline()) {
                hider.getInventory().clear();
                hider.clearActivePotionEffects();
                hider.setGameMode(GameMode.SURVIVAL);
                hider.setWalkSpeed(0.2f);
                hider.setGlowing(false);

                var maxHealth = hider.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (maxHealth != null) {
                    maxHealth.setBaseValue(hiderHealth);
                }
                hider.setHealth(Math.min(hider.getHealth(), hiderHealth));
                if (hider.getHealth() < hiderHealth) {
                    hider.setHealth(hiderHealth);
                }


                hider.addPotionEffect(new PotionEffect(
                        PotionEffectType.SATURATION,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false
                ));

                boolean shouldBeInvisible = grantInvisibility || gameMode == GameModeEnum.BLOCK;

                if (shouldBeInvisible) {
                    try {
                        hider.addPotionEffect(new PotionEffect(
                                PotionEffectType.INVISIBILITY,
                                Integer.MAX_VALUE,
                                0,
                                false,
                                false
                        ));
                        if (gameMode == GameModeEnum.BLOCK) {
                            hider.sendMessage(
                                    Component.text("Choose your block with the block selector", NamedTextColor.GREEN));
                        } else {
                            hider.sendMessage(Component.text("You are invisible! Use this time to hide!", NamedTextColor.GREEN));
                        }
                        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info(hider.getName() + " granted invisibility");
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to grant invisibility to " + hider.getName() + ": " + e.getMessage());
                    }
                } else {
                    hider.sendMessage(Component.text("Hide now! You have " + timeRemaining + " seconds!", NamedTextColor.GREEN));
                }


                if (gameMode == GameModeEnum.SMALL) {
                    final double finalSizeModifier = sizeModifier;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            Objects.requireNonNull(hider.getAttribute(Attribute.SCALE)).setBaseValue(finalSizeModifier);
                            if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
                                plugin.getLogger().info(hider.getName() + " size set to " + finalSizeModifier);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Failed to set size for " + hider.getName() + ": " + e.getMessage());
                        }
                    }, 1L);
                }


                hider.customName(Component.empty());
                hider.setCustomNameVisible(false);
            }
        }

        for (Player spectator : plugin.getTeamManager().getPlayersInTeam(plugin.getTeamManager().getSpectatorTeam())) {
            spectator.setGameMode(GameMode.SPECTATOR);
            spectator.setAllowFlight(true);
            spectator.setFlying(true);
            spectator.setWalkSpeed(0.2f);
            spectator.setHealth(0);
            spectator.setFoodLevel(0);
            spectator.setFireTicks(0);
            spectator.setGlowing(false);
            spectator.setInvulnerable(true);
            spectator.setSilent(true);
            spectator.setGlowing(false);
        }

        if (gameMode == GameModeEnum.BLOCK) {
            for (java.util.UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                org.bukkit.entity.Player hider = org.bukkit.Bukkit.getPlayer(hiderId);
                if (hider != null) {
                    HiderItems.giveItems(hider, hideAndSeekPlugin, true);
                }
            }
        }

        startHiderPointsTask(plugin);

        startCountdown(plugin);

        plugin.getTeamManager().getTeam(plugin.getTeamManager().getPlayerTeam(Bukkit.getPlayer(HideAndSeek.getDataController().getSeekers().getFirst()))).setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        plugin.getTeamManager().getTeam(plugin.getTeamManager().getPlayerTeam(Bukkit.getPlayer(HideAndSeek.getDataController().getSeekers().getFirst()))).setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        plugin.getTeamManager().getTeam(plugin.getTeamManager().getPlayerTeam(Bukkit.getPlayer(HideAndSeek.getDataController().getHiders().getFirst()))).setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        plugin.getTeamManager().getTeam(plugin.getTeamManager().getPlayerTeam(Bukkit.getPlayer(HideAndSeek.getDataController().getHiders().getFirst()))).setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);

        Bukkit.getScheduler().runTaskLater(hideAndSeekPlugin, () -> hideAndSeekPlugin.getAntiCheatVisibilityListener().refreshSoon(), 2L);
    }

    @Override
    public void onEnd(MinigameFramework plugin) {

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;

        if (pointsTask != null) {
            pointsTask.cancel();
        }

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum gameMode = (gameModeObj instanceof GameModeEnum) ?
                (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;


        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline()) {

                if (gameMode != GameModeEnum.BLOCK) {
                    try {
                        hider.removePotionEffect(PotionEffectType.INVISIBILITY);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to remove invisibility from " + hider.getName());
                    }
                }


            }
        }


        for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker != null && seeker.isOnline()) {

                seeker.removePotionEffect(PotionEffectType.BLINDNESS);

                Iterator<Map.Entry<BukkitTask, Location>> iterator = blockTasks.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<BukkitTask, Location> entry = iterator.next();

                    BukkitTask task = entry.getKey();
                    Location location = entry.getValue();

                    if (task.getOwner() == plugin) {
                        iterator.remove();
                        task.cancel();

                        seeker.sendBlockChange(location, location.getBlock().getBlockData());

                        Location above = location.clone().add(0, 1, 0);
                        seeker.sendBlockChange(above, above.getBlock().getBlockData());

                    }
                }

                seeker.setAllowFlight(false);
                seeker.setFlying(false);

                seeker.getInventory().setHelmet(null);

                seeker.setGameMode(GameMode.SURVIVAL);
                seeker.setWalkSpeed(0.2f);

                Title title = Title.title(
                        Component.text("GO!", NamedTextColor.RED),
                        Component.text("Find the hiders!", NamedTextColor.YELLOW),
                        Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(2), Duration.ofMillis(500))
                );
                seeker.showTitle(title);
            }
        }

        HiderItems.removeFromAllPlayers();

        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Hiding phase ended - seeking phase starting");
        }

        TimerManager.cleanupTimer(hideAndSeekPlugin, "Hiding");
    }

    @Override
    public List<String> getAllowedTransitions() {
        return List.of("seeking");
    }

    private void startCountdown(MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        TimerManager.startHidingTimer(hideAndSeekPlugin);
    }

    @Override
    public boolean allowBlockBreak() {
        return false;
    }

    @Override
    public boolean allowBlockPlace() {
        return false;
    }

    private void startHiderPointsTask(MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        int startDelaySeconds = Math.max(0, hideAndSeekPlugin.getPointService().getInt("points.hider.survival.start-delay-seconds", 20));
        int intervalSeconds = Math.max(1, hideAndSeekPlugin.getPointService().getInt("points.hider.survival.interval-seconds", 20));

        pointsTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                    Player hider = Bukkit.getPlayer(hiderId);
                    if (hider != null && hider.isOnline() && hider.getGameMode() != GameMode.SPECTATOR) {
                        hideAndSeekPlugin.getPointService().award(hiderId, PointAction.HIDER_SURVIVAL_TICK);
                    }
                }
            }
        }.runTaskTimer(plugin, startDelaySeconds * 20L, intervalSeconds * 20L);
    }

    @Override
    public boolean canBreakBlock(Block block, Player player, MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        Material blockType = block.getType();

        if (HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
            if (hideAndSeekPlugin.getCustomItemManager().hasItemInMainHand(player, SeekersSwordItem.ID)) {


                return allowedMaterials.contains(blockType);
            }
        }
        return GamePhase.super.canBreakBlock(block, player, plugin);
    }

    @Override
    public boolean allowBlockInteraction() {
        return false;
    }

    @Override
    public boolean allowEntityInteraction() {
        return false;
    }

    @Override
    public boolean allowBlockDetection() {
        return false;
    }

    @Override
    public boolean allowEntityDetection() {
        return false;
    }

    @Override
    public List<Material> getBlockInteractionExceptions() {
        return new ArrayList<>(blockInteractionExceptions);
    }

    @Override
    public boolean allowBlockPhysics() {
        return false;
    }

    @Override
    public boolean allowEntityChangeBlock() {
        return false;
    }

    @Override
    public boolean allowBlockExplosions() {
        return false;
    }

    @Override
    public boolean allowEntityExplosions() {
        return false;
    }

    @Override
    public List<Material> getBlockPhysicsExceptions() {
        return new ArrayList<>(blockPhysicsExceptions);
    }

    @Override
    public boolean allowBlockDrops() {
        return false;
    }

    @Override
    public boolean allowBlockExperienceDrop() {
        return false;
    }

    @Override
    public boolean allowEntityDrops() {
        return false;
    }

    @Override
    public boolean allowEntityExperienceDrop() {
        return false;
    }

    private void generateAllowedBreakBlocks(HideAndSeek plugin, String mapName) {
        allowedMaterials.clear();
        List<String> rawBlockList = MapConfigHelper.getSeekerBreakBlockPatterns(plugin, mapName);

        for (String entry : rawBlockList) {
            allowedMaterials.addAll(BlockListParser.parseBlockList(entry));
        }
    }

    private void generateBlockExceptionMaterials(HideAndSeek plugin, String mapName) {
        blockInteractionExceptions.clear();
        blockInteractionExceptions.addAll(MapConfigHelper.getBlockInteractionExceptions(plugin, mapName));

        blockPhysicsExceptions.clear();
        blockPhysicsExceptions.addAll(MapConfigHelper.getBlockPhysicsExceptions(plugin, mapName));
    }

    private void showRoundStartMapInfoTitle(MinigameFramework plugin, MapData mapData, String mapName) {
        var enabledResult = plugin.getSettingService().getSetting("game.maps.show-round-start-map-info-title");
        Object enabledObj = enabledResult.isSuccess() ? enabledResult.getValue() : true;
        boolean showTitle = (enabledObj instanceof Boolean) ? (Boolean) enabledObj : true;

        if (!showTitle) {
            return;
        }

        MapInfoDisplayMode displayMode = resolveMapInfoDisplayMode(plugin);

        String resolvedMapName = mapData != null && hasText(mapData.getDisplayName()) ? mapData.getDisplayName() : mapName;
        if (!hasText(resolvedMapName)) {
            resolvedMapName = "Unknown Map";
        }

        List<String> subtitleParts = new ArrayList<>();
        if (displayMode == MapInfoDisplayMode.NAME_AND_AUTHOR || displayMode == MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION) {
            subtitleParts.add("By " + resolveAuthor(mapData));
        }

        if (displayMode == MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION) {
            subtitleParts.add(trimForTitle(resolveDescription(mapData)));
        }

        String subtitle = String.join(" | ", subtitleParts);
        Title title = Title.title(
                Component.text(resolvedMapName, NamedTextColor.AQUA),
                Component.text(subtitle, NamedTextColor.GRAY),
                Title.Times.times(Duration.ofMillis(400), Duration.ofSeconds(4), Duration.ofMillis(500))
        );

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
        }
    }

    private void sendRoundStartAnnouncementChat(MinigameFramework plugin, MapData mapData, String mapName) {
        Set<UUID> seekerIds = new HashSet<>(HideAndSeek.getDataController().getSeekers());
        Set<UUID> hiderIds = new HashSet<>(HideAndSeek.getDataController().getHiders());
        MapInfoDisplayMode displayMode = resolveMapInfoDisplayMode(plugin);

        Set<UUID> spectatorIds = new HashSet<>();
        for (Player spectator : plugin.getTeamManager().getPlayersInTeam(plugin.getTeamManager().getSpectatorTeam())) {
            spectatorIds.add(spectator.getUniqueId());
        }

        String displayMapName = mapData != null && hasText(mapData.getDisplayName()) ? mapData.getDisplayName() : mapName;
        if (!hasText(displayMapName)) {
            displayMapName = "Unknown Map";
        }

        String author = resolveAuthor(mapData);
        String description = resolveDescription(mapData);

        for (Player player : Bukkit.getOnlinePlayers()) {

            Component roleText;
            if (seekerIds.contains(player.getUniqueId())) {
                roleText = Component.text("SEEKER", NamedTextColor.RED);
            } else if (hiderIds.contains(player.getUniqueId())) {
                roleText = Component.text("HIDER", NamedTextColor.GREEN);
            } else if (spectatorIds.contains(player.getUniqueId())) {
                roleText = Component.text("SPECTATOR", NamedTextColor.GRAY);
            } else {
                roleText = Component.text("UNASSIGNED", NamedTextColor.DARK_GRAY);
            }


            Component roleMessage = Component.text("You're a ", NamedTextColor.AQUA)
                    .append(roleText);
            player.sendMessage(roleMessage);


            Component mapMessage = Component.text("You're playing on ", NamedTextColor.AQUA)
                    .append(Component.text(displayMapName, NamedTextColor.YELLOW));

            if (displayMode == MapInfoDisplayMode.NAME_AND_AUTHOR || displayMode == MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION) {
                mapMessage = mapMessage.append(Component.text(" by ", NamedTextColor.AQUA))
                        .append(Component.text(author, NamedTextColor.GOLD));
            }

            Component mapBoxLine = Component.text("═══════════════════════════════", NamedTextColor.AQUA);
            player.sendMessage(mapBoxLine);
            player.sendMessage(mapMessage);

            if (displayMode == MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION) {
                player.sendMessage(Component.text(description, NamedTextColor.GRAY));
            }
            player.sendMessage(mapBoxLine);
        }
    }

    private MapInfoDisplayMode resolveMapInfoDisplayMode(MinigameFramework plugin) {
        var displayModeResult = plugin.getSettingService().getSetting("game.maps.round-start-map-info-display-mode");
        if (!displayModeResult.isSuccess()) {
            return MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION;
        }

        Object value = displayModeResult.getValue();
        if (value instanceof MapInfoDisplayMode mode) {
            return mode;
        }
        if (value instanceof String modeString) {
            try {
                return MapInfoDisplayMode.valueOf(modeString.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION;
            }
        }
        return MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION;
    }

    private String resolveAuthor(MapData mapData) {
        if (mapData != null && hasText(mapData.getAuthor())) {
            return mapData.getAuthor().trim();
        }
        return "Unknown author";
    }

    private String resolveDescription(MapData mapData) {
        if (mapData != null && hasText(mapData.getDescription())) {
            return mapData.getDescription().trim();
        }
        return "No description.";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String trimForTitle(String input) {
        String cleaned = input.trim();
        if (cleaned.length() <= 90) {
            return cleaned;
        }
        return cleaned.substring(0, 90 - 3) + "...";
    }

    @Override
    public boolean allowHunger() {
        return false;
    }

    @Override
    public boolean allowEntityPortals() {
        return false;
    }
}
