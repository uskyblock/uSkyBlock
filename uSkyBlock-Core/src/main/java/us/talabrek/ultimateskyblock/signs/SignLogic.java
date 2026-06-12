package us.talabrek.ultimateskyblock.signs;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dk.lockfuglsang.minecraft.file.FileUtil;
import dk.lockfuglsang.minecraft.util.ItemStackUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.bootstrap.PluginLog;
import us.talabrek.ultimateskyblock.challenge.ChallengeText;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.InventoryItemsRequirement;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements.ItemRequirementSpec;
import us.talabrek.ultimateskyblock.challenge.ChallengeCompletion;
import us.talabrek.ultimateskyblock.challenge.ChallengeKey;
import us.talabrek.ultimateskyblock.challenge.ChallengeLogic;
import us.talabrek.ultimateskyblock.handler.WorldGuardHandler;
import us.talabrek.ultimateskyblock.island.IslandInfo;
import us.talabrek.ultimateskyblock.player.PlayerInfo;
import us.talabrek.ultimateskyblock.uSkyBlock;
import us.talabrek.ultimateskyblock.util.LocationUtil;
import us.talabrek.ultimateskyblock.util.Scheduler;
import us.talabrek.ultimateskyblock.world.WorldManager;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static us.talabrek.ultimateskyblock.message.Msg.PRIMARY;
import static us.talabrek.ultimateskyblock.message.Msg.sendErrorTr;
import static us.talabrek.ultimateskyblock.message.Msg.sendTr;
import static us.talabrek.ultimateskyblock.message.Placeholder.component;
import static us.talabrek.ultimateskyblock.message.Placeholder.unparsed;
import static dk.lockfuglsang.minecraft.po.I18nUtil.trLegacy;
import static dk.lockfuglsang.minecraft.util.FormatUtil.wordWrap;
import static us.talabrek.ultimateskyblock.message.Msg.ERROR;

/**
 * Responsible for keeping track of signs.
 */
@Singleton
public class SignLogic {
    private static final int SIGN_LINE_WIDTH = 11; // Actually more like 15, but we break after.

    private final FileConfiguration config;
    private final File configFile;
    private final Logger logger;
    private final uSkyBlock plugin;
    private final Scheduler scheduler;
    private final ChallengeLogic challengeLogic;
    private final WorldManager worldManager;

    @Inject
    public SignLogic(
        @NotNull @PluginLog Logger logger,
        @NotNull uSkyBlock plugin,
        @NotNull Scheduler scheduler,
        @NotNull ChallengeLogic challengeLogic,
        @NotNull WorldManager worldManager
    ) {
        this.logger = logger;
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.challengeLogic = challengeLogic;
        this.worldManager = worldManager;
        this.configFile = new File(plugin.getDataFolder(), "signs.yml");
        this.config = FileUtil.getYmlConfiguration("signs.yml");
    }

    void addSign(Sign block, String[] lines, Chest chest, Player player) {
        var result = challengeLogic.resolveChallenge(lines[1].trim());
        if (result.getStatus() != ChallengeLogic.ChallengeLookupResult.Status.FOUND) {
            sendErrorTr(player, "No challenge named <challenge> was found!", unparsed("challenge", lines[1].trim()));
            return;
        }
        ChallengeDefinition challenge = result.getChallenge();
        if (requirementSpecs(challenge).isEmpty()) {
            sendErrorTr(player, "The <challenge> challenge has no item hand-in, so it cannot be completed from a sign.",
                component("challenge", ChallengeText.displayName(challenge)));
            return;
        }
        Location loc = block.getLocation();
        ConfigurationSection signs = config.getConfigurationSection("signs");
        if (signs == null) {
            signs = config.createSection("signs");
        }
        String signLocation = LocationUtil.asKey(loc);
        ConfigurationSection signSection = signs.createSection(signLocation);
        signSection.set("location", LocationUtil.asString(loc));
        // Resolved once: the canonical id survives display-name edits in challenges.yml.
        signSection.set("challenge", challenge.id().value());
        String chestLocation = LocationUtil.asString(chest.getLocation());
        signSection.set("chest", chestLocation);
        ConfigurationSection chests = config.getConfigurationSection("chests");
        if (chests == null) {
            chests = config.createSection("chests");
        }
        String chestPath = LocationUtil.asKey(chest.getLocation());
        List<String> signList = chests.getStringList(chestPath);
        if (!signList.contains(signLocation)) {
            signList.add(signLocation);
        }
        chests.set(chestPath, signList);
        saveAsync();
        sendTr(player, "Challenge sign created for <challenge>.", component("challenge", ChallengeText.displayName(challenge), PRIMARY));
        updateSignsOnContainer(chest.getLocation());
    }

    void removeSign(final Location loc) {
        scheduler.async(() -> removeSignAsync(loc));
    }

    private void removeSignAsync(Location loc) {
        String signKey = LocationUtil.asKey(loc);
        String chestLoc = config.getString("signs." + signKey + ".chest", null);
        if (chestLoc != null) {
            String chestKey = LocationUtil.asKey(LocationUtil.fromString(chestLoc));
            List<String> signList = config.getStringList("chests." + chestKey);
            signList.remove(signKey);
            if (signList.isEmpty()) {
                config.set("chests." + chestKey, null);
            }
        }
        config.set("signs." + signKey, null);
        save();
    }

    void removeChest(final Location loc) {
        scheduler.async(() -> removeChestAsync(loc));
    }

    private void removeChestAsync(Location loc) {
        String chestKey = LocationUtil.asKey(loc);
        List<String> signList = config.getStringList("chests." + chestKey);
        for (String signKey : signList) {
            config.set("signs." + signKey, null);
        }
        config.set("chests." + chestKey, null);
        save();
    }

    void updateSignsOnContainer(final Location... containerLocations) {
        scheduler.async(() -> {
            for (Location loc : containerLocations) {
                if (loc == null) {
                    continue;
                }
                long x1 = (long) Math.floor(loc.getX());
                long x2 = Math.round(loc.getX());
                long z1 = (long) Math.floor(loc.getZ());
                long z2 = Math.round(loc.getZ());
                if (x1 != x2) {
                    // Double Chest!
                    Location loc1 = loc.clone();
                    loc1.setX(x1);
                    Location loc2 = loc.clone();
                    loc2.setX(x2);
                    updateSignAsync(loc1);
                    updateSignAsync(loc2);
                } else if (z1 != z2) {
                    // Double Chest!
                    Location loc1 = loc.clone();
                    loc1.setZ(z1);
                    Location loc2 = loc.clone();
                    loc2.setZ(z2);
                    updateSignAsync(loc1);
                    updateSignAsync(loc2);
                } else {
                    updateSignAsync(loc);
                }
            }
        });
    }

    void updateSign(Location signLocation) {
        String signString = LocationUtil.asKey(signLocation);
        String chestLocStr = config.getString("signs." + signString + ".chest", null);
        Location chestLoc = LocationUtil.fromString(chestLocStr);
        if (chestLoc != null) {
            updateSignAsync(chestLoc);
        }
    }

    private void updateSignAsync(final Location chestLoc) {
        if (chestLoc == null || !worldManager.isSkyAssociatedWorld(chestLoc.getWorld())) {
            return;
        }
        String locString = LocationUtil.asKey(chestLoc);
        List<String> signList = config.getStringList("chests." + locString);
        if (signList.isEmpty()) {
            return;
        }
        String islandName = WorldGuardHandler.getIslandNameAt(chestLoc);
        if (islandName == null) {
            return;
        }
        for (String signLoc : signList) {
            updateSignAsync(chestLoc, islandName, signLoc);
        }
    }

    // TODO: This method accesses a lot of unsynchronized data, and should be refactored to be sync.
    private void updateSignAsync(final Location chestLoc, String islandName, String signLoc) {
        String challengeName = config.getString("signs." + signLoc + ".challenge", null);
        if (challengeName == null) {
            return;
        }
        var result = challengeLogic.resolveChallenge(challengeName);
        if (result.getStatus() != ChallengeLogic.ChallengeLookupResult.Status.FOUND) {
            return;  // TODO: proper player feedback
        }
        ChallengeDefinition challenge = result.getChallenge();
        ChallengeKey challengeId = result.getChallengeKey();
        if (requirementSpecs(challenge).isEmpty()) {
            // Signs only support item hand-in challenges.
            return;
        }
        Map<ItemStack, Integer> requiredItems = new LinkedHashMap<>();
        ChallengeCompletion completion = challengeLogic.getIslandCompletion(islandName, challengeId);
        if (completion != null) {
            for (ItemRequirementSpec spec : requirementSpecs(challenge)) {
                requiredItems.put(spec.item().create(), spec.amountForRepetitions(completion.getTimesCompletedInCooldown()));
            }
        }
        boolean isChallengeAvailable = false;
        IslandInfo islandInfo = plugin.getIslandInfo(islandName);
        if (islandInfo != null && islandInfo.getLeaderUniqueId() != null) {
            PlayerInfo playerInfo = plugin.getPlayerInfo(islandInfo.getLeaderUniqueId());
            if (playerInfo != null) {
                isChallengeAvailable = challengeLogic.getUnlockEvaluator()
                    .isChallengeUnlocked(challenge, challengeLogic.unlockContextFor(playerInfo));
            }
        }
        String signLocString = config.getString("signs." + signLoc + ".location", null);
        final Location signLocation = LocationUtil.fromString(signLocString);
        final boolean challengeLocked = !isChallengeAvailable;
        final Map<ItemStack, Integer> requiredItemsFinal = requiredItems;
        // Back to sync
        scheduler.sync(() -> updateSignFromChestSync(chestLoc, signLocation, challenge, requiredItemsFinal, challengeLocked));
    }

    private static List<ItemRequirementSpec> requirementSpecs(ChallengeDefinition challenge) {
        return challenge.completionRequirements().stream()
            .filter(InventoryItemsRequirement.class::isInstance)
            .map(InventoryItemsRequirement.class::cast)
            .flatMap(requirement -> requirement.items().stream())
            .toList();
    }

    private void updateSignFromChestSync(Location chestLoc, Location signLoc, ChallengeDefinition challenge, Map<ItemStack, Integer> requiredItems, boolean challengeLocked) {
        Block chestBlock = chestLoc.getBlock();
        Block signBlock = signLoc != null ? signLoc.getBlock() : null;
        if (signBlock != null && isChest(chestBlock) && signBlock.getState().getBlockData() instanceof WallSign) {
            Sign sign = (Sign) signBlock.getState();
            Chest chest = (Chest) chestBlock.getState();
            int missing = -1;
            if (!requiredItems.isEmpty() && !challengeLocked) {
                missing = 0;
                for (Map.Entry<ItemStack, Integer> required : requiredItems.entrySet()) {
                    ItemStack requiredType = required.getKey();
                    int requiredAmount = required.getValue();
                    if (!chest.getInventory().containsAtLeast(requiredType, requiredAmount)) {
                        // Max shouldn't be needed, provided containsAtLeast matches getCountOf... but it might not
                        missing += Math.max(0, requiredAmount - challengeLogic.getCountOf(chest.getInventory(), requiredType));
                    }
                }
            }
            List<String> lines = wordWrap(ChallengeText.plainName(challenge), SIGN_LINE_WIDTH, SIGN_LINE_WIDTH);
            if (challengeLocked) {
                lines.add(null); // placeholder, rendered as the locked label below
            } else {
                lines.addAll(wordWrap(ChallengeText.plain(challenge.display().description()), SIGN_LINE_WIDTH, SIGN_LINE_WIDTH));
            }
            SignSide front = sign.getSide(Side.FRONT);
            for (int i = 0; i < 3; i++) {
                if (i < lines.size()) {
                    front.setLine(i, lines.get(i) != null ? lines.get(i) : trLegacy("Locked Challenge", ERROR));
                } else {
                    front.setLine(i, "");
                }
            }
            if (missing > 0) {
                front.setLine(3, "\u00a74\u00a7l" + missing);
            } else if (missing == 0) {
                // I18N: Status label on challenge signs when all requirements are met.
                front.setLine(3, "\u00a72\u00a7l" + trLegacy("Ready"));
            } else if (lines.size() > 3 && lines.get(3) != null) {
                front.setLine(3, lines.get(3));
            } else {
                front.setLine(3, "");
            }
            // Waxed signs cannot be edited by hand, so a stray right-click does not wipe the sign.
            if (!sign.isWaxed()) {
                sign.setWaxed(true);
            }
            if (!sign.update()) {
                logger.info("Unable to update sign at " + LocationUtil.asString(signLoc));
            }
        }
    }

    private boolean isChest(Block chestBlock) {
        return (chestBlock.getType() == Material.CHEST || chestBlock.getType() == Material.TRAPPED_CHEST) && chestBlock.getState() instanceof Chest;
    }

    private void saveAsync() {
        scheduler.async(this::save);
    }

    private void save() {
        synchronized (configFile) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                logger.info("Unable to save to " + configFile);
            }
        }
    }

    void signClicked(final Player player, final Location location) {
        scheduler.async(() -> tryCompleteAsync(player, location));
    }

    private void tryCompleteAsync(final Player player, Location location) {
        String signLoc = LocationUtil.asKey(location);
        String challengeName = config.getString("signs." + signLoc + ".challenge", null);
        if (challengeName != null) {
            String islandName = WorldGuardHandler.getIslandNameAt(location);
            String chestLocString = config.getString("signs." + signLoc + ".chest", null);
            final Location chestLoc = LocationUtil.fromString(chestLocString);
            if (islandName != null && chestLoc != null) {
                var lookupResult = challengeLogic.resolveChallenge(challengeName);
                if (lookupResult.getStatus() != ChallengeLogic.ChallengeLookupResult.Status.FOUND
                    || requirementSpecs(lookupResult.getChallenge()).isEmpty()) {
                    return; // TODO: proper player feedback
                }
                // Availability and unlock checks happen in the executor on the main thread.
                scheduler.sync(() -> tryComplete(player, chestLoc, lookupResult.getChallengeKey()));
            }
        }
    }

    private void tryComplete(Player player, Location chestLoc, ChallengeKey challengeId) {
        BlockState state = chestLoc.getBlock().getState();
        if (!(state instanceof Chest chest)) {
            return;
        }
        PlayerInfo playerInfo = plugin.getPlayerInfo(player);
        if (playerInfo == null || !playerInfo.getHasIsland()) {
            return;
        }
        challengeLogic.completeChallenge(player, challengeId, List.of(player.getInventory(), chest.getInventory()));
        updateSignsOnContainer(chest.getLocation());
    }
}
