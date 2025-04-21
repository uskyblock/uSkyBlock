package us.talabrek.ultimateskyblock.api.model;

/**
 * PlayerPermission represents a {@link org.bukkit.permissions.Permission} handed
 * out to a {@link Player}, usually as a challenge reward.
 * The PlayerPermission is stored by uSkyBlock to be able to remove given permission
 * on island/player reset.
 */
public class PlayerPermission extends Model {
    protected final String value;

    public PlayerPermission(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
