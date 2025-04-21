package us.talabrek.ultimateskyblock.uuid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import us.talabrek.ultimateskyblock.uSkyBlock;

import java.util.UUID;
import java.util.function.Consumer;

public class MojangUUIDFetcher implements Runnable {
    protected final uSkyBlock plugin;
    protected final UUID uuid;
    protected final Consumer<String> callback;

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();
    private static final String NAME_URL = "https://api.mojang.com/user/profile/%s";

    public MojangUUIDFetcher(uSkyBlock plugin, UUID uuid, Consumer<String> callback) {
        this.plugin = plugin;
        this.uuid = uuid;
        this.callback = callback;
    }

    public @NotNull String getCurrentVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public void run() {
        plugin.getLog4JLogger().info("Fetching name for UUID {} from Mojang API.", uuid);
        String userAgent = "uSkyBlock-Plugin/v" + getCurrentVersion() + " (www.ultimateskyblock.net)";
        try (var httpclient = HttpClients.custom().setUserAgent(userAgent).build()) {
            int CONNECTION_TIMEOUT_MS = 10 * 1000; // Timeout in millis.
            RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_TIMEOUT_MS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                .setSocketTimeout(CONNECTION_TIMEOUT_MS)
                .build();

            HttpGet request = new HttpGet(String.format(NAME_URL, UUIDTypeAdapter.fromUUID(uuid)));
            request.setConfig(requestConfig);
            HttpResponse response = httpclient.execute(request);
            HttpEntity entity = response.getEntity();

            int status = response.getStatusLine().getStatusCode();
            if (status < 200 || status >= 300) {
                callback.accept(null);
            }

            if (entity != null) {
                JsonObject obj = gson.fromJson(EntityUtils.toString(entity), JsonObject.class);
                if (obj.has("name")) {
                    callback.accept(obj.get("name").getAsString());
                }
            }
        } catch (Exception ex) {
            plugin.getLog4JLogger().warn("Failed to create HTTP client for Mojang API.", ex);
        }

        callback.accept(null);
    }
}
