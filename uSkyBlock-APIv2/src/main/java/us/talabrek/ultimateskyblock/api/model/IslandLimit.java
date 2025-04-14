package us.talabrek.ultimateskyblock.api.model;

public class IslandLimit extends Model {
    protected final EntityType entityType;
    protected final String entity;
    protected int limit;

    public IslandLimit(EntityType entityType, String entity, int limit) {
        this.entityType = entityType;
        this.entity = entity;
        this.limit = limit;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public String getEntity() {
        return entity;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
        setDirty(true);
    }

    public enum EntityType {
        /**
         * Limiting blocks that can be directly found in {@link org.bukkit.Material}.
         */
        BLOCK,
        /**
         * Limiting entities that extend {@link org.bukkit.entity.Entity}.
         */
        ENTITY,
        /**
         * Limithing something that we have to implement in the plugin.
         */
        PLUGIN
    }
}
