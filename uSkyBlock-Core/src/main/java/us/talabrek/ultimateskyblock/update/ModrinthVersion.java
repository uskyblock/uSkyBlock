package us.talabrek.ultimateskyblock.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A uSkyBlock version as Modrinth lists it.
 *
 * @param version      Its {@code version_number}, e.g. 3.8.1.
 * @param type         Its {@code version_type}: release, beta or alpha.
 * @param published    Its {@code date_published}.
 * @param gameVersions The Minecraft versions it runs on.
 * @param loaders      The server platforms it runs on, e.g. paper and spigot.
 * @param file         Its primary file, NULL if it has none.
 */
public record ModrinthVersion(
    @NotNull String version,
    @NotNull String type,
    @NotNull Instant published,
    @NotNull List<String> gameVersions,
    @NotNull List<String> loaders,
    @Nullable DownloadFile file
) {
    public static final String PROJECT = "uskyblock";
    public static final URI PROJECT_PAGE = URI.create("https://modrinth.com/plugin/" + PROJECT);

    /**
     * @param sha512 The file's SHA-512 hash, in hex.
     */
    public record DownloadFile(@NotNull URI url, @NotNull String filename, @NotNull String sha512) {
    }

    public @NotNull URI page() {
        return URI.create(PROJECT_PAGE + "/version/" + version);
    }

    public @NotNull String minecraftVersions() {
        return String.join(", ", gameVersions);
    }

    /**
     * @return The platforms, e.g. Paper/Spigot.
     */
    public @NotNull String platforms() {
        return loaders.stream()
            .map(loader -> loader.isEmpty() ? loader : loader.substring(0, 1).toUpperCase(Locale.ROOT) + loader.substring(1))
            .collect(Collectors.joining("/"));
    }

    /**
     * Reads the body of Modrinth's {@code GET /v2/project/{id}/version}.
     *
     * @return The versions, skipping entries that miss a field, or NULL if the body is not a list.
     */
    public static @Nullable List<ModrinthVersion> parseList(@Nullable String body) {
        if (body == null) {
            return null;
        }
        JsonElement json;
        try {
            json = JsonParser.parseString(body);
        } catch (JsonParseException ex) {
            return null;
        }
        if (!json.isJsonArray()) {
            return null;
        }
        return json.getAsJsonArray().asList().stream()
            .map(ModrinthVersion::parse)
            .filter(Objects::nonNull)
            .toList();
    }

    private static @Nullable ModrinthVersion parse(@NotNull JsonElement element) {
        if (!element.isJsonObject()) {
            return null;
        }
        JsonObject json = element.getAsJsonObject();
        String version = string(json, "version_number");
        String type = string(json, "version_type");
        String published = string(json, "date_published");
        List<String> gameVersions = strings(json, "game_versions");
        List<String> loaders = strings(json, "loaders");
        if (version == null || type == null || published == null || gameVersions == null || loaders == null) {
            return null;
        }
        try {
            return new ModrinthVersion(version, type, Instant.parse(published), gameVersions, loaders, primaryFile(json));
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    // Modrinth treats the first file as primary when none is marked.
    private static @Nullable DownloadFile primaryFile(@NotNull JsonObject version) {
        JsonElement files = version.get("files");
        if (files == null || !files.isJsonArray()) {
            return null;
        }
        List<DownloadFile> parsed = new ArrayList<>();
        DownloadFile primary = null;
        for (JsonElement element : files.getAsJsonArray()) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            String url = string(file, "url");
            String filename = string(file, "filename");
            JsonElement hashes = file.get("hashes");
            String sha512 = hashes != null && hashes.isJsonObject() ? string(hashes.getAsJsonObject(), "sha512") : null;
            if (url == null || filename == null || sha512 == null) {
                continue;
            }
            DownloadFile download = new DownloadFile(URI.create(url), filename, sha512);
            parsed.add(download);
            JsonElement isPrimary = file.get("primary");
            if (primary == null && isPrimary != null && isPrimary.isJsonPrimitive() && isPrimary.getAsBoolean()) {
                primary = download;
            }
        }
        return primary != null ? primary : parsed.stream().findFirst().orElse(null);
    }

    private static @Nullable String string(@NotNull JsonObject json, @NotNull String name) {
        JsonElement member = json.get(name);
        return member != null && member.isJsonPrimitive() && member.getAsJsonPrimitive().isString()
            ? member.getAsString() : null;
    }

    private static @Nullable List<String> strings(@NotNull JsonObject json, @NotNull String name) {
        JsonElement member = json.get(name);
        if (member == null || !member.isJsonArray()) {
            return null;
        }
        JsonArray array = member.getAsJsonArray();
        List<String> values = new ArrayList<>();
        for (JsonElement value : array) {
            if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                values.add(value.getAsString());
            }
        }
        return List.copyOf(values);
    }
}
