package pl.kosma.mapchameleon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client → Server: Player deletes a previously shared waypoint.
 */
public record WaypointDeletePayload(String name, int x, int y, int z, String worldId) implements CustomPayload {
    public static final Id<WaypointDeletePayload> ID = new Id<>(Identifier.of("mapchameleon", "waypoint_delete"));
    public static final PacketCodec<RegistryByteBuf, WaypointDeletePayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.name, 256);
            buf.writeInt(value.x);
            buf.writeInt(value.y);
            buf.writeInt(value.z);
            buf.writeString(value.worldId, 128);
        },
        buf -> new WaypointDeletePayload(
            buf.readString(256),
            buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readString(128)
        )
    );
    @Override public Id<WaypointDeletePayload> getId() { return ID; }
}
