package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import us.talabrek.ultimateskyblock.challenge.Rank;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.config.runtime.RuntimeConfig;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.challenge.catalog.yaml.ChallengeCatalogYamlParser;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChallengeCatalogRuntimeAdapterTest {
    private final ChallengeCatalogYamlParser parser = new ChallengeCatalogYamlParser(new GameObjectFactory());

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
    }

    @Test
    void skipsChallengesMixingCompletionRequirementKinds() {
        ChallengeCatalog catalog = parser.parse(YamlConfiguration.loadConfiguration(new StringReader("""
            schemaVersion: 1
            ranks:
              starter:
                display:
                  name: "<green>Starter"
                lockedDisplayItem: "minecraft:barrier"
                challenges:
                  plain:
                    display:
                      name: "<gray>Plain"
                      item: "minecraft:stone"
                    complete:
                      - type: inventory-items
                        items:
                          - item: "minecraft:cobblestone"
                            amount: 8
                  mixed:
                    display:
                      name: "<gray>Mixed"
                      item: "minecraft:brewing_stand"
                    complete:
                      - type: inventory-items
                        items:
                          - item: "minecraft:potion"
                            amount: 3
                      - type: island-level
                        minimum: 10
            """))).catalog();

        Map<String, Rank> ranks = new ChallengeCatalogRuntimeAdapter().adapt(catalog, challengeSettings());

        // The mixed challenge is skipped (reported as an ERROR diagnostic at parse time) instead
        // of aborting plugin enable.
        assertEquals(List.of("plain"), ranks.get("starter").getChallenges().stream()
            .map(challenge -> challenge.getId().id())
            .toList());
    }

    private static RuntimeConfig.Challenges challengeSettings() {
        return new RuntimeConfig.Challenges(true, false, false, new RuntimeConfig.Broadcast(true, "&6"));
    }
}
