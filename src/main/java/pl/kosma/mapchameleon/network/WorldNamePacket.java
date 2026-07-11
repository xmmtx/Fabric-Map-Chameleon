package pl.kosma.mapchameleon.network;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Packet formatting for VoxelMap, XaeroMap, JourneyMap world-name sync.
 *
 * Protocol logic adapted from MapModCompanion (turikhay, MIT License):
 *   https://github.com/turikhay/MapModCompanion
 *
 * VoxelMap/JourneyMap request variants handled:
 *   [0,0]              → Forge 1.12.2 (padding=1, no magic)
 *   [0,42,0]           → Forge 1.13.2-1.16.3 (padding=1, no magic)
 *   [0,42,0]           → Forge 1.16.4+/Fabric 1.19.3+/JM 1.16.5+ (padding=1, magic)
 *   [0,0,0,42]         → Fabric 1.14.4-1.19.x VoxelMap bug (padding=0, magic)
 *
 * Response format: [padding_zeros, magic_42, length_byte, UTF-8_name]
 *
 * XaeroMap format: [0x00, int32(worldId)]
 */
public class WorldNamePacket {
    static final byte MAGIC = 42;
    public static final String CHANNEL_VOXELMAP = "worldinfo:world_id";
    public static final String CHANNEL_XAERO_MINIMAP = "xaerominimap:main";
    public static final String CHANNEL_XAERO_WORLDMAP = "xaeroworldmap:main";

    // ── VoxelMap / JourneyMap ──

    /**
     * Parse a VoxelMap/JourneyMap client request and generate the response.
     * Handles all known format variants per MapModCompanion's PrefixedIdRequest.parse().
     */
    public static byte[] formatWorldIdResponse(byte[] requestBytes, String serverId) {
        ParsedRequest req = parseRequest(requestBytes);
        return buildResponse(req.padding, req.usesMagic, serverId);
    }

    /** Result of parsing a VoxelMap/JourneyMap request packet. */
    private record ParsedRequest(int padding, boolean usesMagic) {}

    /**
     * Parses a VoxelMap/JourneyMap world_id request.
     * Based on MapModCompanion's PrefixedIdRequest.parse().
     */
    private static ParsedRequest parseRequest(byte[] payload) {
        int padding = -1;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int c;
            try {
                do {
                    padding++;
                    c = in.readByte();
                } while (c == 0);
                if (c != MAGIC) {
                    // No magic byte → VoxelMap Forge 1.13.2-1.16.3
                    return new ParsedRequest(1, false);
                }
            } catch (EOFException e) {
                if (padding == 2) {
                    // VoxelMap Forge 1.12.2: [0, 0]
                    return new ParsedRequest(1, false);
                }
                // Ambiguous zero-filled — treat as modern
                return new ParsedRequest(1, true);
            }
        } catch (IOException e) {
            // Fallback
            return new ParsedRequest(1, true);
        }

        return switch (padding) {
            case 0 -> new ParsedRequest(0, true);   // VoxelMap Fabric bug: [0,0,0,42]
            case 1 -> new ParsedRequest(1, true);   // Standard modern
            case 3 -> new ParsedRequest(0, true);   // LiteLoader / old Fabric
            default -> new ParsedRequest(1, true);
        };
    }

    /** Build response: [padding*0x00, 0x42, length_byte, UTF-8_bytes] */
    private static byte[] buildResponse(int padding, boolean usesMagic, String name) {
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < padding; i++) out.write(0);
        if (usesMagic) out.write(MAGIC);
        out.write(nameBytes.length);
        out.write(nameBytes, 0, nameBytes.length);
        return out.toByteArray();
    }

    // ── XaeroMap ──

    /**
     * Format XaeroMap LevelMapProperties packet: [0x00, int32(worldId)].
     * Based on MapModCompanion's LevelMapProperties.Serializer.
     */
    public static byte[] formatXaeroResponse(int worldId) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(out)) {
            dos.writeByte(0);
            dos.writeInt(worldId);
        } catch (IOException ignored) {}
        return out.toByteArray();
    }

    // ── Debug ──

    public static String byteArrayToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02x ", b & 0xff));
        return sb.toString().trim();
    }
}
