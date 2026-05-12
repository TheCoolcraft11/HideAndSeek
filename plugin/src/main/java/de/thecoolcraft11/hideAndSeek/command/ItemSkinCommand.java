package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import de.thecoolcraft11.minigameframework.translation.TranslationArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSkinCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;
    private static final String PERMISSION = "hideandseek.command.skin";
    private final Map<String, String> itemAliases = new HashMap<>();

    public ItemSkinCommand(HideAndSeek plugin) {
        this.plugin = plugin;
        registerItems();
    }

    @Override
    public @NotNull String getName() {
        return "skin";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("skins", "variant", "itemskin");
    }

    @Override
    public @Nullable String getPermission() {
        return PERMISSION;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
            return;
        }

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(plugin.tr(sender, "common.command.no_permission"));
            return;
        }

        if (!"lobby".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(plugin.tr(player, "command.skin.only_lobby"));
            return;
        }

        if (args.length == 0) {
            plugin.getSkinGUI().open(player);
            return;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            handleList(player, args);
            return;
        }

        if (args.length < 2) {
            sendUsage(player);
            return;
        }

        String logicalItemId = resolveLogicalItemId(args[0]);
        if (logicalItemId == null) {
            player.sendMessage(plugin.tr(player, "command.skin.unknown_item",
                    Map.of("item", args[0])));
            player.sendMessage(plugin.tr(player, "command.skin.list_hint",
                    Map.of("item", args[0])));
            return;
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        var variantManager = plugin.getCustomItemManager().getVariantManager();

        if (!variantManager.hasVariants(runtimeItemId)) {
            player.sendMessage(plugin.tr(player, "command.skin.no_variants"));
            return;
        }

        String variantInput = args[1];
        ItemVariant targetVariant = variantManager.getVariant(runtimeItemId, variantInput);

        if (targetVariant == null) {
            targetVariant = variantManager.getVariants(runtimeItemId).stream()
                    .filter(v -> v.getId().equalsIgnoreCase(variantInput))
                    .findFirst()
                    .orElse(null);
        }

        if (targetVariant == null) {
            player.sendMessage(plugin.tr(player, "command.skin.unknown_variant",
                    Map.of("variant", variantInput)));
            player.sendMessage(plugin.tr(player, "command.skin.list_hint",
                    Map.of("item", args[0])));
            return;
        }

        if (!ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, targetVariant.getId())) {
            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, targetVariant.getId());

            player.sendMessage(plugin.tr(player, "command.skin.locked",
                    Map.of("cost", String.valueOf(cost))));
            return;
        }

        ItemSkinSelectionService.setSelectedVariant(player.getUniqueId(), logicalItemId, targetVariant.getId());
        ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());

        String displayName = targetVariant.getDisplayName();
        String shownName = (displayName == null || displayName.isBlank())
                ? targetVariant.getId()
                : displayName;

        player.sendMessage(plugin.tr(player, "command.skin.selected", Map.of(
                "skin", shownName,
                "item", logicalItemId
        )));

        player.sendMessage(plugin.tr(player, "command.skin.apply_hint"));
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            Set<String> firstArgs = new TreeSet<>();
            firstArgs.add("list");
            firstArgs.addAll(getPrimaryItemKeys());
            return firstArgs.stream().filter(v -> v.startsWith(prefix)).toList();
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return getPrimaryItemKeys().stream().filter(v -> v.startsWith(prefix)).toList();
        }

        if (args.length == 2 && sender instanceof Player player) {
            String logicalItemId = resolveLogicalItemId(args[0]);
            if (logicalItemId == null) {
                return List.of();
            }
            String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).stream()
                    .map(ItemVariant::getId)
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private void handleList(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(plugin.tr(player, "command.skin.usage_list"));
            return;
        }

        String logicalItemId = resolveLogicalItemId(args[1]);
        if (logicalItemId == null) {
            player.sendMessage(plugin.tr(player, "command.skin.unknown_item",
                    Map.of("item", args[1])));
            return;
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        List<ItemVariant> variants = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId);

        if (variants.isEmpty()) {
            player.sendMessage(plugin.tr(player, "command.skin.no_variants"));
            return;
        }

        player.sendMessage(plugin.tr(player, "command.skin.list_header",
                Map.of("item", logicalItemId)));

        int coins = ItemSkinSelectionService.getCoins(player.getUniqueId());
        player.sendMessage(plugin.tr(player, "command.skin.coins",
                TranslationArguments.ofNamed(Map.of()).withCount(coins)));

        for (ItemVariant variant : variants) {
            String display = variant.getDisplayName().isEmpty()
                    ? variant.getId()
                    : variant.getDisplayName();

            boolean unlocked = ItemSkinSelectionService.isUnlocked(
                    player.getUniqueId(), logicalItemId, variant.getId());

            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variant.getId());
            String rarity = ItemSkinSelectionService.getRarity(logicalItemId, variant.getId()).name();

            String stateKey = unlocked
                    ? "command.skin.state.unlocked"
                    : "command.skin.state.locked";

            String rarityKey = "command.skin.rarity." + rarity;

            player.sendMessage(plugin.tr(player, "command.skin.list_entry", Map.of(
                    "id", variant.getId(),
                    "display", display,
                    "rarity", plugin.trText(player, rarityKey),
                    "cost", String.valueOf(cost),
                    "state", plugin.trText(player, stateKey)
            )));
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(plugin.tr(player, "command.skin.usage_main"));
        player.sendMessage(plugin.tr(player, "command.skin.usage_list"));
    }

    private void registerItems() {
        addAliases(ExplosionItem.ID, "explosionitem", "explosion");
        addAliases(RandomBlockItem.ID, "randomblockitem", "randomblock");
        addAliases(SpeedBoostItem.ID, "speedboostitem", "speedboost");
        addAliases(TrackerCrossbowItem.ID, "trackercrossbowitem", "trackercrossbow", "crossbowtracker");
        addAliases(KnockbackStickItem.ID, "knockbackstickitem", "knockbackstick");
        addAliases(BlockSwapItem.ID, "blockswapitem", "blockswap");
        addAliases(BigFirecrackerItem.ID, "bigfirecrackeritem", "bigfirecracker");
        addAliases(FireworkRocketItem.ID, "fireworkrocketitem", "fireworkrocket");
        addAliases(SlownessBallItem.ID, "slownessballitem", "slownessball");
        addAliases(SmokeBombItem.ID, "smokebombitem", "smokebomb");
        addAliases(GhostEssenceItem.ID, "ghostessenceitem", "ghostessence");
        addAliases(InvisibilityCloakItem.ID, "invisibilitycloakitem", "invisibilitycloak");
        addAliases(MedkitItem.ID, "medkititem", "medkit");
        addAliases(TotemItem.ID, "totemitem", "totem");
        addAliases(SoundItem.ID, "sounditem", "sound");

        addAliases(GrapplingHookItem.ID, "grapplinghookitem", "grapplinghook");
        addAliases(GlowingCompassItem.ID, "glowingcompassitem", "glowingcompass");
        addAliases(BlockRandomizerItem.ID, "blockrandomizeritem", "blockrandomizer");
        addAliases(ChainPullItem.ID, "chainpullitem", "chainpull");
        addAliases(CageTrapItem.ID, "cagetrapitem", "cagetrap");
        addAliases(ProximitySensorItem.ID, "proximitysensoritem", "proximitysensor");
        addAliases(CameraItem.ID, "cameraitem", "camera");
        addAliases(CurseSpellItem.ID, "cursespellitem", "cursespell");
        addAliases(InkSplashItem.ID, "inksplashitem", "inksplash");
        addAliases(LightningFreezeItem.ID, "lightningfreezeitem", "lightningfreeze");
        addAliases(SeekersSwordItem.ID, "seekerssworditem", "seekerssword");
    }

    private void addAliases(String logicalItemId, String... aliases) {
        itemAliases.put(logicalItemId.toLowerCase(Locale.ROOT), logicalItemId);
        itemAliases.put(ItemSkinSelectionService.normalizeLogicalItemId(logicalItemId).toLowerCase(Locale.ROOT),
                ItemSkinSelectionService.normalizeLogicalItemId(logicalItemId));

        for (String alias : aliases) {
            itemAliases.put(alias.toLowerCase(Locale.ROOT), logicalItemId);
        }
    }

    private String resolveLogicalItemId(String input) {
        String direct = itemAliases.get(input.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return ItemSkinSelectionService.normalizeLogicalItemId(direct);
        }

        String normalized = ItemSkinSelectionService.normalizeLogicalItemId(input);
        String mapped = itemAliases.get(normalized.toLowerCase(Locale.ROOT));
        if (mapped != null) {
            return ItemSkinSelectionService.normalizeLogicalItemId(mapped);
        }

        return null;
    }

    private List<String> getPrimaryItemKeys() {
        return itemAliases.entrySet().stream()
                .filter(e -> e.getKey().equals(e.getValue().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
