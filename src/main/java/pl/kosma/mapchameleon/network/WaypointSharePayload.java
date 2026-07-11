package pl.kosma.mapchameleon.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Client → Server: Player shares a waypoint.
 */
public record WaypointSharePayload(String name, int x, int y, int z, String worldId) implements CustomPayload {
    public static final Id<WaypointSharePayload> ID = new Id<>(Identifier.of("mapchameleon", "waypoint_share"));
    public static final PacketCodec<RegistryByteBuf, WaypointSharePayload> CODEC = PacketCodec.of(
        (value, buf) -> {
            buf.writeString(value.name, 256);
            buf.writeInt(value.x);
            buf.writeInt(value.y);
            buf.writeInt(value.z);
            buf.writeString(value.worldId, 128);
        },
        buf -> new WaypointSharePayload(
            buf.readString(256),
            buf.readInt(), buf.readInt(), buf.readInt(),
            buf.readString(128)
        )
    );
    @Override public Id<WaypointSharePayload> getId() { return ID; }
}
