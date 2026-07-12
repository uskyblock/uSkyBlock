package us.talabrek.ultimateskyblock.ittest.result;

import java.util.Objects;

public record Verdict(
    int schema,
    String phase,
    String scenario,
    Result result,
    long durationMs,
    Category category,
    String detail
) {
    public static final int SCHEMA_VERSION = 1;

    public Verdict {
        if (schema != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported verdict schema: " + schema);
        }
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(scenario, "scenario");
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(detail, "detail");
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs cannot be negative");
        }
    }

    public enum Result { PASS, FAIL, SKIP }

    public enum Category {
        NONE,
        PLUGIN_FAIL,
        DEPENDENCY_FAIL,
        HARNESS_ERROR,
        UNATTRIBUTED_ERROR
    }
}
