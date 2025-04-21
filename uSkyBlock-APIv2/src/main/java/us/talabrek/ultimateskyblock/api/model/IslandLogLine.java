package us.talabrek.ultimateskyblock.api.model;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

public class IslandLogLine extends Model implements Comparable<IslandLogLine> {
    protected final UUID uuid;
    protected final Instant timestamp;
    protected final String line;
    protected final String[] variables;

    public IslandLogLine(UUID uuid, Instant timestamp, String line, String[] variables) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.line = line;
        this.variables = variables;
    }

    public IslandLogLine(Instant timestamp, String line, String[] variables) {
        this.uuid = UUID.randomUUID();
        this.timestamp = timestamp;
        this.line = line;
        this.variables = variables;
    }

    public IslandLogLine(String line, String[] variables) {
        this.uuid = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.line = line;
        this.variables = variables;
    }

    public IslandLogLine(String line) {
        this.uuid = UUID.randomUUID();
        this.timestamp = Instant.now();
        this.line = line;
        this.variables = null;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getLine() {
        return line;
    }

    public String[] getVariables() {
        return variables;
    }

    @Override
    public int compareTo(@NotNull IslandLogLine o) {
        return getTimestamp().compareTo(o.getTimestamp());
    }
}
