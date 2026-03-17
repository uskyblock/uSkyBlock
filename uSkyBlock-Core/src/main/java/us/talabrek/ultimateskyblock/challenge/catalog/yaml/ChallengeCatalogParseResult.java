package us.talabrek.ultimateskyblock.challenge.catalog.yaml;

import us.talabrek.ultimateskyblock.challenge.catalog.ChallengeCatalog;

import java.util.List;
import java.util.Objects;

public record ChallengeCatalogParseResult(ChallengeCatalog catalog, List<String> warnings) {
    public ChallengeCatalogParseResult {
        catalog = Objects.requireNonNull(catalog, "catalog");
        warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }
}
