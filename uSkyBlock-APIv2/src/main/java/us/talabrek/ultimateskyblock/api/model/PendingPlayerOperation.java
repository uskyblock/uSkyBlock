package us.talabrek.ultimateskyblock.api.model;

public class PendingPlayerOperation extends Model {
    protected final OperationType operationType;
    protected final String value;

    public PendingPlayerOperation(OperationType operationType, String value) {
        this.operationType = operationType;
        this.value = value;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public String getValue() {
        return value;
    }

    public enum OperationType {
        COMMAND,
        PERMISSION
    }
}
