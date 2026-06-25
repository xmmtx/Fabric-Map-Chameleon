package pl.kosma.mapchameleon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

public class MapChameleonMod implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();

    private static MapChameleonConfig config;

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
        // Load config
        config = MapChameleonConfig.load(FabricLoader.getInstance().getConfigDir());

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

    // --- Name resolution ---

    /**
     * Resolve the effective world name according to the current config mode.
     */
    private static String resolveWorldName(MinecraftServer server) {
        switch (config.mode) {
            case CUSTOM:
                return config.name;

            case RANDOM:
                return generateRandomName(config.length);

            case LEVEL_NAME:
            default:
                return getLevelName(server);
        }
    }

    /**
     * Generate a random integer string of the given digit length,
     * with a random sign (positive or negative).
     */
    private static String generateRandomName(int digits) {
        if (digits <= 0) digits = 12;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Build the absolute value as a string of 'digits' decimal digits
        StringBuilder sb = new StringBuilder(digits);
        // First digit: 1-9 (no leading zero)
        sb.append(rng.nextInt(1, 10));
        for (int i = 1; i < digits; i++) {
            sb.append(rng.nextInt(0, 10));
        }

        // Random sign: 50% positive, 50% negative
        if (rng.nextBoolean()) {
            sb.insert(0, '-');
        }

        return sb.toString();
    }

    /**
     * Safely extract the level-name from the server.
     * Falls back to "world" on class-cast (singleplayer / LAN integrated server).
     */
    private static String getLevelName(MinecraftServer server) {
        try {
            return ((MinecraftDedicatedServer) server).getLevelName();
        } catch (ClassCastException e) {
            LOGGER.warn("[MapChameleon] Not a dedicated server, using fallback world name");
            return "world";
        }
    }

    // --- Response helpers ---

    private static void sendVoxelMapResponse(ServerPlayerEntity player, byte[] requestBytes) {
        String worldName = resolveWorldName(player.getServer());
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(requestBytes, worldName);

        LOGGER.debug("request:  {}", WorldNamePacket.byteArrayToHexString(requestBytes));
        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_VOXELMAP, worldName);

        ServerPlayNetworking.send(player, new VoxelMapPayload(responseBytes));
    }

    private static void sendXaeroMapResponse(ServerPlayerEntity player) {
        String worldName = resolveWorldName(player.getServer());
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(new byte[0], worldName);

        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_XAEROMAP, worldName);

        ServerPlayNetworking.send(player, new XaeroMapPayload(responseBytes));
    }
}
