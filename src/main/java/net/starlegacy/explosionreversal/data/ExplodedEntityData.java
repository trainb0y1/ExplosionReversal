package net.starlegacy.explosionreversal.data;

import net.starlegacy.explosionreversal.nms.NMSWrapper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import javax.annotation.Nullable;
import java.io.IOException;

public class ExplodedEntityData {
    private final EntityType entityType;
    private final double x;
    private final double y;
    private final double z;
    private final float pitch;
    private final float yaw;
    private final long explodedTime;
    @Nullable
    private final byte[] nmsData;

    public ExplodedEntityData(EntityType entityType, double x, double y, double z, float pitch, float yaw,
                              long explodedTime, @Nullable byte[] nmsData) {
        this.entityType = entityType;
        this.x = x;
        this.y = y;
        this.z = z;
        this.pitch = pitch;
        this.yaw = yaw;
        this.explodedTime = explodedTime;
        this.nmsData = nmsData;
    }

    public ExplodedEntityData(Entity entity, long explosionTime) throws IOException {
        this(entity.getType(), entity.getLocation().getX(), entity.getLocation().getY(), entity.getLocation().getZ(),
                entity.getLocation().getPitch(), entity.getLocation().getYaw(),
                explosionTime, NMSWrapper.completeGetEntityData(entity));
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getPitch() {
        return pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public long getExplodedTime() {
        return explodedTime;
    }

    @Nullable
    public byte[] getNmsData() {
        return nmsData;
    }
}
