package us.talabrek.ultimateskyblock.ittest.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class HarnessCli {
    private HarnessCli() {
    }

    public static void main(String[] args) {
        int exit = run(args);
        if (exit != 0) System.exit(exit);
    }

    static int run(String[] args) {
        if (args.length == 2 && args[0].equals("offline-uuid")) {
            System.out.println(offlineUuid(args[1]));
            return 0;
        }
        if (args.length >= 1 && args[0].equals("classify")) {
            return classify(Arrays.copyOfRange(args, 1, args.length));
        }
        System.err.println("Usage: java -jar uSkyBlock-ittest.jar offline-uuid <name>");
        System.err.println("   or: java -jar uSkyBlock-ittest.jar classify --player-flows <true|false> --results <file>... --logs <file>...");
        return HarnessClassifier.EXIT_HARNESS_ERROR;
    }

    private static int classify(String[] args) {
        Boolean playerFlows = null;
        List<Path> results = new ArrayList<>();
        List<Path> logs = new ArrayList<>();
        List<Path> current = null;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--player-flows" -> {
                    if (++i >= args.length) return usage("--player-flows requires a value");
                    if (!args[i].equals("true") && !args[i].equals("false")) return usage("--player-flows must be true or false");
                    playerFlows = Boolean.valueOf(args[i]);
                    current = null;
                }
                case "--results" -> current = results;
                case "--logs" -> current = logs;
                default -> {
                    if (current == null) return usage("unexpected argument: " + args[i]);
                    current.add(Path.of(args[i]));
                }
            }
        }
        if (playerFlows == null || results.isEmpty() || logs.isEmpty()) return usage("player-flows, results, and logs are required");
        try {
            HarnessClassifier.Classification classification = HarnessClassifier.classify(playerFlows, results, logs);
            System.out.println(classification.outcome());
            classification.details().forEach(detail -> System.out.println(" - " + detail));
            return classification.exitCode();
        } catch (RuntimeException e) {
            return usage(e.getMessage());
        }
    }

    static UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static int usage(String message) {
        System.err.println(message);
        return HarnessClassifier.EXIT_HARNESS_ERROR;
    }
}
