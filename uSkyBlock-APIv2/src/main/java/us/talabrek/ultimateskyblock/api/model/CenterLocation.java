package us.talabrek.ultimateskyblock.api.model;

public class CenterLocation extends Model {
    protected int x;
    protected int z;

    public CenterLocation(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }
}
