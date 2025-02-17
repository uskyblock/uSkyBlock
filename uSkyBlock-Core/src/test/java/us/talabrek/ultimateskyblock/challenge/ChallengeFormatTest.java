package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import us.talabrek.ultimateskyblock.player.PlayerInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

public class ChallengeFormatTest {

    private static ChallengeLogic challengeLogic;
    private static PlayerInfo playerInfo;

    @BeforeClass
    public static void beforeAll() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        playerInfo = Mockito.mock(PlayerInfo.class);
        challengeLogic = Mockito.mock(ChallengeLogic.class);

        setupChallengeMock(playerInfo, "cobblestonegenerator", "§9Cobble Stone Generator");
        setupChallengeMock(playerInfo, "pumpkinfarmer", "§aPumpkin §9Farmer");
    }

    private static void setupChallengeMock(PlayerInfo playerInfo, String challengeName, String displayName) {
        ChallengeCompletion challengeCompletion = Mockito.mock(ChallengeCompletion.class);
        when(challengeCompletion.getTimesCompleted()).thenReturn(0);
        when(challengeCompletion.getName()).thenReturn(challengeName);
        when(playerInfo.getChallenge(MockitoHamcrest.argThat(is(challengeName)))).thenReturn(challengeCompletion);

        Challenge challenge = Mockito.mock(Challenge.class);
        when(challenge.getDisplayName()).thenReturn(displayName);
        when(challengeLogic.getChallenge(MockitoHamcrest.argThat(is(challengeName)))).thenReturn(challenge);
    }

    @Test
    public void getMissingRequirement_ZeroCompleted() {
        ChallengeCompletion pumpkinfarmer = playerInfo.getChallenge("pumpkinfarmer");
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(0);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§7§9Cobble Stone Generator, §f2x §7§aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_PartiallyCompleted() {
        ChallengeCompletion pumpkinfarmer = playerInfo.getChallenge("pumpkinfarmer");
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(1);
        ChallengeCompletion cobblestonegenerator = playerInfo.getChallenge("cobblestonegenerator");
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(0);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§7§9Cobble Stone Generator, §7§aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_OneFullyCompleted() {
        ChallengeCompletion pumpkinfarmer = playerInfo.getChallenge("pumpkinfarmer");
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(0);
        ChallengeCompletion cobblestonegenerator = playerInfo.getChallenge("cobblestonegenerator");
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(1);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§f2x §7§aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_AllFullyCompleted() {
        ChallengeCompletion pumpkinfarmer = playerInfo.getChallenge("pumpkinfarmer");
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(2);
        ChallengeCompletion cobblestonegenerator = playerInfo.getChallenge("cobblestonegenerator");
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(1);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is(nullValue()));
    }
}
