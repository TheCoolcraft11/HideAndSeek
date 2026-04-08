package de.thecoolcraft11.hideAndSeek.items.api;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public static final Map<Location, UUID> cageTrapLocations = new ConcurrentHashMap<>();
    public static final Set<UUID> cageTrapIndicatorEntities = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> proximitySensorEntities = ConcurrentHashMap.newKeySet();
    public static final Set<UUID> cameraEntities = ConcurrentHashMap.newKeySet();


    public static final Map<UUID, List<UUID>> activeAssistants = new ConcurrentHashMap<>();
    public static final Map<UUID, Location> assistantOrigins = new ConcurrentHashMap<>();
    public static final Map<UUID, Long> assistantSpawnTimes = new ConcurrentHashMap<>();
    public static final Map<UUID, Integer> assistantHitCounts = new ConcurrentHashMap<>();

    public static void removeAssistant(UUID assistantId) {
        if (assistantId == null) {
            return;
        }

        activeAssistants.values().forEach(list -> list.remove(assistantId));
        activeAssistants.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        assistantHitCounts.remove(assistantId);
        assistantOrigins.remove(assistantId);
        assistantSpawnTimes.remove(assistantId);
    }

    public static void pruneInvalidAssistants(UUID seekerId) {
        List<UUID> assistants = activeAssistants.get(seekerId);
        if (assistants == null || assistants.isEmpty()) {
            return;
        }

        for (UUID assistantId : List.copyOf(assistants)) {
            var entity = Bukkit.getEntity(assistantId);
            if (!(entity instanceof org.bukkit.entity.LivingEntity living) || !entity.isValid() || living.isDead()) {
                removeAssistant(assistantId);
            }
        }
    }

    public record PlacedCamera(UUID ownerId, Location torchLocation, BlockFace facing, float placementYaw,
                               ItemDisplay headDisplay) {
    }

    public enum CameraVisionMode {
        NORMAL,
        NIGHT_VISION,
        TERMINAL_VISION;

        public CameraVisionMode next() {
            CameraVisionMode[] values = values();
            return values[(this.ordinal() + 1) % values.length];
        }
    }

    public static final class CameraSessionState {
        private int currentIndex;
        private CameraVisionMode visionMode = CameraVisionMode.NORMAL;
        private int fakeEntityId = Integer.MIN_VALUE;
        private float rotationYaw;
        private long activatedAtMs = System.currentTimeMillis();
        private String skinVariant;

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

        public CameraVisionMode visionMode() {
            return visionMode;
        }

        public void visionMode(CameraVisionMode visionMode) {
            this.visionMode = visionMode == null ? CameraVisionMode.NORMAL : visionMode;
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

        public long activatedAtMs() {
            return activatedAtMs;
        }

        public String skinVariant() {
            return skinVariant;
        }

        public void skinVariant(String skinVariant) {
            this.skinVariant = skinVariant;
        }

        public void activatedAtMs(long activatedAtMs) {
            this.activatedAtMs = activatedAtMs;
        }
    }
}
