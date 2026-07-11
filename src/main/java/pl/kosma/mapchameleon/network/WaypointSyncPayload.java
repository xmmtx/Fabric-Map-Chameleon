package pl.kosma.mapchameleon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Server → Client: Push a public waypoint to all online players.
 */
public record WaypointSyncPayload(String id, String name, int x, int y, int z,
                                   String worldId, String ownerName, boolean deleted) implements CustomPayload {
    public static final Id<WaypointSyncPayload> ID = new Id<>(Identifier.of("mapchameleon", "waypoint_sync"));
    public static final PacketCodec<RegistryByteBuf, WaypointSyncPayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.id, 64);
            buf.writeString(value.name, 256);
            buf.writeInt(value.x);
            buf.writeInt(value.y);
            buf.writeInt(value.z);
            buf.writeString(value.worldId, 128);
            buf.writeString(value.ownerName, 64);
            buf.writeBoolean(value.deleted);
        },
        buf -> new WaypointSyncPayload(
            buf.readString(64),
            buf.readString(256),
            buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readString(128),
            buf.readString(64),
            buf.readBoolean()
        )
    );
    @Override public Id<WaypointSyncPayload> getId() { return ID; }
}
