package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.util.BukkitServerMock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

/**
 * Tests for ChallengeLogic.resolveChallenge matching behavior.
 */
public class ChallengeLogicResolveTest {

    private ChallengeLogic logic;

    private Challenge chCobbleGen;      // id: cobblestonegenerator, display: §aCobblestone Generator
    private Challenge chCobbleGolem;    // id: cobblegolem,          display: §aCobblestone Golem
    private Challenge chSandCastle;     // id: sandcastle,           display: §eSand Castle

    @Before
    public void setUp() throws Exception {
        BukkitServerMock.setupServerMock();

        // Create a mock that calls real methods but does not invoke the real constructor
        logic = Mockito.mock(ChallengeLogic.class, Mockito.CALLS_REAL_METHODS);

        // Prepare test challenge doubles with minimal behavior used by resolver
        chCobbleGen = Mockito.mock(Challenge.class);
        when(chCobbleGen.getId()).thenReturn(ChallengeKey.of("cobblestonegenerator"));
        when(chCobbleGen.getDisplayName()).thenReturn("\u00a7aCobblestone Generator");

        chCobbleGolem = Mockito.mock(Challenge.class);
        when(chCobbleGolem.getId()).thenReturn(ChallengeKey.of("cobblegolem"));
        when(chCobbleGolem.getDisplayName()).thenReturn("\u00a7aCobblestone Golem");

        chSandCastle = Mockito.mock(Challenge.class);
        when(chSandCastle.getId()).thenReturn(ChallengeKey.of("sandcastle"));
        when(chSandCastle.getDisplayName()).thenReturn("\u00a7eSand Castle");

        // Inject into the private byId map
        Field byIdField = ChallengeLogic.class.getDeclaredField("byId");
        byIdField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<ChallengeKey, Challenge> byId = (Map<ChallengeKey, Challenge>) byIdField.get(logic);
        if (byId == null) {
            byId = new HashMap<>();
            byIdField.set(logic, byId);
        } else {
            byId.clear();
        }
        byId.put(chCobbleGen.getId(), chCobbleGen);
        byId.put(chCobbleGolem.getId(), chCobbleGolem);
        byId.put(chSandCastle.getId(), chSandCastle);
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
