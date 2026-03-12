package us.talabrek.ultimateskyblock.challenge;

import dk.lockfuglsang.minecraft.po.I18nUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import us.talabrek.ultimateskyblock.player.PlayerInfo;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

public class ChallengeFormatTest {

    private static ChallengeLogic challengeLogic;
    private static PlayerInfo playerInfo;

    @BeforeAll
    public static void beforeAll() {
        I18nUtil.initialize(new File("."), Locale.ENGLISH);
        playerInfo = Mockito.mock(PlayerInfo.class);
        challengeLogic = Mockito.mock(ChallengeLogic.class);

        setupChallengeMock(playerInfo, "cobblestonegenerator", "§9Cobble Stone Generator");
        setupChallengeMock(playerInfo, "pumpkinfarmer", "§aPumpkin §9Farmer");
    }

    private static void setupChallengeMock(PlayerInfo playerInfo, String challengeName, String displayName) {
        ChallengeKey key = ChallengeKey.of(challengeName);

        ChallengeCompletion challengeCompletion = Mockito.mock(ChallengeCompletion.class);
        when(challengeCompletion.getTimesCompleted()).thenReturn(0);
        when(challengeCompletion.getId()).thenReturn(key);
        when(challengeLogic.getChallengeCompletion(MockitoHamcrest.argThat(is(playerInfo)), MockitoHamcrest.argThat(is(key)))).thenReturn(challengeCompletion);

        Challenge challenge = Mockito.mock(Challenge.class);
        when(challenge.getDisplayName()).thenReturn(displayName);
        when(challengeLogic.getChallengeById(MockitoHamcrest.argThat(is(key)))).thenReturn(Optional.of(challenge));
    }

    @Test
    public void getMissingRequirement_ZeroCompleted() {
        ChallengeCompletion pumpkinfarmer = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("pumpkinfarmer"));
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(0);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§9Cobble Stone Generator, §a2§7x §aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_PartiallyCompleted() {
        ChallengeCompletion pumpkinfarmer = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("pumpkinfarmer"));
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(1);
        ChallengeCompletion cobblestonegenerator = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("cobblestonegenerator"));
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(0);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§9Cobble Stone Generator, §aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_OneFullyCompleted() {
        ChallengeCompletion pumpkinfarmer = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("pumpkinfarmer"));
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(0);
        ChallengeCompletion cobblestonegenerator = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("cobblestonegenerator"));
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(1);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is("§a2§7x §aPumpkin §9Farmer"));
    }

    @Test
    public void getMissingRequirement_AllFullyCompleted() {
        ChallengeCompletion pumpkinfarmer = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("pumpkinfarmer"));
        when(pumpkinfarmer.getTimesCompleted()).thenReturn(2);
        ChallengeCompletion cobblestonegenerator = challengeLogic.getChallengeCompletion(playerInfo, ChallengeKey.of("cobblestonegenerator"));
        when(cobblestonegenerator.getTimesCompleted()).thenReturn(1);

        String missingRequirement = ChallengeFormat.getMissingRequirement(playerInfo, Arrays.asList("cobblestonegenerator", "pumpkinfarmer:2"), challengeLogic);
        assertThat(missingRequirement, is(nullValue()));
    }
}
