package de.thecoolcraft11.hideAndSeek.items.api;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

public record ItemStateManager(HideAndSeek plugin) {
    public static final Map<UUID, Long> seekerCurseActiveUntil = new HashMap<>();
    public static final Map<UUID, Long> hiderCursedUntil = new HashMap<>();
    public static final Map<UUID, ItemStack> inkHelmetBackup = new HashMap<>();
    public static final Map<Location, BlockDisplay> sensorDisplays = new HashMap<>();
    public static final Map<UUID, LinkedList<PlacedCamera>> seekerCameras = new HashMap<>();
    public static final Map<UUID, CameraSessionState> activeCameraSessions = new HashMap<>();
    public static final Map<UUID, BukkitTask> swordChargeTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> swordChargeXpTasks = new HashMap<>();
    public static final Map<UUID, XpProgressHelper.SavedXp> swordChargeXp = new HashMap<>();
    public static final Map<UUID, BukkitTask> medkitChannelTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> medkitChannelParticleTasks = new HashMap<>();
    public static final Map<UUID, XpProgressHelper.SavedXp> medkitChannelXp = new HashMap<>();


    public static final Map<UUID, BukkitTask> invisibilityCloakXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> lightningFreezeXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> lightningFreezeHiderXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> totemXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> curseSpellSeekerXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> hiderCursedXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> inkSplashXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> inkSplashSeekerXpTasks = new HashMap<>();
    public static final Map<UUID, BukkitTask> ghostEssenceXpTasks = new HashMap<>();

    public record PlacedCamera(UUID ownerId, Location torchLocation, BlockFace facing, float placementYaw,
                               ItemDisplay headDisplay) {
    }

    public static final class CameraSessionState {
        private int currentIndex;
        private boolean nightVision;
        private int fakeEntityId = Integer.MIN_VALUE;
        private float rotationYaw;

        public CameraSessionState(int currentIndex, float rotationYaw) {
            this.currentIndex = currentIndex;
            this.rotationYaw = rotationYaw;
        }

        public int currentIndex() {
            return currentIndex;
        }

        public void currentIndex(int currentIndex) {
            this.currentIndex = currentIndex;
        }

        public boolean nightVision() {
            return nightVision;
        }

        public void nightVision(boolean nightVision) {
            this.nightVision = nightVision;
        }

        public int fakeEntityId() {
            return fakeEntityId;
        }

        public void fakeEntityId(int fakeEntityId) {
            this.fakeEntityId = fakeEntityId;
        }

        public float rotationYaw() {
            return rotationYaw;
        }

        public void rotationYaw(float rotationYaw) {
            this.rotationYaw = rotationYaw;
        }
    }
}
