package us.talabrek.ultimateskyblock.itclient;

import net.kyori.adventure.text.Component;
import org.geysermc.mcprotocollib.auth.SessionService;
import org.geysermc.mcprotocollib.network.ClientSession;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.event.session.DisconnectedEvent;
import org.geysermc.mcprotocollib.network.event.session.SessionAdapter;
import org.geysermc.mcprotocollib.network.factory.ClientNetworkSessionFactory;
import org.geysermc.mcprotocollib.network.packet.Packet;
import org.geysermc.mcprotocollib.protocol.MinecraftConstants;
import org.geysermc.mcprotocollib.protocol.MinecraftProtocol;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.ClientboundLoginPacket;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A deliberately minimal protocol presence client. MCProtocolLib's client listener owns the
 * login/configuration transitions and keepalive replies; this class sends no gameplay packets.
 */
public final class PresenceClient {
    private PresenceClient() {
    }

    public static void main(String[] args) throws Exception {
        Configuration configuration;
        try {
            configuration = Configuration.parse(args);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java -jar uSkyBlock-itclient.jar --host 127.0.0.1 --port <port> --name <name> --ready-file <path> [--timeout-seconds 240]");
            System.exit(2);
            return;
        }

        MinecraftProtocol protocol = new MinecraftProtocol(configuration.name());
        ClientSession client = ClientNetworkSessionFactory.factory()
            .setRemoteSocketAddress(new InetSocketAddress(configuration.host(), configuration.port()))
            .setProtocol(protocol)
            .create();
        client.setFlag(MinecraftConstants.SESSION_SERVICE_KEY, new SessionService());

        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicBoolean enteredPlay = new AtomicBoolean();
        AtomicBoolean readyWritten = new AtomicBoolean();
        AtomicReference<String> disconnectReason = new AtomicReference<>("not disconnected");
        client.addListener(new SessionAdapter() {
            @Override
            public void packetReceived(Session session, Packet packet) {
                if (packet instanceof ClientboundLoginPacket && enteredPlay.compareAndSet(false, true)) {
                    try {
                        writeReady(configuration.readyFile(), configuration.name());
                        readyWritten.set(true);
                        System.out.println("USKYBLOCK-TEST CLIENT-READY name=" + configuration.name());
                    } catch (Exception e) {
                        disconnectReason.set("could not write ready marker: " + e.getMessage());
                        session.disconnect(Component.text("presence client ready marker failed"));
                    }
                }
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                disconnectReason.set(String.valueOf(event.getReason()) + (event.getCause() == null ? "" : " cause=" + event.getCause()));
                disconnected.countDown();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (client.isConnected()) client.disconnect(Component.text("presence client stopping"));
        }, "uskyblock-itclient-shutdown"));

        client.connect();
        boolean ended = disconnected.await(configuration.timeout().toSeconds(), TimeUnit.SECONDS);
        if (!ended) {
            if (client.isConnected()) client.disconnect(Component.text("presence client deadline"));
            System.err.println("USKYBLOCK-TEST CLIENT-TIMEOUT");
            System.exit(4);
        }
        System.out.println("USKYBLOCK-TEST CLIENT-DISCONNECTED reason=" + sanitize(disconnectReason.get()));
        if (!enteredPlay.get() || !readyWritten.get()) System.exit(3);
    }

    private static void writeReady(Path target, String playerName) throws Exception {
        Path absolute = target.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), "client-ready", ".tmp");
        Files.writeString(temporary, playerName + "\n", StandardCharsets.UTF_8);
        try {
            Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static String sanitize(String value) {
        return value.replaceAll("[\\r\\n\\t]+", " ").replaceAll("(?i)(token|secret|password)=\\S+", "$1=<redacted>");
    }

    record Configuration(String host, int port, String name, Path readyFile, Duration timeout) {
        static Configuration parse(String[] args) {
            String host = "127.0.0.1";
            Integer port = null;
            String name = null;
            Path readyFile = null;
            Duration timeout = Duration.ofSeconds(240);
            for (int i = 0; i < args.length; i++) {
                if (i + 1 >= args.length) throw new IllegalArgumentException(args[i] + " requires a value");
                String value = args[++i];
                switch (args[i - 1]) {
                    case "--host" -> host = value;
                    case "--port" -> port = Integer.parseInt(value);
                    case "--name" -> name = value;
                    case "--ready-file" -> readyFile = Path.of(value);
                    case "--timeout-seconds" -> timeout = Duration.ofSeconds(Long.parseLong(value));
                    default -> throw new IllegalArgumentException("Unknown option: " + args[i - 1]);
                }
            }
            if (port == null || port < 1 || port > 65535) throw new IllegalArgumentException("A valid --port is required");
            if (name == null || !name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("A valid --name is required");
            if (readyFile == null) throw new IllegalArgumentException("--ready-file is required");
            if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
            return new Configuration(host, port, name, readyFile, timeout);
        }
    }
}
