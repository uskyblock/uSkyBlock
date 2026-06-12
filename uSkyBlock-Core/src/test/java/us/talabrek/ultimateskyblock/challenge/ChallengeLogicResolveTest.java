package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeId;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeProperties;
import us.talabrek.ultimateskyblock.challenge.catalog.DisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDefinition;
import us.talabrek.ultimateskyblock.challenge.catalog.RankDisplaySpec;
import us.talabrek.ultimateskyblock.challenge.catalog.RankId;
import us.talabrek.ultimateskyblock.challenge.catalog.RepeatPolicy;
import us.talabrek.ultimateskyblock.challenge.catalog.RewardBundle;
import us.talabrek.ultimateskyblock.challenge.catalog.TextSpec;
import us.talabrek.ultimateskyblock.gameobject.GameObjectFactory;
import us.talabrek.ultimateskyblock.gameobject.ItemStackSpec;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for ChallengeLogic.resolveChallenge matching behavior.
 */
public class ChallengeLogicResolveTest {

    private ChallengeLogic logic;

    private ChallengeDefinition chCobbleGen;      // id: cobblestonegenerator, display: Cobblestone Generator
    private ChallengeDefinition chCobbleGolem;    // id: cobblegolem,          display: Cobblestone Golem
    private ChallengeDefinition chSandCastle;     // id: sandcastle,           display: Sand Castle

    @BeforeEach
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();

        // Create a mock that calls real methods but does not invoke the real constructor
        logic = Mockito.mock(ChallengeLogic.class, Mockito.CALLS_REAL_METHODS);

        chCobbleGen = definition("cobblestonegenerator", "<green>Cobblestone Generator");
        chCobbleGolem = definition("cobblegolem", "<green>Cobblestone Golem");
        chSandCastle = definition("sandcastle", "<yellow>Sand Castle");

        ChallengeCatalog catalog = new ChallengeCatalog(List.of(new RankDefinition(
            RankId.of("starter"),
            new RankDisplaySpec(TextSpec.miniMessage("Starter"), TextSpec.empty()),
            new GameObjectFactory().itemStack("minecraft:barrier"),
            List.of(),
            List.of(chCobbleGen, chCobbleGolem, chSandCastle)
        )));

        Field catalogField = ChallengeLogic.class.getDeclaredField("catalog");
        catalogField.setAccessible(true);
        catalogField.set(logic, catalog);
    }

    private static ChallengeDefinition definition(String id, String miniMessageName) {
        ItemStackSpec stone = new GameObjectFactory().itemStack("minecraft:stone");
        return new ChallengeDefinition(
            ChallengeId.of(id),
            new DisplaySpec(TextSpec.miniMessage(miniMessageName), TextSpec.empty(), stone),
            stone,
            List.of(),
            List.of(),
            new ChallengeProperties(true),
            new RepeatPolicy(false, Duration.ZERO, 0),
            RewardBundle.empty(),
            RewardBundle.empty()
        );
    }

    @Test
    public void exactId_caseInsensitive_matches() {
        var res = logic.resolveChallenge("CobbleStoneGenerator");
        assertThat(res.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.FOUND));
        assertThat(res.getChallenge(), is(chCobbleGen));
    }

    @Test
    public void exactDisplayName_colorStripped_matches() {
        var res = logic.resolveChallenge("Cobblestone Generator");
        assertThat(res.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.FOUND));
        assertThat(res.getChallenge(), is(chCobbleGen));
    }

    @Test
    public void exactSlug_matchesIdOrDisplay() {
        // Spaces removed, mixed case
        var res1 = logic.resolveChallenge("CobblestoneGenerator");
        assertThat(res1.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.FOUND));
        assertThat(res1.getChallenge(), is(chCobbleGen));

        var res2 = logic.resolveChallenge("sand    castle");
        assertThat(res2.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.FOUND));
        assertThat(res2.getChallenge(), is(chSandCastle));
    }

    @Test
    public void uniquePrefixOnSlug_findsSingleCandidate() {
        var res = logic.resolveChallenge("sand ca"); // slug: sandca → matches only sandcastle
        assertThat(res.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.FOUND));
        assertThat(res.getChallenge(), is(chSandCastle));
    }

    @Test
    public void ambiguousPrefixOnSlug_returnsAmbiguousWithSuggestions() {
        var res = logic.resolveChallenge("cobble"); // slug prefix matches both cobblestonegenerator and cobblegolem
        assertThat(res.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.AMBIGUOUS));
        List<String> suggestions = res.getSuggestions();
        assertThat(suggestions, hasItems("Cobblestone Generator", "Cobblestone Golem"));
    }

    @Test
    public void notFound_returnsNotFound() {
        var res = logic.resolveChallenge("no such challenge");
        assertThat(res.getStatus(), is(ChallengeLogic.ChallengeLookupResult.Status.NOT_FOUND));
    }
}
