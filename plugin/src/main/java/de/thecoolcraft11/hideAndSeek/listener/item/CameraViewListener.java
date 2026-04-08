package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.CameraVisionMode;
import de.thecoolcraft11.hideAndSeek.items.seeker.CameraItem;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.activeCameraSessions;

public class CameraViewListener implements Listener {

    private final HideAndSeek plugin;
    private final Map<UUID, Long> lastCycleAtMs = new HashMap<>();

    public CameraViewListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSneakDetach(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        if (!activeCameraSessions.containsKey(player.getUniqueId())) {
            return;
        }

        CameraItem.stopCameraSession(player, plugin, true);
        player.sendMessage(Component.text("Detached from camera.", NamedTextColor.YELLOW));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCameraClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!activeCameraSessions.containsKey(player.getUniqueId())) {
            return;
        }

        Action action = event.getAction();
        var state = activeCameraSessions.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && state.activatedAtMs() > 0L
                && System.currentTimeMillis() - state.activatedAtMs() < 250L) {
            state.activatedAtMs(0L);
            event.setCancelled(true);
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            CameraItem.stopCameraSession(player, plugin, true);
            event.setCancelled(true);
            return;
        }


        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            cycleCameraWithDebounce(player);
            event.setCancelled(true);
            return;
        }

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            cycleVisionMode(player);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamageWhileInCamera(BlockDamageEvent event) {
        Player player = event.getPlayer();

        if (!activeCameraSessions.containsKey(player.getUniqueId())) {
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            CameraItem.stopCameraSession(player, plugin, true);
            event.setCancelled(true);
            return;
        }

        cycleCameraWithDebounce(player);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onArmSwingWhileInCamera(PlayerAnimationEvent event) {
        Player player = event.getPlayer();
        if (!activeCameraSessions.containsKey(player.getUniqueId())) {
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            CameraItem.stopCameraSession(player, plugin, true);
            return;
        }

        cycleCameraWithDebounce(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onRotateWithScroll(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        var state = activeCameraSessions.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            CameraItem.stopCameraSession(player, plugin, true);
            event.setCancelled(true);
            return;
        }

        int delta = event.getNewSlot() - event.getPreviousSlot();
        if (delta > 4) {
            delta -= 9;
        } else if (delta < -4) {
            delta += 9;
        }

        state.rotationYaw(state.rotationYaw() + (delta * 15f));
        CameraItem.attachToCurrentCamera(player, plugin);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHiderMove(PlayerMoveEvent event) {
        Player moved = event.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(moved.getUniqueId())) {
            return;
        }

        if (event.getFrom().getWorld() == null || event.getTo().getWorld() == null) {
            return;
        }

        boolean changedBlock = event.getFrom().getBlockX() != event.getTo().getBlockX()
                || event.getFrom().getBlockY() != event.getTo().getBlockY()
                || event.getFrom().getBlockZ() != event.getTo().getBlockZ()
                || !event.getFrom().getWorld().getUID().equals(event.getTo().getWorld().getUID());
        if (!changedBlock) {
            return;
        }

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum gameMode = (gameModeObj instanceof GameModeEnum) ?
                (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;

        for (Map.Entry<UUID, de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.CameraSessionState> entry : activeCameraSessions.entrySet()) {
            var session = entry.getValue();
            if (session.visionMode() != CameraVisionMode.TERMINAL_VISION && session.visionMode() != CameraVisionMode.NIGHT_VISION) {
                continue;
            }

            Player viewer = plugin.getServer().getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
                continue;
            }
            if (!viewer.getWorld().equals(moved.getWorld())) {
                continue;
            }

            if (gameMode == GameModeEnum.BLOCK) {
                BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(moved.getUniqueId());
                if (display != null && display.isValid()) {
                    display.setGlowing(true);
                }
            } else {
                plugin.getNmsAdapter().setEntityGlowingForViewer(viewer, moved, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreakCameraTorch(BlockBreakEvent event) {
        if (activeCameraSessions.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            cycleCameraWithDebounce(event.getPlayer());
            return;
        }

        Block block = event.getBlock();
        Material type = block.getType();

        if (type != Material.REDSTONE_TORCH && type != Material.REDSTONE_WALL_TORCH) {
            return;
        }

        Location location = block.getLocation();
        CameraItem.removeCameraAt(location);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();

        lastCycleAtMs.remove(playerId);
        CameraItem.stopCameraSession(player, plugin, true);
        CameraItem.clearPlacedCameras(playerId);
    }

    private void cycleCameraWithDebounce(Player player) {
        long now = System.currentTimeMillis();
        long last = lastCycleAtMs.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 120L) {
            return;
        }
        lastCycleAtMs.put(player.getUniqueId(), now);
        cycleCamera(player);
    }

    private void cycleCamera(Player player) {
        var state = activeCameraSessions.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        int size = CameraItem.getPlacedCameras(player.getUniqueId()).size();
        if (size == 0) {
            CameraItem.stopCameraSession(player, plugin, true);
            return;
        }

        state.currentIndex((state.currentIndex() + 1) % size);
        CameraItem.attachToCurrentCamera(player, plugin);
        player.sendActionBar(Component.text("Camera " + (state.currentIndex() + 1) + "/" + size, NamedTextColor.AQUA));
    }

    private void cycleVisionMode(Player player) {
        var state = activeCameraSessions.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        state.visionMode(state.visionMode().next());

        if (state.visionMode() != CameraVisionMode.TERMINAL_VISION && state.visionMode() != CameraVisionMode.NIGHT_VISION) {
            CameraItem.setViewerGlow(player, plugin, false);
        }

        CameraItem.attachToCurrentCamera(player, plugin);
        CameraItem.playVisionModeSwitchSound(player, state.skinVariant(), state.visionMode());

        String modeLabel = switch (state.visionMode()) {
            case NIGHT_VISION -> "Night Vision";
            case TERMINAL_VISION -> "Terminal Vision";
            default -> "Normal";
        };
        NamedTextColor color = state.visionMode() == CameraVisionMode.TERMINAL_VISION
                ? NamedTextColor.LIGHT_PURPLE
                : state.visionMode() == CameraVisionMode.NIGHT_VISION ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        player.sendActionBar(Component.text("Camera mode: " + modeLabel, color));
    }
}
