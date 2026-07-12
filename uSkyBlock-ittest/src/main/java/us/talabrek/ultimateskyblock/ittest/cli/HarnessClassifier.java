package us.talabrek.ultimateskyblock.ittest.cli;

import us.talabrek.ultimateskyblock.ittest.result.Verdict;
import us.talabrek.ultimateskyblock.ittest.result.VerdictCodec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public final class HarnessClassifier {
    public static final int EXIT_PASS = 0;
    public static final int EXIT_FAIL = 10;
    public static final int EXIT_HARNESS_ERROR = 20;

    // A Paper/Spigot ERROR or SEVERE level line ("... ERROR]:"), a "Caused by:" line, or a
    // stack-trace throwable class (including *Error throwables such as LinkageError/
    // NoSuchMethodError - the NMS-break signature this harness exists to catch). The throwable
    // header is enough to flag a trace, so individual "at ..." frames are intentionally not matched
    // (one error should be one finding). Case-sensitive so prose such as "error" is not matched.
    private static final Pattern ERROR_LINE = Pattern.compile(
        "(?:ERROR|SEVERE)\\]:"
            + "|(?:^|\\s)Caused by:"
            + "|(?:^|\\s)[A-Za-z0-9_.$]+(?:Exception|Error)(?::|$)"
    );
    private static final List<Pattern> LOG_ALLOWLIST = List.of(
        Pattern.compile(".*Server thread/WARN.*Can't keep up!.*"),
        Pattern.compile(".*Legacy plugin uSkyBlock.*"),
        Pattern.compile(".*USKYBLOCK-TEST.*")
    );

    private HarnessClassifier() {
    }

    public static Classification classify(boolean playerFlows, List<Path> resultFiles, List<Path> logFiles) {
        Map<String, Verdict> unique = new HashMap<>();
        List<String> errors = new ArrayList<>();
        for (Path file : resultFiles) {
            if (!Files.isRegularFile(file)) {
                errors.add("missing results file: " + file);
                continue;
            }
            try {
                List<String> lines = Files.readAllLines(file);
                if (lines.isEmpty()) errors.add("empty results file: " + file);
                for (int i = 0; i < lines.size(); i++) {
                    if (lines.get(i).isBlank()) {
                        errors.add("blank verdict at " + file + ':' + (i + 1));
                        continue;
                    }
                    try {
                        Verdict verdict = VerdictCodec.decode(lines.get(i));
                        String key = verdict.phase() + '/' + verdict.scenario();
                        if (unique.putIfAbsent(key, verdict) != null) {
                            errors.add("duplicate verdict: " + key);
                        }
                    } catch (RuntimeException e) {
                        errors.add("malformed verdict at " + file + ':' + (i + 1) + ": " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                errors.add("cannot read results " + file + ": " + e.getMessage());
            }
        }

        for (String required : requiredScenarios(playerFlows)) {
            if (!unique.containsKey(required)) errors.add("missing required verdict: " + required);
        }
        for (Verdict verdict : unique.values()) {
            if (verdict.result() == Verdict.Result.SKIP && requiredScenarios(playerFlows).contains(verdict.phase() + '/' + verdict.scenario())) {
                errors.add("required verdict was skipped: " + verdict.phase() + '/' + verdict.scenario());
            }
        }

        errors.addAll(scanLogs(logFiles));
        if (!errors.isEmpty()) {
            return new Classification(Outcome.HARNESS_ERROR, EXIT_HARNESS_ERROR, errors);
        }

        List<String> failures = unique.values().stream()
            .filter(v -> v.result() == Verdict.Result.FAIL)
            .map(v -> v.phase() + '/' + v.scenario() + " [" + v.category().name().replace('_', '-') + "]: " + v.detail())
            .toList();
        if (!failures.isEmpty()) {
            // Any scenario that ran and failed is a red build. We deliberately do NOT re-route by
            // category: harness-vs-plugin can't be told apart from a thrown exception, and both fail
            // the build anyway. The category stays on each verdict purely as a triage label.
            return new Classification(Outcome.FAIL, EXIT_FAIL, failures);
        }
        return new Classification(Outcome.PASS, EXIT_PASS, List.of("all required scenarios passed and logs are clean"));
    }

    // The required set is a function of capability, not lane name: a lane whose Minecraft version has
    // a presence-client codec runs the full player-driven set plus the restart-persistence phase;
    // a server-only lane (no codec) runs the fresh smoke set only.
    static Set<String> requiredScenarios(boolean playerFlows) {
        if (playerFlows) {
            return Set.of(
                "fresh/harness-canary",
                "fresh/initial-setup",
                "fresh/create-island",
                "fresh/complete-challenge",
                "fresh/secondary-smokes",
                "restart/harness-canary",
                "restart/restart-persistence",
                "restart/secondary-smokes"
            );
        }
        return Set.of(
            "fresh/harness-canary",
            "fresh/initial-setup",
            "fresh/secondary-smokes"
        );
    }

    static List<String> scanLogs(List<Path> logs) {
        List<String> errors = new ArrayList<>();
        for (Path log : logs) {
            if (!Files.isRegularFile(log)) {
                errors.add("missing server log: " + log);
                continue;
            }
            try {
                int lineNumber = 0;
                boolean attributedScenario = false;
                for (String line : Files.readAllLines(log)) {
                    lineNumber++;
                    if (line.contains("USKYBLOCK-TEST START phase=")) {
                        attributedScenario = true;
                        continue;
                    }
                    if (line.contains("USKYBLOCK-TEST END phase=")) {
                        attributedScenario = false;
                        continue;
                    }
                    if (!attributedScenario && ERROR_LINE.matcher(line).find()
                        && LOG_ALLOWLIST.stream().noneMatch(pattern -> pattern.matcher(line).matches())) {
                        errors.add("unattributed log error " + log + ':' + lineNumber + ": " + sanitize(line));
                    }
                }
            } catch (IOException e) {
                errors.add("cannot read server log " + log + ": " + e.getMessage());
            }
        }
        return errors;
    }

    private static String sanitize(String line) {
        String sanitized = VerdictCodec.sanitize(line);
        return sanitized.length() <= 300 ? sanitized : sanitized.substring(0, 297) + "...";
    }

    public enum Outcome { PASS, FAIL, HARNESS_ERROR }

    public record Classification(Outcome outcome, int exitCode, List<String> details) {
        public Classification {
            details = List.copyOf(new LinkedHashSet<>(details));
        }
    }
}
