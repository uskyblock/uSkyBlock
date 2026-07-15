package us.talabrek.ultimateskyblock.ittest.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VerdictCodecTest {
    @Test
    void roundTripsEscapedDetail() {
        Verdict verdict = new Verdict(1, "fresh", "canary", Verdict.Result.FAIL, 42,
            Verdict.Category.HARNESS_ERROR, "line one\n\"quoted\" password=hunter2");
        Verdict decoded = VerdictCodec.decode(VerdictCodec.encode(verdict));
        assertEquals("line one \"quoted\" password=<redacted>", decoded.detail());
        assertEquals(verdict.phase(), decoded.phase());
        assertEquals(verdict.category(), decoded.category());
    }

    @Test
    void rejectsMissingExtraAndDuplicateFields() {
        assertThrows(IllegalArgumentException.class, () -> VerdictCodec.decode("{}"));
        assertThrows(IllegalArgumentException.class, () -> VerdictCodec.decode(
            "{\"schema\":1,\"phase\":\"fresh\",\"scenario\":\"x\",\"result\":\"PASS\",\"durationMs\":0,\"category\":\"NONE\",\"detail\":\"\",\"extra\":1}"
        ));
        assertThrows(IllegalArgumentException.class, () -> VerdictCodec.decode(
            "{\"schema\":1,\"schema\":1,\"phase\":\"fresh\",\"scenario\":\"x\",\"result\":\"PASS\",\"durationMs\":0,\"category\":\"NONE\",\"detail\":\"\"}"
        ));
    }
}
