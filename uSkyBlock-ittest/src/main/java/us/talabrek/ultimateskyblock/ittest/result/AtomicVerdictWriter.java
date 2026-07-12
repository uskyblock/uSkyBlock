package us.talabrek.ultimateskyblock.ittest.result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class AtomicVerdictWriter {
    private final Path target;
    private final List<Verdict> verdicts = new ArrayList<>();

    public AtomicVerdictWriter(Path target) {
        this.target = target.toAbsolutePath().normalize();
    }

    public synchronized void append(Verdict verdict) throws IOException {
        verdicts.add(verdict);
        write();
    }

    public synchronized List<Verdict> snapshot() {
        return List.copyOf(verdicts);
    }

    private void write() throws IOException {
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), target.getFileName().toString(), ".tmp");
        try {
            String content = verdicts.stream().map(VerdictCodec::encode).reduce("", (a, b) -> a + b + "\n");
            Files.writeString(temporary, content, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
