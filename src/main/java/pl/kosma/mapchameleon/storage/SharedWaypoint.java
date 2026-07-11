package pl.kosma.mapchameleon.storage;

import java.util.UUID;

/**
 * A shared waypoint stored in the database.
 */
public class SharedWaypoint {
    private String id;           // UUID string
    private String serverId;     // e.g. "fabric_tech_01"
    private String worldId;      // e.g. "minecraft:overworld"
    private String name;         // waypoint display name
    private int x, y, z;
    private String ownerUuid;    // player UUID who shared
    private String ownerName;    // player name
    private long createdAt;      // epoch millis
    private long updatedAt;

    public SharedWaypoint() {}

    public SharedWaypoint(String serverId, String worldId, String name,
                          int x, int y, int z, UUID ownerUuid, String ownerName) {
        this.id = UUID.randomUUID().toString();
        this.serverId = serverId;
        this.worldId = worldId;
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ownerUuid = ownerUuid.toString();
        this.ownerName = ownerName;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = this.createdAt;
    }

    // ── Getters / Setters ──

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getServerId() { return serverId; }
    public void setServerId(String serverId) { this.serverId = serverId; }

    public String getWorldId() { return worldId; }
    public void setWorldId(String worldId) { this.worldId = worldId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public int getZ() { return z; }
    public void setZ(int z) { this.z = z; }

    public String getOwnerUuid() { return ownerUuid; }
    public void setOwnerUuid(String ownerUuid) { this.ownerUuid = ownerUuid; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    /** Display name for BlueMap: "name (by player)" */
    public String getDisplayName() {
        return name + " (由 " + ownerName + " 分享)";
    }
}
