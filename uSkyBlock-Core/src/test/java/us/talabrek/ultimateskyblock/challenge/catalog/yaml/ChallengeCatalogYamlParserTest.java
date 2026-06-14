package us.talabrek.ultimateskyblock.challenge.catalog.yaml;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalogDiagnostic;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRequirements;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeRewards;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;

import java.io.StringReader;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ChallengeCatalogYamlParserTest {
    private final ChallengeCatalogYamlParser parser = new ChallengeCatalogYamlParser(new GameObjectFactory());

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        Server server = BukkitServerMock.setupServerMock();
        when(server.createBlockData(anyString())).thenAnswer(invocation -> {
            String specification = invocation.getArgument(0, String.class);
            Material material = Material.matchMaterial(specification);
            if (material == null && specification.startsWith("minecraft:")) {
                material = Material.matchMaterial(specification.substring("minecraft:".length()));
            }
            if (material == null) {
                throw new IllegalArgumentException("Unknown block: " + specification);
            }
            BlockData blockData = mock(BlockData.class);
            when(blockData.getMaterial()).thenReturn(material);
            when(blockData.clone()).thenReturn(blockData);
            return blockData;
        });
        when(server.getTag(eq(org.bukkit.Tag.REGISTRY_ITEMS), any(org.bukkit.NamespacedKey.class), eq(Material.class)))
            .thenAnswer(invocation -> tagFor(invocation.getArgument(1), "beds", "_BED"));
        when(server.getTag(eq(org.bukkit.Tag.REGISTRY_BLOCKS), any(org.bukkit.NamespacedKey.class), eq(Material.class)))
            .thenAnswer(invocation -> tagFor(invocation.getArgument(1), "wooden_doors", "_DOOR"));
    }

    private static org.bukkit.Tag<Material> tagFor(org.bukkit.NamespacedKey key, String knownKey, String suffix) {
        if (!key.getKey().equals(knownKey)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        org.bukkit.Tag<Material> tag = mock(org.bukkit.Tag.class);
        when(tag.isTagged(any(Material.class)))
            .thenAnswer(check -> check.getArgument(0).toString().endsWith(suffix));
        when(tag.getKey()).thenReturn(key);
        return tag;
    }

    @Test
    void parsesOrderedRanksAndChallenges() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:emerald"
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
                  beta:
                    display:
                      name: "<gray>Beta"
                      item: "minecraft:cobblestone"
              adept:
                display:
                  name: "<blue>Adept"
                lockedDisplayItem: "minecraft:diamond"
                challenges:
                  gamma:
                    display:
                      name: "<gray>Gamma"
                      item: "minecraft:iron_ingot"
            """));

        ChallengeCatalog catalog = result.catalog();
        assertEquals("starter", catalog.ranks().get(0).id().value());
        assertEquals("adept", catalog.ranks().get(1).id().value());
        assertEquals("alpha", catalog.ranks().get(0).challenges().get(0).id().value());
        assertEquals("beta", catalog.ranks().get(0).challenges().get(1).id().value());
        assertEquals("<green>Starter", catalog.ranks().get(0).display().name().source());
        assertEquals(Material.EMERALD, catalog.ranks().get(0).lockedDisplayItem().create().getType());
        assertEquals(Material.EMERALD, catalog.challenge(ChallengeId.of("alpha")).orElseThrow().lockedDisplayItem().create().getType());
        assertEquals(RankId.of("adept"), catalog.index().challengeOwners().get(ChallengeId.of("gamma")));
    }

    @Test
    void parsesTaggedRequirementsRewardsAndDefaults() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                unlock:
                  - type: completed-challenges
                    challenges: [bootstrap]
                challenges:
                  cobble:
                    lockedDisplayItem: "minecraft:obsidian"
                    display:
                      name: "<gray>Cobble"
                      description: "Collect cobble."
                      item: "minecraft:cobblestone"
                    unlock:
                      - type: permission
                        permission: usb.cobble
                    complete:
                      - type: inventory-items
                        items:
                          - item: "minecraft:cobblestone"
                            amount: 64
                            progression:
                              operator: add
                              increment: 2
                      - type: island-level
                        minimum: 10
                      - type: island-blocks
                        radius: 12
                        blocks:
                          - block: "minecraft:stone"
                            amount: 8
                      - type: entity-presence
                        radius: 6
                        entities:
                          - entity: sheep
                            count: 3
                            metadata:
                              Color: RED
                    repeat:
                      enabled: true
                      resetWindow: 12h
                      limit: 5
                    rewards:
                      first:
                        - type: item
                          items:
                            - item: "minecraft:leather"
                              amount: 3
                        - type: economy
                          amount: 10
                        - type: experience
                          amount: 4
                        - type: permission
                          permissions: [usb.reward]
                        - type: command
                          commands:
                            - execution: console
                              command: "say hi"
            """));

        ChallengeDefinition challenge = result.catalog().challenge(ChallengeId.of("cobble")).orElseThrow();
        assertEquals(Material.BARRIER, result.catalog().rank(RankId.of("starter")).orElseThrow().lockedDisplayItem().create().getType());
        assertEquals(Material.OBSIDIAN, challenge.lockedDisplayItem().create().getType());

        assertEquals("<gray>Cobble", challenge.display().name().source());
        assertEquals("Collect cobble.", challenge.display().description().source());
        assertTrue(challenge.properties().consumeItemsOnCompletion());
        assertEquals(new RepeatPolicy(true, Duration.ofHours(12), 5), challenge.repeatPolicy());
        assertEquals(4, challenge.completionRequirements().size());
        assertInstanceOf(ChallengeRequirements.InventoryItemsRequirement.class, challenge.completionRequirements().get(0));
        assertInstanceOf(ChallengeRequirements.IslandLevelRequirement.class, challenge.completionRequirements().get(1));
        assertInstanceOf(ChallengeRequirements.IslandBlocksRequirement.class, challenge.completionRequirements().get(2));
        assertInstanceOf(ChallengeRequirements.EntityPresenceRequirement.class, challenge.completionRequirements().get(3));
        assertEquals(5, challenge.firstCompletionReward().actions().size());
        assertInstanceOf(ChallengeRewards.ItemReward.class, challenge.firstCompletionReward().actions().get(0));
    }

    @Test
    void warnsOnUnknownKeysWithoutFailing() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            unknownRoot: true
            ranks:
              starter:
                unknownRank: true
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:emerald"
                challenges:
                  alpha:
                    weird: true
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
            """));

        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.unknownRoot")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.unknownRank")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.alpha.weird")));
    }

    @Test
    void fallsBackForRecoverableMissingPresentationFields() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                challenges:
                  alpha: {}
            """));

        var rank = result.catalog().rank(RankId.of("starter")).orElseThrow();
        var challenge = result.catalog().challenge(ChallengeId.of("alpha")).orElseThrow();

        assertEquals("starter", rank.display().name().source());
        assertEquals("", rank.display().description().source());
        assertEquals(Material.BARRIER, rank.lockedDisplayItem().create().getType());
        assertEquals("alpha", challenge.display().name().source());
        assertEquals("", challenge.display().description().source());
        assertEquals(Material.STONE, challenge.display().displayItem().create().getType());
        assertEquals(Material.BARRIER, challenge.lockedDisplayItem().create().getType());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.display")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.lockedDisplayItem")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.alpha.display")));
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.alpha.lockedDisplayItem")));
    }

    @Test
    void validatesSuspiciousButParseableChallenges() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
                    repeat:
                      enabled: true
                    rewards:
                      repeat:
                        - type: economy
                          amount: 5
                  beta:
                    display:
                      name: "<gray>Beta"
                      item: "minecraft:cobblestone"
                    unlock:
                      - type: completed-challenges
                        challenges: [missing]
            """));

        List<String> warnings = result.warnings();
        assertTrue(warnings.stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.alpha.complete")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.alpha.repeat")));
        assertTrue(warnings.stream().anyMatch(w -> w.contains("$.ranks.starter.challenges.beta.unlock[0]")));
        assertTrue(result.diagnostics().stream().allMatch(d -> d.severity() == ChallengeCatalogDiagnostic.Severity.WARNING));
    }

    @Test
    void parsesMixedCompletionRequirementKindsWithoutWarning() {
        // Mixed completion kinds (items + island blocks) are fully supported: the executor
        // evaluates each requirement and the menu renders each, so they parse cleanly.
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  brewer:
                    display:
                      name: "<gray>Brewer"
                      item: "minecraft:brewing_stand"
                    complete:
                      - type: inventory-items
                        items:
                          - item: "minecraft:potion"
                            amount: 3
                      - type: island-blocks
                        radius: 10
                        blocks:
                          - block: "minecraft:brewing_stand"
                            amount: 1
            """));

        ChallengeDefinition challenge = result.catalog().challenge(ChallengeId.of("brewer")).orElseThrow();
        assertEquals(2, challenge.completionRequirements().size());
        assertInstanceOf(ChallengeRequirements.InventoryItemsRequirement.class, challenge.completionRequirements().get(0));
        assertInstanceOf(ChallengeRequirements.IslandBlocksRequirement.class, challenge.completionRequirements().get(1));
        assertFalse(result.warnings().stream().anyMatch(w -> w.contains("Mixes completion requirement kinds")));
    }

    @Test
    void warnsWhenCompletedRankMinimumExceedsRankSize() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
              adept:
                display:
                  name: "<blue>Adept"
                lockedDisplayItem: "minecraft:barrier"
                unlock:
                  - type: completed-rank
                    rank: starter
                    minimumCompletedChallenges: 5
                challenges:
                  beta:
                    display:
                      name: "<gray>Beta"
                      item: "minecraft:cobblestone"
            """));

        assertTrue(result.warnings().stream().anyMatch(w ->
            w.contains("$.ranks.adept.unlock[0]") && w.contains("only has 1")));
    }

    @Test
    void parsesIslandLevelRankGatesAndRankRepeatDefaults() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              tier2:
                display:
                  name: "<green>Homesteader"
                lockedDisplayItem: "minecraft:barrier"
                unlock:
                  - type: island-level
                    minimum: 20
                  - type: completed-rank
                    rank: tier1
                    minimumCompletedChallenges: 7
                challengeDefaults:
                  repeat:
                    resetWindow: 20h
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
                    repeat:
                      enabled: true
                  beta:
                    display:
                      name: "<gray>Beta"
                      item: "minecraft:cobblestone"
                    repeat:
                      enabled: true
                      resetWindow: 4h
            """));

        var rank = result.catalog().rank(RankId.of("tier2")).orElseThrow();
        assertInstanceOf(ChallengeRequirements.IslandLevelRequirement.class, rank.unlockRequirements().get(0));
        assertEquals(20d, ((ChallengeRequirements.IslandLevelRequirement) rank.unlockRequirements().get(0)).minimumLevel());
        // Rank-wide default applies when a challenge does not set its own reset window.
        assertEquals(Duration.ofHours(20), result.catalog().challenge(ChallengeId.of("alpha")).orElseThrow().repeatPolicy().resetWindow());
        assertEquals(Duration.ofHours(4), result.catalog().challenge(ChallengeId.of("beta")).orElseThrow().repeatPolicy().resetWindow());
    }

    @Test
    void parsesBiomeRewards() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  fisherman:
                    display:
                      name: "<gray>Fisherman"
                      item: "minecraft:fishing_rod"
                    rewards:
                      first:
                        - type: biome
                          biomes: [Deep_Ocean, jungle]
            """));

        var challenge = result.catalog().challenge(ChallengeId.of("fisherman")).orElseThrow();
        var reward = (ChallengeRewards.BiomeReward) challenge.firstCompletionReward().actions().getFirst();
        // Keys are normalized to lower case.
        assertEquals(List.of("deep_ocean", "jungle"), reward.biomes());
    }

    @Test
    void parsesTagItemRequirements() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  homeowner:
                    display:
                      name: "<gray>Homeowner"
                      item: "minecraft:white_bed"
                    complete:
                      - type: inventory-items
                        items:
                          - item: "#minecraft:beds"
                            amount: 1
            """));

        var challenge = result.catalog().challenge(ChallengeId.of("homeowner")).orElseThrow();
        var items = (ChallengeRequirements.InventoryItemsRequirement) challenge.completionRequirements().getFirst();
        var matcher = (ChallengeRequirements.ItemTag) items.items().getFirst().matcher();
        assertEquals("minecraft:beds", matcher.key());
        assertTrue(items.items().getFirst().matches(new org.bukkit.inventory.ItemStack(Material.RED_BED)));
        assertFalse(items.items().getFirst().matches(new org.bukkit.inventory.ItemStack(Material.STONE)));
    }

    @Test
    void rejectsUnknownItemTags() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
                    complete:
                      - type: inventory-items
                        items:
                          - item: "#minecraft:nosuchtag"
                            amount: 1
            """)));

        assertTrue(error.getMessage().contains("Unknown item tag"));
    }

    @Test
    void parsesAnyOfItemAndBlockGroups() {
        ChallengeCatalogParseResult result = parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  homeowner:
                    display:
                      name: "<gray>Homeowner"
                      item: "minecraft:white_bed"
                    complete:
                      - type: inventory-items
                        items:
                          - item: ["minecraft:oak_door", "minecraft:spruce_door"]
                            amount: 1
                      - type: island-blocks
                        radius: 10
                        blocks:
                          - block: "#minecraft:wooden_doors"
                            amount: 1
                          - block: ["minecraft:lectern", "minecraft:smithing_table"]
                            amount: 1
            """));

        var challenge = result.catalog().challenge(ChallengeId.of("homeowner")).orElseThrow();
        var items = (ChallengeRequirements.InventoryItemsRequirement) challenge.completionRequirements().get(0);
        var itemMatcher = (ChallengeRequirements.AnyOfItems) items.items().getFirst().matcher();
        assertEquals(2, itemMatcher.matchers().size());
        assertTrue(items.items().getFirst().matches(new org.bukkit.inventory.ItemStack(Material.OAK_DOOR)));
        assertFalse(items.items().getFirst().matches(new org.bukkit.inventory.ItemStack(Material.IRON_DOOR)));

        var blocks = (ChallengeRequirements.IslandBlocksRequirement) challenge.completionRequirements().get(1);
        var blockTag = (ChallengeRequirements.BlockTag) blocks.blocks().get(0).matcher();
        assertEquals("minecraft:wooden_doors", blockTag.key());
        assertTrue(blocks.blocks().get(0).matches(Material.SPRUCE_DOOR));
        var blockAnyOf = (ChallengeRequirements.AnyOfBlocks) blocks.blocks().get(1).matcher();
        assertTrue(blockAnyOf.matches(Material.LECTERN));
        assertTrue(blockAnyOf.matches(Material.SMITHING_TABLE));
        assertFalse(blockAnyOf.matches(Material.STONE));
    }

    @Test
    void rejectsUnknownBlockTags() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  alpha:
                    display:
                      name: "<gray>Alpha"
                      item: "minecraft:stone"
                    complete:
                      - type: island-blocks
                        radius: 10
                        blocks:
                          - block: "#minecraft:nosuchblocktag"
                            amount: 1
            """)));

        assertTrue(error.getMessage().contains("Unknown block tag"));
    }

    @Test
    void rejectsMissingSchemaVersion() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:emerald"
                challenges: {}
            """)));

        assertTrue(error.getMessage().contains("schemaVersion"));
    }

    @Test
    void rejectsLegacyShape() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.parse(load("""
            allowChallenges: true
            ranks:
              Tier1:
                name: "&7Novice"
                displayItem: cyan_terracotta
                challenges:
                  cobblestonegenerator:
                    name: "&7Cobble"
                    type: onPlayer
                    requiredItems:
                      - cobblestone:64;+2
                    reward:
                      text: "test"
                      items:
                        - leather:3
            """)));

        assertTrue(error.getMessage().contains("schemaVersion"));
    }

    private static YamlConfiguration load(String yaml) {
        return YamlConfiguration.loadConfiguration(new StringReader(yaml));
    }
}
