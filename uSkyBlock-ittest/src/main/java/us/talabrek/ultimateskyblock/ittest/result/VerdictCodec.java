package us.talabrek.ultimateskyblock.ittest.result;

import java.util.Map;
import java.util.Set;

public final class VerdictCodec {
    private static final Set<String> KEYS = Set.of(
        "schema", "phase", "scenario", "result", "durationMs", "category", "detail"
    );

    private VerdictCodec() {
    }

    public static String encode(Verdict verdict) {
        return "{" +
            "\"schema\":" + verdict.schema() + ',' +
            "\"phase\":" + JsonLine.quote(verdict.phase()) + ',' +
            "\"scenario\":" + JsonLine.quote(verdict.scenario()) + ',' +
            "\"result\":" + JsonLine.quote(verdict.result().name()) + ',' +
            "\"durationMs\":" + verdict.durationMs() + ',' +
            "\"category\":" + JsonLine.quote(verdict.category().name().replace('_', '-')) + ',' +
            "\"detail\":" + JsonLine.quote(sanitize(verdict.detail())) +
            '}';
    }

    public static Verdict decode(String line) {
        Map<String, String> values = JsonLine.parseObject(line);
        if (!values.keySet().equals(KEYS)) {
            throw new IllegalArgumentException("Verdict keys differ from schema: " + values.keySet());
        }
        try {
            return new Verdict(
                Integer.parseInt(values.get("schema")),
                nonBlank(values, "phase"),
                nonBlank(values, "scenario"),
                Verdict.Result.valueOf(values.get("result")),
                Long.parseLong(values.get("durationMs")),
                Verdict.Category.valueOf(values.get("category").replace('-', '_')),
                values.get("detail")
            );
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Malformed verdict: " + e.getMessage(), e);
        }
    }

    public static String sanitize(String detail) {
        String clean = detail.replaceAll("(?i)(access[_ -]?token|password|secret)=\\S+", "$1=<redacted>")
            .replaceAll("[\\r\\n\\t]+", " ")
            .trim();
        return clean.length() <= 1000 ? clean : clean.substring(0, 997) + "...";
    }

    private static String nonBlank(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " is blank");
        return value;
    }
}
