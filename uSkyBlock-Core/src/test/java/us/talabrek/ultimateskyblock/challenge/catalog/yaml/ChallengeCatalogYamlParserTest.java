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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.anyString;
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
