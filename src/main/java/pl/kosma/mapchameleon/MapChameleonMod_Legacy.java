package pl.kosma.mapchameleon;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Mod entrypoint for MC 1.20–1.20.4 (old networking API, pre-CustomPayload).
 * Functionally identical to {@link MapChameleonMod} but uses
 * {@code Identifier}-based channels instead of {@code CustomPayload}.
 *
 * @see MapChameleonMod   the 1.20.5+ entrypoint
 */
public class MapChameleonMod_Legacy implements ModInitializer {

    public static final Logger LOGGER = LogManager.getLogger();

    private static MapChameleonConfig config;

    private static final Identifier VOXELMAP_ID = new Identifier("worldinfo", "world_id");
    private static final Identifier XAEROMAP_ID = new Identifier("xaeroworldmap", "main");

    // --- Mod init ---

    @Override
    public void onInitialize() {
        config = MapChameleonConfig.load(FabricLoader.getInstance().getConfigDir());

        // VoxelMap: request-response (client sends request, server responds)
        ServerPlayNetworking.registerGlobalReceiver(VOXELMAP_ID,
            (server, player, handler, buf, responseSender) -> {
                byte[] requestBytes = new byte[buf.readableBytes()];
                buf.readBytes(requestBytes);
                server.execute(() -> sendVoxelMapResponse(player, requestBytes));
            });
    }

    // --- Hooks ---

    /**
     * Called from Mixin when a player joins or changes world (XaeroMap push).
     */
    public static void onServerWorldInfo(ServerPlayerEntity player) {
        sendXaeroMapResponse(player);
    }

    // --- Name resolution ---

    private static String resolveWorldName(MinecraftServer server) {
        switch (config.mode) {
            case custom:
                return config.name;
            case random:
                return generateRandomName(config.length);
            case level:
            default:
                return getLevelName(server);
        }
    }

    private static String generateRandomName(int digits) {
        if (digits <= 0) digits = 12;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        StringBuilder sb = new StringBuilder(digits);
        sb.append(rng.nextInt(1, 10));
        for (int i = 1; i < digits; i++) {
            sb.append(rng.nextInt(0, 10));
        }

        if (rng.nextBoolean()) {
            sb.insert(0, '-');
        }
        return sb.toString();
    }

    private static String getLevelName(MinecraftServer server) {
        try {
            return ((MinecraftDedicatedServer) server).getLevelName();
        } catch (ClassCastException e) {
            LOGGER.warn("[MapChameleon] Not a dedicated server, using fallback world name");
            return "world";
        }
    }

    // --- Response helpers (old API: Identifier + PacketByteBuf) ---

    private static void sendVoxelMapResponse(ServerPlayerEntity player, byte[] requestBytes) {
        String worldName = resolveWorldName(player.getServer());
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(requestBytes, worldName);

        LOGGER.debug("request:  {}", WorldNamePacket.byteArrayToHexString(requestBytes));
        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_VOXELMAP, worldName);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBytes(responseBytes);
        ServerPlayNetworking.send(player, VOXELMAP_ID, buf);
    }

    private static void sendXaeroMapResponse(ServerPlayerEntity player) {
        String worldName = resolveWorldName(player.getServer());
        byte[] responseBytes = WorldNamePacket.formatResponsePacket(new byte[0], worldName);

        LOGGER.debug("response: {}", WorldNamePacket.byteArrayToHexString(responseBytes));
        LOGGER.info("[MapChameleon] [{}] sending worldName: {}", WorldNamePacket.CHANNEL_NAME_XAEROMAP, worldName);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBytes(responseBytes);
        ServerPlayNetworking.send(player, XAEROMAP_ID, buf);
    }
}
