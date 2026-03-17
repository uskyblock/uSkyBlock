package us.talabrek.ultimateskyblock.challenge.catalog;

import java.util.Objects;

public record ChallengeCatalogDiagnostic(Severity severity, String path, String message) {
    public ChallengeCatalogDiagnostic {
        severity = Objects.requireNonNull(severity, "severity");
        path = Objects.requireNonNull(path, "path").trim();
        message = Objects.requireNonNull(message, "message").trim();
        if (path.isEmpty()) {
            throw new IllegalArgumentException("path cannot be blank");
        }
        if (message.isEmpty()) {
            throw new IllegalArgumentException("message cannot be blank");
        }
    }

    public enum Severity {
        WARNING
    }
}
