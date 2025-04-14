package us.talabrek.ultimateskyblock.api.model;

public abstract class Model {
    protected boolean dirty = false;

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean state) {
        dirty = state;
    }
}
