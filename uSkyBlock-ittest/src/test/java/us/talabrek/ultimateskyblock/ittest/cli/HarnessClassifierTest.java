package us.talabrek.ultimateskyblock.ittest.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import us.talabrek.ultimateskyblock.ittest.result.Verdict;
import us.talabrek.ultimateskyblock.ittest.result.VerdictCodec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class HarnessClassifierTest {
    @TempDir Path temporary;

    @Test
    void passesOnlyWithExactlyOneCleanRequiredVerdict() throws Exception {
        Path results = writeRequired("latest-canary", null);
        Path log = Files.writeString(temporary.resolve("server.log"), "server booted\nserver stopped\n");
        assertEquals(HarnessClassifier.Outcome.PASS,
            HarnessClassifier.classify("latest-canary", List.of(results), List.of(log)).outcome());
    }

    @Test
    void pluginAssertionIsFail() throws Exception {
        Path results = writeRequired("latest-canary", "fresh/secondary-smokes");
        Path log = Files.writeString(temporary.resolve("server.log"), "clean\n");
        assertEquals(HarnessClassifier.Outcome.FAIL,
            HarnessClassifier.classify("latest-canary", List.of(results), List.of(log)).outcome());
    }

    @Test
    void zeroMissingDuplicateAndMalformedVerdictsAreHarnessErrors() throws Exception {
        Path log = Files.writeString(temporary.resolve("server.log"), "clean\n");
        Path empty = Files.writeString(temporary.resolve("empty.jsonl"), "");
        assertEquals(HarnessClassifier.Outcome.HARNESS_ERROR,
            HarnessClassifier.classify("latest-canary", List.of(empty), List.of(log)).outcome());

        Path missing = writeRequired("latest-canary", null);
        List<String> lines = new ArrayList<>(Files.readAllLines(missing));
        lines.removeLast();
        Files.write(missing, lines);
        assertEquals(HarnessClassifier.Outcome.HARNESS_ERROR,
            HarnessClassifier.classify("latest-canary", List.of(missing), List.of(log)).outcome());

        Path duplicate = writeRequired("latest-canary", null);
        lines = new ArrayList<>(Files.readAllLines(duplicate));
        lines.add(lines.getFirst());
        Files.write(duplicate, lines);
        assertEquals(HarnessClassifier.Outcome.HARNESS_ERROR,
            HarnessClassifier.classify("latest-canary", List.of(duplicate), List.of(log)).outcome());

        Path malformed = writeRequired("latest-canary", null);
        Files.writeString(malformed, "not-json\n");
        assertEquals(HarnessClassifier.Outcome.HARNESS_ERROR,
            HarnessClassifier.classify("latest-canary", List.of(malformed), List.of(log)).outcome());
    }

    @Test
    void attributesScenarioErrorsButRejectsStartupAndShutdownErrors() throws Exception {
        Path attributed = Files.writeString(temporary.resolve("attributed.log"), String.join("\n",
            "USKYBLOCK-TEST START phase=fresh scenario=x",
            "[Server thread/ERROR]: assertion context",
            "java.lang.IllegalStateException: captured",
            "USKYBLOCK-TEST END phase=fresh scenario=x result=FAIL"
        ));
        assertFalse(HarnessClassifier.scanLogs(List.of(attributed)).iterator().hasNext());

        Path startup = Files.writeString(temporary.resolve("startup.log"), "[Server thread/ERROR]: plugin load failed\n");
        assertEquals(1, HarnessClassifier.scanLogs(List.of(startup)).size());
        Path shutdown = Files.writeString(temporary.resolve("shutdown.log"), String.join("\n",
            "USKYBLOCK-TEST START phase=fresh scenario=x",
            "USKYBLOCK-TEST END phase=fresh scenario=x result=PASS",
            "Caused by: java.lang.IllegalStateException: shutdown"
        ));
        assertEquals(1, HarnessClassifier.scanLogs(List.of(shutdown)).size());
    }

    private Path writeRequired(String lane, String failed) throws Exception {
        List<String> lines = new ArrayList<>();
        for (String required : HarnessClassifier.requiredScenarios(lane)) {
            String[] pieces = required.split("/", 2);
            boolean isFailure = required.equals(failed);
            lines.add(VerdictCodec.encode(new Verdict(1, pieces[0], pieces[1],
                isFailure ? Verdict.Result.FAIL : Verdict.Result.PASS, 1,
                isFailure ? Verdict.Category.PLUGIN_FAIL : Verdict.Category.NONE,
                isFailure ? "assertion" : "ok")));
        }
        Path file = temporary.resolve("results-" + System.nanoTime() + ".jsonl");
        Files.write(file, lines);
        return file;
    }
}
