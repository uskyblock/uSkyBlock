package us.talabrek.ultimateskyblock.challenge.catalog.yaml;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalogDiagnostic;
import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;

import java.util.List;
import java.util.Objects;

public record ChallengeCatalogParseResult(ChallengeCatalog catalog, List<ChallengeCatalogDiagnostic> diagnostics) {
    public ChallengeCatalogParseResult {
        catalog = Objects.requireNonNull(catalog, "catalog");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public List<String> warnings() {
        return diagnostics.stream()
            .filter(diagnostic -> diagnostic.severity() == ChallengeCatalogDiagnostic.Severity.WARNING)
            .map(diagnostic -> diagnostic.path() + ": " + diagnostic.message())
            .toList();
    }
}
