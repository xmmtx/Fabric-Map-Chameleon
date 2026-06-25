package pl.kosma.worldnamepacket;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricMod implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    // --- CustomPayload definitions for each channel ---

    public record VoxelMapPayload(byte[] data) implements CustomPayload {
        public static final Id<VoxelMapPayload> ID = new Id<>(Identifier.of("worldinfo", "world_id"));
        public static final PacketCodec<RegistryByteBuf, VoxelMapPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new VoxelMapPayload(bytes);
            }
        );
        @Override public Id<VoxelMapPayload> getId() { return ID; }
    }

    public record XaeroMapPayload(byte[] data) implements CustomPayload {
        public static final Id<XaeroMapPayload> ID = new Id<>(Identifier.of("xaeroworldmap", "main"));
        public static final PacketCodec<RegistryByteBuf, XaeroMapPayload> CODEC = PacketCodec.of(
            (value, buf) -> buf.writeBytes(value.data),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new XaeroMapPayload(bytes);
            }
        );
        @Override public Id<XaeroMapPayload> getId() { return ID; }
    }

    // --- Mod init ---

    @Override
    public void onInitialize() {
        // VoxelMap: request-response channel (client sends request, server responds)
        PayloadTypeRegistry.playC2S().register(VoxelMapPayload.ID, VoxelMapPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoxelMapPayload.ID, VoxelMapPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VoxelMapPayload.ID,
            (payload, context) -> sendVoxelMapResponse(context.player(), payload.data()));

        // Xaero's Map: server-to-client only (proactive push)
        PayloadTypeRegistry.playS2C().register(XaeroMapPayload.ID, XaeroMapPayload.CODEC);
    }

    // --- Hooks ---

    /**
     * Called from Mixin when a player joins or changes world.
     * Xaero's Map requires the world name to be sent unprompted.
     */
    public static void onServerWorldInfo(ServerPlayerEntity player) {
        sendXaeroMapResponse(player);
    }

    // --- Response helpers ---

    private static void sendVoxelMapResponse(ServerPlayerEntity player, byte[] requestBytes) {
        String levelName = ((MinecraftDedicatedServer) player.getServerWorld().getServer()).getLevelName();
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(requestBytes, levelName);

        LOGGER.debug("request:  {}", WorldNamePacket.byteArrayToHexString(requestBytes));
        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("WorldNamePacket: [{}] sending levelName: {}", WorldNamePacket.CHANNEL_NAME_VOXELMAP, levelName);

        ServerPlayNetworking.send(player, new VoxelMapPayload(responseBytes));
    }

    private static void sendXaeroMapResponse(ServerPlayerEntity player) {
        String levelName = ((MinecraftDedicatedServer) player.getServerWorld().getServer()).getLevelName();
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(new byte[0], levelName);

        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("WorldNamePacket: [{}] sending levelName: {}", WorldNamePacket.CHANNEL_NAME_XAEROMAP, levelName);

        ServerPlayNetworking.send(player, new XaeroMapPayload(responseBytes));
    }
}
