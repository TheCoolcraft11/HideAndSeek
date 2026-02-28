package de.thecoolcraft11.hideAndSeek.listener;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockDirectionUtil;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import io.papermc.paper.event.block.BlockBreakProgressUpdateEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.bukkit.util.VoxelShape;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;

public class BlockModeListener implements Listener {
    private final HideAndSeek plugin;
    private static final long SNEAK_DURATION_MS = 5000;
    private BukkitTask sneakTimerTask;

    public BlockModeListener(HideAndSeek plugin) {
        this.plugin = plugin;
        startSneakTimerTask();
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();


        if (event.getFrom().getX() == event.getTo().getX() &&
                event.getFrom().getY() == event.getTo().getY() &&
                event.getFrom().getZ() == event.getTo().getZ()) {
            return;
        }

        String phase = plugin.getStateManager().getCurrentPhaseId();
        if (!phase.equals("seeking") && !phase.equals("hiding")) {
            return;
        }


        if (isNotBlockModeHider(player)) {
            return;
        }


        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            return;
        }


        updateBlockDisplay(player);


        if (!player.isSneaking()) {

            HideAndSeek.getDataController().clearSneakStart(player.getUniqueId());
            player.setLevel(0);
            player.setExp(0.0f);
        }
    }


    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();

        if (isNotBlockModeHider(player)) {
            return;
        }


        if (HideAndSeek.getDataController().isHidden(player.getUniqueId()) && event.isSneaking()) {
            Bukkit.getScheduler().runTask(plugin, () -> unhidePlayer(player));
            return;
        }

        if (!event.isSneaking()) {

            HideAndSeek.getDataController().clearSneakStart(player.getUniqueId());
            player.setLevel(0);
            player.setExp(0.0f);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();


        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            if (event.getAction() == Action.LEFT_CLICK_BLOCK ||
                    event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBreakProgress(BlockBreakProgressUpdateEvent event) {
        if (event.getEntity() instanceof Player breaker) {
            damageHiddenPlayer(breaker, event.getBlock(), false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        damageHiddenPlayer(event.getPlayer(), event.getBlock(), false);
    }


    public void damageHiddenPlayer(Player breaker, Block block, boolean gazeKill) {
        if (!plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
            return;
        }


        if (!HideAndSeek.getDataController().getSeekers().contains(breaker.getUniqueId())) {
            return;
        }


        java.util.UUID hiderId = HideAndSeek.getDataController().getHiderByBlock(block.getLocation());

        if (hiderId != null) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline()) {

                double damage = gazeKill ? 1000 : getDamage(breaker);

                HideAndSeek.getDataController().setBlockDamageOverride(hider.getUniqueId(), System.currentTimeMillis() + 500);
                hider.damage(damage, breaker);

                Entity vehicle = hider.getVehicle();
                Entity sittingEntity = HideAndSeek.getDataController().getSittingEntity(hider.getUniqueId());


                if (vehicle != null && vehicle.equals(sittingEntity)) {

                    Bukkit.getScheduler().runTask(plugin, () -> unhidePlayer(hider));
                }

                plugin.getLogger().info(breaker.getName() + " hit hidden hider " + hider.getName() + " for " + damage + " damage");
            }
        }
    }

    private static double getDamage(Player breaker) {
        org.bukkit.inventory.ItemStack tool = breaker.getInventory().getItemInMainHand();
        double damage = 4.0;


        if (!tool.getType().isAir()) {
            damage = switch (tool.getType()) {
                case WOODEN_AXE, WOODEN_PICKAXE, WOODEN_SHOVEL, WOODEN_SWORD -> 4.0;
                case STONE_AXE, STONE_PICKAXE, STONE_SHOVEL, STONE_SWORD -> 5.0;
                case IRON_AXE, IRON_PICKAXE, IRON_SHOVEL, IRON_SWORD -> 6.0;
                case DIAMOND_AXE, DIAMOND_PICKAXE, DIAMOND_SHOVEL, DIAMOND_SWORD -> 8.0;
                case NETHERITE_AXE, NETHERITE_PICKAXE, NETHERITE_SHOVEL, NETHERITE_SWORD -> 10.0;
                default -> 2.0;
            };
        }
        return damage;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        cleanupBlockDisplay(player);
        cleanupSittingEntity(player);
        cleanupInteractionEntity(player);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }


        if (isNotBlockModeHider(player)) {
            return;
        }

        if (!HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            return;
        }

        Entity vehicle = event.getVehicle();
        Entity sittingEntity = HideAndSeek.getDataController().getSittingEntity(player.getUniqueId());


        if (vehicle.equals(sittingEntity)) {

            Bukkit.getScheduler().runTask(plugin, () -> unhidePlayer(player));
        }
    }

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player attacker = event.getPlayer();
        Entity entity = event.getRightClicked();

        
        if (!(entity instanceof Interaction interaction)) {
            return;
        }

        String hiderId = interaction.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "hidingBlock"),
                PersistentDataType.STRING
        );

        if (hiderId == null) {
            return;
        }

        try {
            java.util.UUID hiderUUID = java.util.UUID.fromString(hiderId);

            
            if (attacker.getUniqueId().equals(hiderUUID)) {
                event.setCancelled(false); 
                return;
            }

            
            
            event.setCancelled(true);
        } catch (IllegalArgumentException e) {
            
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Interaction interaction)) {
            return;
        }

        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        String hiderId = interaction.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "hidingBlock"),
                PersistentDataType.STRING
        );

        if (hiderId == null) {
            return;
        }

        try {
            java.util.UUID hiderUUID = java.util.UUID.fromString(hiderId);

            
            if (attacker.getUniqueId().equals(hiderUUID)) {
                event.setCancelled(true);
                return;
            }
        } catch (IllegalArgumentException e) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        handleInteractionHit(attacker, interaction);
    }

    private void handleInteractionHit(Player attacker, Interaction interaction) {
        String hiderId = interaction.getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "hidingBlock"),
                PersistentDataType.STRING
        );

        if (hiderId == null) {
            return;
        }

        try {
            java.util.UUID hiderUUID = java.util.UUID.fromString(hiderId);
            Player hider = Bukkit.getPlayer(hiderUUID);

            if (hider == null || !hider.isOnline()) {
                return;
            }

            
            if (!HideAndSeek.getDataController().getSeekers().contains(attacker.getUniqueId())) {
                return;
            }

            
            if (HideAndSeek.getDataController().isHidden(hiderUUID)) {
                
                Location hiddenLocation = HideAndSeek.getDataController().getLastLocation(hiderUUID);
                if (hiddenLocation != null) {
                    Block hiddenBlock = hiddenLocation.getBlock();
                    boolean gazeKill = plugin.getSettingRegistry().get("game.seeker-kill-mode").equals("GAZE_KILL");
                    damageHiddenPlayer(attacker, hiddenBlock, gazeKill);
                    plugin.getLogger().info(attacker.getName() + " hit hidden hider " + hider.getName() + " via interaction entity");
                }
            } else {
                
                double attackDamage = getDamage(attacker);
                hider.damage(attackDamage, attacker);
                plugin.getLogger().info(attacker.getName() + " hit walking hider " + hider.getName() + " via interaction for " + attackDamage + " damage");
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid hider UUID in interaction entity: " + hiderId);
        }
    }

    private boolean isNotBlockModeHider(Player player) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return true;
        }

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;

        return gameModeObj == null || !gameModeObj.toString().equals("BLOCK");
    }


    private void updateBlockDisplay(Player player) {
        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        if (chosenBlock == null) {
            return;
        }

        BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        Location playerLoc = player.getLocation();

        boolean needsNewDisplay = false;

        if (display == null || !display.isValid()) {
            needsNewDisplay = true;
        } else {
            String tempGlowId = display.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING);
            if (tempGlowId != null) {
                display.remove();
                needsNewDisplay = true;
            } else {
                try {
                    Material currentBlockType = display.getBlock().getMaterial();
                    if (currentBlockType != chosenBlock) {

                        display.remove();
                        needsNewDisplay = true;
                    }
                } catch (Exception e) {

                    display.remove();
                    needsNewDisplay = true;
                }
            }
        }

        if (needsNewDisplay) {

            org.bukkit.block.data.BlockData blockData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
            if (blockData == null) {
                blockData = chosenBlock.createBlockData();
            }
            display = createBlockDisplay(player, playerLoc, blockData);
            HideAndSeek.getDataController().setBlockDisplay(player.getUniqueId(), display);

            
            ensureInteractionPassenger(player, display);
        } else {

            display.teleport(playerLoc.setRotation(playerLoc.getYaw(), 0));
            display.setBrightness(new Display.Brightness(player.getLocation().getBlock().getLightFromBlocks(), player.getLocation().getBlock().getLightFromSky()));


            boolean shouldGlow = HideAndSeek.getDataController().isGlowing(player.getUniqueId());
            if (shouldGlow && !display.isGlowing()) {
                display.setGlowing(true);
            }
            if (shouldGlow) {
                org.bukkit.scoreboard.Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                org.bukkit.scoreboard.Team hiderTeam = scoreboard.getEntryTeam(player.getName());
                display.setGlowColorOverride(toGlowColor(hiderTeam));
            }

            
            ensureInteractionPassenger(player, display);
        }

        boolean scaleToBlock = plugin.getSettingRegistry().get("game.block_size_to_block", false);
        if (scaleToBlock && !HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            try {
                BlockData blockData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
                if (blockData == null) {
                    blockData = chosenBlock.createBlockData();
                }
                double scale = getHeightFactorComparedToPlayer(blockData, playerLoc);
                scale = Math.max(0.1, Math.min(2.0, scale));
                Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(scale);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to scale player to block size: " + e.getMessage());
            }
        }

        if (!player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
            ));
        }
    }

    private BlockDisplay createBlockDisplay(Player player, Location location, org.bukkit.block.data.BlockData blockData) {
        return player.getWorld().spawn(location, BlockDisplay.class, bd -> {
            bd.setBlock(blockData);

            bd.setTransformation(new Transformation(
                    new Vector3f(-0.5f, 0, -0.5f),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(1, 1, 1),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            bd.setBrightness(new Display.Brightness(15, 15));

            
            
            bd.setInvisible(true);

            if (HideAndSeek.getDataController().isGlowing(player.getUniqueId())) {
                bd.setGlowing(true);
                org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                org.bukkit.scoreboard.Team hiderTeam = scoreboard.getEntryTeam(player.getName());
                bd.setGlowColorOverride(toGlowColor(hiderTeam));
            }
        });
    }

    private void ensureInteractionPassenger(Player player, BlockDisplay display) {
        if (display == null || !display.isValid()) {
            return;
        }

        Entity existingInteraction = HideAndSeek.getDataController().getInteractionEntity(player.getUniqueId());

        
        if (existingInteraction != null && existingInteraction.isValid()) {
            if (existingInteraction.getVehicle() != display) {
                display.addPassenger(existingInteraction);
            }
            return;
        }

        
        Interaction interaction = display.getWorld().spawn(display.getLocation(), Interaction.class, entity -> {
            entity.setResponsive(true);
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(1.0f);
            
            entity.setInvisible(true);
            entity.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "hidingBlock"),
                    PersistentDataType.STRING,
                    player.getUniqueId().toString()
            );
            player.hideEntity(plugin, entity);
        });

        display.addPassenger(interaction);
        HideAndSeek.getDataController().setInteractionEntity(player.getUniqueId(), interaction);
    }

    private void handleSneaking(Player player) {
        UUID playerId = player.getUniqueId();
        Long sneakStart = HideAndSeek.getDataController().getSneakStart(playerId);
        long currentTime = System.currentTimeMillis();

        if (SeekerItems.isHiderCursed(playerId)) {
            HideAndSeek.getDataController().clearSneakStart(playerId);
            player.setLevel(0);
            player.setExp(0.0f);
            player.sendMessage(Component.text("You are cursed and cannot hide right now!", NamedTextColor.RED));
            return;
        }

        if (sneakStart == null) {

            HideAndSeek.getDataController().setSneakStart(playerId, currentTime);

            player.setLevel(5);
            player.setExp(1.0f);
            return;
        }

        long sneakDuration = currentTime - sneakStart;

        if (sneakDuration >= SNEAK_DURATION_MS) {

            player.setLevel(0);
            player.setExp(0.0f);
            placeBlockAndHide(player);
        } else {


            int secondsLeft = (int) ((SNEAK_DURATION_MS - sneakDuration) / 1000);
            if (secondsLeft < 0) secondsLeft = 0;


            float progress = (float) (SNEAK_DURATION_MS - sneakDuration) / SNEAK_DURATION_MS;

            player.setLevel(secondsLeft);
            player.setExp(progress);
        }
    }

    private void placeBlockAndHide(Player player) {
        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        if (chosenBlock == null) {
            player.sendMessage(Component.text("You haven't chosen a block yet! Use /mg chooseblock", NamedTextColor.RED));
            player.setLevel(0);
            player.setExp(0.0f);
            return;
        }

        BlockData chosenData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (chosenData == null) {
            chosenData = chosenBlock.createBlockData();
        }

        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap != null && !currentMap.isEmpty()) {
            java.util.List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
            if (!allowedBlocks.isEmpty()) {
                var selector = plugin.getBlockSelectorGUI();
                var resolvedConfig = selector.resolveConfigForMaterial(allowedBlocks, chosenData.getMaterial());
                if (resolvedConfig == null || !resolvedConfig.isBlockStateAllowed(chosenData)) {
                    player.sendMessage(Component.text("That block is not allowed on this map! Use /mg chooseblock to select an allowed block.", NamedTextColor.RED));
                    HideAndSeek.getDataController().clearSneakStart(player.getUniqueId());
                    player.setLevel(0);
                    player.setExp(0.0f);
                    return;
                }
            }
        }

        Location loc = player.getLocation();
        Block blockAtFeet = loc.getBlock();
        Block blockAbove = blockAtFeet.getRelative(BlockFace.UP);
        Block blockAboveAbove = blockAbove.getRelative(BlockFace.UP);

        boolean inLiquid = blockAtFeet.getType() == Material.WATER || blockAtFeet.getType() == Material.LAVA;
        int maxAirAbove = Math.max(0, plugin.getConfig().getInt("game.max-air-above-liquid", 2));
        Block targetBlock = blockAtFeet;
        if (inLiquid) {
            targetBlock = null;
            for (int i = 1; i <= maxAirAbove; i++) {
                Block candidate = blockAtFeet.getRelative(BlockFace.UP, i);
                if (!candidate.getType().isAir()) {
                    continue;
                }

                boolean allLiquidBetween = true;
                for (int j = 1; j < i; j++) {
                    Material betweenType = blockAtFeet.getRelative(BlockFace.UP, j).getType();
                    if (betweenType != Material.WATER && betweenType != Material.LAVA) {
                        allLiquidBetween = false;
                        break;
                    }
                }

                if (allLiquidBetween) {
                    targetBlock = candidate;
                    break;
                }
            }
        }

        if (targetBlock == null || !targetBlock.getType().isAir()) {
            String error = inLiquid
                    ? "Cannot place block here - air above water/lava (within " + maxAirAbove + " blocks) is required!"
                    : "Cannot place block here - not in air!";
            player.sendMessage(Component.text(error, NamedTextColor.RED));
            HideAndSeek.getDataController().clearSneakStart(player.getUniqueId());
            player.setLevel(0);
            player.setExp(0.0f);
            return;
        }


        BlockData blockData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        boolean applyPlayerDirection = plugin.getConfig().getBoolean("game.apply-player-direction", true);
        if (blockData != null) {

            blockData = blockData.clone();
        } else {

            blockData = chosenBlock.createBlockData();
        }
        if (applyPlayerDirection) {

            BlockDirectionUtil.applyPlayerDirection(blockData, player);
        }
        targetBlock.setBlockData(blockData);


        if (HideAndSeek.getDataController().isGlowing(player.getUniqueId())) {
            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (display != null && display.isValid()) {
                String tempGlowId = display.getPersistentDataContainer().get(new org.bukkit.NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING);
                if (tempGlowId != null && tempGlowId.equals(player.getUniqueId().toString())) {

                    display.remove();
                    HideAndSeek.getDataController().setBlockDisplay(player.getUniqueId(), null);
                    plugin.getLogger().fine("Removed temporary glow display as hider hides");
                }
            }
        }

        cleanupBlockDisplay(player);
        cleanupInteractionEntity(player);


        if (HideAndSeek.getDataController().isGlowing(player.getUniqueId())) {
            Location spawnLoc = targetBlock.getLocation().clone().add(0.5, 0, 0.5);
            BlockData displayData = blockData.clone();
            BlockDisplay tempDisplay = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, bd -> {
                bd.setBlock(displayData);
                bd.setGlowing(true);
                bd.setTransformation(new Transformation(
                        new Vector3f(-0.5f, 0.001f, -0.5f),
                        new AxisAngle4f(0, 0, 0, 0),
                        new Vector3f(1.001f, 1.001f, 1.001f),
                        new AxisAngle4f(0, 0, 0, 0)
                ));
                bd.setBrightness(new Display.Brightness(15, 15));
                org.bukkit.scoreboard.Scoreboard scoreboard = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                org.bukkit.scoreboard.Team hiderTeam = scoreboard.getEntryTeam(player.getName());
                bd.setGlowColorOverride(toGlowColor(hiderTeam));
                bd.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING, player.getUniqueId().toString());
            });

            HideAndSeek.getDataController().setBlockDisplay(player.getUniqueId(), tempDisplay);
        }


        double viewHeight = calculateViewHeight(blockData, targetBlock, plugin);
        Location sittingLoc = targetBlock.getLocation().clone().add(0.5, viewHeight, 0.5);
        ArmorStand stand = player.getWorld().spawn(sittingLoc, ArmorStand.class, entity -> {
            entity.setVisible(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setMarker(true);
            entity.setSmall(true);
            entity.setSilent(true);
            entity.setCanPickupItems(false);
            entity.setPersistent(false);
            entity.setCollidable(false);
            entity.setBasePlate(false);
            entity.setArms(false);
            entity.registerAttribute(Attribute.SCALE);
            Objects.requireNonNull(entity.getAttribute(Attribute.SCALE)).setBaseValue(0.0);
            entity.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "isHidingSit"), PersistentDataType.BOOLEAN, true);
        });


        stand.addPassenger(player);

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {

                if (!player.isInsideVehicle() || player.getVehicle() != stand) {
                    stand.addPassenger(player);
                }
            }
        };
        task.runTaskLater(plugin, 2L);


        player.setCollidable(false);

        HideAndSeek.getDataController().setHidden(player.getUniqueId(), true);
        HideAndSeek.getDataController().setSittingEntity(player.getUniqueId(), stand);
        HideAndSeek.getDataController().addPlacedBlockKey(targetBlock.getLocation(), player.getUniqueId());
        HideAndSeek.getDataController().setLastLocation(player.getUniqueId(), targetBlock.getLocation());
        HideAndSeek.getDataController().clearSneakStart(player.getUniqueId());
        HideAndSeek.getDataController().changeHiddenBlock(player.getUniqueId(), targetBlock);


        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));

        player.sendActionBar(Component.text("Dismount (Shift) to unhide", NamedTextColor.GRAY));

        plugin.getLogger().info(player.getName() + " hid as a " + chosenBlock.name() + " at " +
                targetBlock.getX() + "," + targetBlock.getY() + "," + targetBlock.getZ());
    }

    private void unhidePlayer(Player player) {
        if (!HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            return;
        }


        if (player.isInsideVehicle()) {
            player.leaveVehicle();
        }

        Location lastLoc = HideAndSeek.getDataController().getLastLocation(player.getUniqueId());
        if (lastLoc != null) {
            Block block = lastLoc.getBlock();
            Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());


            if (block.getType() == chosenBlock) {
                block.setType(Material.AIR);
                HideAndSeek.getDataController().removePlacedBlockKey(block.getLocation());
            }
        }


        cleanupSittingEntity(player);
        cleanupInteractionEntity(player);

        HideAndSeek.getDataController().setHidden(player.getUniqueId(), false);
        HideAndSeek.getDataController().removeHiddenBlock(player.getUniqueId());

        boolean scaleToBlock = plugin.getSettingRegistry().get("game.block_size_to_block", false);
        if (scaleToBlock) {
            try {
                Objects.requireNonNull(player.getAttribute(Attribute.SCALE)).setBaseValue(1.0);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reset player scale: " + e.getMessage());
            }
        }


        player.setCollidable(true);

        player.sendMessage(Component.text("You are no longer hidden!", NamedTextColor.YELLOW));


        updateBlockDisplay(player);


        if (HideAndSeek.getDataController().isGlowing(player.getUniqueId())) {
            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (display != null && display.isValid()) {

                plugin.getLogger().fine("Hider " + player.getName() + " unhid while glowing - existing display glows");
            }
        }
    }

    public void forceUnhide(Player player) {
        if (player == null) {
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> unhidePlayer(player));
    }

    private void cleanupBlockDisplay(Player player) {
        BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.remove();
        }
        HideAndSeek.getDataController().setBlockDisplay(player.getUniqueId(), null);
    }

    private void cleanupSittingEntity(Player player) {
        Entity sittingEntity = HideAndSeek.getDataController().getSittingEntity(player.getUniqueId());
        if (sittingEntity != null && sittingEntity.isValid()) {
            sittingEntity.remove();
        }
        HideAndSeek.getDataController().setSittingEntity(player.getUniqueId(), null);
    }

    private void cleanupInteractionEntity(Player player) {
        Entity interactionEntity = HideAndSeek.getDataController().getInteractionEntity(player.getUniqueId());
        if (interactionEntity != null && interactionEntity.isValid()) {
            interactionEntity.remove();
        }
        HideAndSeek.getDataController().setInteractionEntity(player.getUniqueId(), null);
    }

    private void startSneakTimerTask() {
        sneakTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                String phase = plugin.getStateManager().getCurrentPhaseId();
                if (!phase.equals("seeking") && !phase.equals("hiding")) {
                    return;
                }

                for (Player player : Bukkit.getOnlinePlayers()) {

                    if (isNotBlockModeHider(player)) {
                        continue;
                    }


                    if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
                        continue;
                    }


                    if (player.isSneaking()) {
                        handleSneaking(player);
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    public void cancelSneakTimerTask() {
        if (sneakTimerTask != null && !sneakTimerTask.isCancelled()) {
            sneakTimerTask.cancel();
        }
    }

    private static double getHeightFactorComparedToPlayer(BoundingBox blockBox) {
        double blockHeight = blockBox.getMaxY() - blockBox.getMinY();
        return blockHeight / 1.8f;
    }

    private static double getHeightFactorComparedToPlayer(BlockData data, Location loc) {
        return getHeightFactorComparedToPlayer(getBlockBoundingBox(data, loc));
    }

    private static BoundingBox getBlockBoundingBox(BlockData data, Location loc) {
        VoxelShape shape = data.getCollisionShape(loc);


        var boxes = shape.getBoundingBoxes();

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (BoundingBox bb : boxes) {
            minX = Math.min(minX, bb.getMinX());
            minY = Math.min(minY, bb.getMinY());
            minZ = Math.min(minZ, bb.getMinZ());
            maxX = Math.max(maxX, bb.getMaxX());
            maxY = Math.max(maxY, bb.getMaxY());
            maxZ = Math.max(maxZ, bb.getMaxZ());
        }

        if (boxes.isEmpty()) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }

        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static double calculateViewHeight(BlockData blockData, Block targetBlock, HideAndSeek plugin) {
        BoundingBox blockBox = getBlockBoundingBox(blockData, targetBlock.getLocation());
        double blockMaxY = blockBox.getMaxY();
        double configuredHeight = plugin.getSettingRegistry().get("game.block_view_height", 0.1f);


        return Math.max(configuredHeight, blockMaxY + 0.05);
    }

    private static Color toGlowColor(org.bukkit.scoreboard.Team team) {
        if (team == null || team.getColor() == null) {
            return null;
        }
        return switch (team.getColor()) {
            case BLACK -> Color.BLACK;
            case DARK_BLUE -> Color.NAVY;
            case DARK_GREEN -> Color.GREEN;
            case DARK_AQUA -> Color.TEAL;
            case DARK_RED -> Color.MAROON;
            case DARK_PURPLE -> Color.PURPLE;
            case GOLD -> Color.ORANGE;
            case GRAY -> Color.GRAY;
            case DARK_GRAY -> Color.SILVER;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.LIME;
            case AQUA -> Color.AQUA;
            case RED -> Color.RED;
            case LIGHT_PURPLE -> Color.FUCHSIA;
            case YELLOW -> Color.YELLOW;
            case WHITE -> Color.WHITE;
            default -> null;
        };
    }

}
