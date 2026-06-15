package us.talabrek.ultimateskyblock.challenge.catalog.bootstrap;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.yaml.ChallengeCatalogYamlParser;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeCatalogLoaderTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        BukkitServerMock.setupServerMock();
    }

    @Test
    void loadsEmptyCatalogInsteadOfFailingOnBrokenFile() throws Exception {
        // Legacy shape without schemaVersion - the parser rejects it with an exception.
        Files.writeString(tempDir.resolve("challenges.yml"), """
            allowChallenges: true
            ranks:
              Tier1:
                name: "&7Novice"
            """);

        ChallengeCatalogLoader loader = new ChallengeCatalogLoader(
            tempDir, Logger.getAnonymousLogger(), new ChallengeCatalogYamlParser(new GameObjectFactory()));

        ChallengeCatalog catalog = loader.load();

        assertTrue(catalog.ranks().isEmpty());
    }
}
