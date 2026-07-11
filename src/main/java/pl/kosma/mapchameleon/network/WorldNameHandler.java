package pl.kosma.mapchameleon.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles world-name synchronization for VoxelMap, XaeroMap, and JourneyMap.
 */
public class WorldNameHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    // ── VoxelMap / JourneyMap Payload ──
    public record VoxelWorldIdPayload(byte[] data) implements CustomPayload {
        public static final Id<VoxelWorldIdPayload> ID = new Id<>(Identifier.of("worldinfo", "world_id"));
        public static final PacketCodec<RegistryByteBuf, VoxelWorldIdPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data),
            buf -> { byte[] b = new byte[buf.readableBytes()]; buf.readBytes(b); return new VoxelWorldIdPayload(b); }
        );
        @Override public Id<VoxelWorldIdPayload> getId() { return ID; }
    }

    // ── XaeroMap Minimap Payload ──
    public record XaeroMinimapPayload(byte[] data) implements CustomPayload {
        public static final Id<XaeroMinimapPayload> ID = new Id<>(Identifier.of("xaerominimap", "main"));
        public static final PacketCodec<RegistryByteBuf, XaeroMinimapPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data),
            buf -> { byte[] b = new byte[buf.readableBytes()]; buf.readBytes(b); return new XaeroMinimapPayload(b); }
        );
        @Override public Id<XaeroMinimapPayload> getId() { return ID; }
    }

    // ── XaeroMap WorldMap Payload ──
    public record XaeroWorldMapPayload(byte[] data) implements CustomPayload {
        public static final Id<XaeroWorldMapPayload> ID = new Id<>(Identifier.of("xaeroworldmap", "main"));
        public static final PacketCodec<RegistryByteBuf, XaeroWorldMapPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data),
            buf -> { byte[] b = new byte[buf.readableBytes()]; buf.readBytes(b); return new XaeroWorldMapPayload(b); }
        );
        @Override public Id<XaeroWorldMapPayload> getId() { return ID; }
    }

    // ── Registration ──

    public static void register(String serverId) {
        // VoxelMap / JourneyMap: request-response channel
        PayloadTypeRegistry.playC2S().register(VoxelWorldIdPayload.ID, VoxelWorldIdPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoxelWorldIdPayload.ID, VoxelWorldIdPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VoxelWorldIdPayload.ID,
            (payload, context) -> {
                byte[] response = WorldNamePacket.formatWorldIdResponse(payload.data(), serverId);
                LOGGER.debug("[WorldName] VoxelMap response: {}", WorldNamePacket.byteArrayToHex(response));
                ServerPlayNetworking.send(context.player(), new VoxelWorldIdPayload(response));
            });

        // XaeroMap: server-to-client push only
        PayloadTypeRegistry.playS2C().register(XaeroMinimapPayload.ID, XaeroMinimapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(XaeroWorldMapPayload.ID, XaeroWorldMapPayload.CODEC);
    }

    /** Send XaeroMap world name to a player (called on join/respawn/world-change). */
    public static void sendXaeroWorldName(ServerPlayerEntity player, int worldId) {
        byte[] data = WorldNamePacket.formatXaeroResponse(worldId);
        LOGGER.debug("[WorldName] XaeroMap -> {} : {}", player.getName().getString(), WorldNamePacket.byteArrayToHex(data));
        ServerPlayNetworking.send(player, new XaeroMinimapPayload(data));
        ServerPlayNetworking.send(player, new XaeroWorldMapPayload(data));
    }
}
