package us.talabrek.ultimateskyblock.update;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * A local stand-in for Modrinth's API and CDN: lists the versions it is given and serves their files.
 */
public final class ModrinthStandIn implements AutoCloseable {

    public static final String VERSIONS_PATH = "/v2/project/uskyblock/version";

    private final HttpServer server;
    private final List<String> versions = new CopyOnWriteArrayList<>();
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> userAgents = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile String body;
    private volatile String rateLimitReset;

    public ModrinthStandIn() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public URI api() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/");
    }

    public ModrinthClient client() {
        return new ModrinthClient(api(), "3.7.1", Clock.systemUTC());
    }

    public ModrinthClient client(Clock clock) {
        return new ModrinthClient(api(), "3.7.1", clock);
    }

    /**
     * Lists a version whose primary file is served with its correct hash.
     */
    public ModrinthStandIn version(String version, String type, Instant published, List<String> gameVersions,
                                   List<String> loaders) {
        byte[] jar = ("uSkyBlock " + version).getBytes(StandardCharsets.UTF_8);
        return version(version, type, published, gameVersions, loaders, jar, sha512(jar));
    }

    public ModrinthStandIn version(String version, String type, Instant published, List<String> gameVersions,
                                   List<String> loaders, byte[] jar, String sha512) {
        String path = "/data/KmR5sN3n/versions/" + version + "/uSkyBlock-" + version + ".jar";
        files.put(path, jar);
        versions.add("""
            {"version_number": "%s", "version_type": "%s", "date_published": "%s", "game_versions": [%s],
             "loaders": [%s], "status": "listed", "files": [{"url": "%s", "filename": "uSkyBlock-%s.jar",
             "primary": true, "size": %d, "hashes": {"sha1": "0", "sha512": "%s"}}]}
            """.formatted(version, type, published, quoted(gameVersions), quoted(loaders),
            api().resolve(path.substring(1)), version, jar.length, sha512));
        return this;
    }

    /**
     * Answers every request with this status and body instead.
     */
    public void answer(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public void rateLimited(String resetSeconds) {
        this.status = 429;
        this.body = "{\"error\": \"ratelimited\"}";
        this.rateLimitReset = resetSeconds;
    }

    public List<String> requests() {
        return new ArrayList<>(requests);
    }

    public List<String> fileRequests() {
        return requests.stream().filter(path -> path.startsWith("/data/")).toList();
    }

    public List<String> userAgents() {
        return new ArrayList<>(userAgents);
    }

    public static String sha512(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-512").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static String quoted(List<String> values) {
        return values.stream().map(value -> "\"" + value + "\"").collect(Collectors.joining(", "));
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(exchange.getRequestURI().toString());
        userAgents.add(exchange.getRequestHeaders().getFirst("User-Agent"));
        byte[] response;
        int code = status;
        if (code != 200 || body != null) {
            response = (body != null ? body : "").getBytes(StandardCharsets.UTF_8);
            if (rateLimitReset != null) {
                exchange.getResponseHeaders().add("X-Ratelimit-Reset", rateLimitReset);
            }
        } else if (exchange.getRequestURI().getPath().equals(VERSIONS_PATH)) {
            response = ("[" + String.join(",", versions) + "]").getBytes(StandardCharsets.UTF_8);
        } else if (files.containsKey(exchange.getRequestURI().getPath())) {
            response = files.get(exchange.getRequestURI().getPath());
        } else {
            code = 404;
            response = "{\"error\": \"not_found\"}".getBytes(StandardCharsets.UTF_8);
        }
        exchange.sendResponseHeaders(code, response.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(response);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
