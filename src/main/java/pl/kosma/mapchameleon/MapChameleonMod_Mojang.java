package pl.kosma.mapchameleon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mod entrypoint for MC 26.x (Mojang official mappings).
 * Functionally identical to {@link MapChameleonMod} but uses
 * Mojang class names instead of Yarn.
 *
 * @see MapChameleonMod       the 1.20.5–1.21.x entrypoint (Yarn)
 * @see MapChameleonMod_Legacy the 1.20–1.20.4 entrypoint (Yarn, old API)
 */
public class MapChameleonMod_Mojang implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("MapChameleon");

    private static MapChameleonConfig config;

    // --- CustomPayload definitions for each channel ---

    public record VoxelMapPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<VoxelMapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("worldinfo", "world_id"));
        public static final StreamCodec<RegistryFriendlyByteBuf, VoxelMapPayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeBytes(value.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new VoxelMapPayload(bytes);
            }
        );
        @Override public Type<VoxelMapPayload> type() { return TYPE; }
    }

    public record XaeroMapPayload(byte[] data) implements CustomPacketPayload {
        public static final Type<XaeroMapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("xaeroworldmap", "main"));
        public static final StreamCodec<RegistryFriendlyByteBuf, XaeroMapPayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeBytes(value.data()),
            buf -> {
                byte[] bytes = new byte[buf.readableBytes()];
                buf.readBytes(bytes);
                return new XaeroMapPayload(bytes);
            }
        );
        @Override public Type<XaeroMapPayload> type() { return TYPE; }
    }

    // --- Mod init ---

    @Override
    public void onInitialize() {
        config = MapChameleonConfig.load(FabricLoader.getInstance().getConfigDir());

        // Fabric API 0.155 (MC 26.x): playC2S/playS2C renamed to serverboundPlay/clientboundPlay
        PayloadTypeRegistry.serverboundPlay().register(VoxelMapPayload.TYPE, VoxelMapPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(VoxelMapPayload.TYPE, VoxelMapPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(VoxelMapPayload.TYPE,
            (payload, context) -> sendVoxelMapResponse(context.player(), payload.data()));

        PayloadTypeRegistry.clientboundPlay().register(XaeroMapPayload.TYPE, XaeroMapPayload.CODEC);
    }

    // --- Hooks ---

    public static void onServerWorldInfo(ServerPlayer player) {
        sendXaeroMapResponse(player);
    }

    // --- Name resolution ---

    private static String resolveWorldName(MinecraftServer server) {
        switch (config.mode) {
            case custom: return config.name;
            case random: return generateRandomName(config.length);
            case level:
            default:     return getLevelName(server);
        }
    }

    private static String generateRandomName(int digits) {
        if (digits <= 0) digits = 12;
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(digits);
        sb.append(rng.nextInt(1, 10));
        for (int i = 1; i < digits; i++) sb.append(rng.nextInt(0, 10));
        if (rng.nextBoolean()) sb.insert(0, '-');
        return sb.toString();
    }

    private static String getLevelName(MinecraftServer server) {
        try {
            return ((DedicatedServer) server).getProperties().levelName;
        } catch (ClassCastException e) {
            LOGGER.warn("[MapChameleon] Not a dedicated server, using fallback world name");
            return "world";
        }
    }

    // --- Response helpers ---

    private static void sendVoxelMapResponse(ServerPlayer player, byte[] requestBytes) {
        MinecraftServer server = player.level().getServer();
        String worldName = resolveWorldName(server);
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(requestBytes, worldName);
        LOGGER.debug("request:  {}", WorldNamePacket.byteArrayToHexString(requestBytes));
        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_VOXELMAP, worldName);
        ServerPlayNetworking.send(player, new VoxelMapPayload(responseBytes));
    }

    private static void sendXaeroMapResponse(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        String worldName = resolveWorldName(server);
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(new byte[0], worldName);
        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_XAEROMAP, worldName);
        ServerPlayNetworking.send(player, new XaeroMapPayload(responseBytes));
    }
}
