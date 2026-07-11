package pl.kosma.mapchameleon;

import de.bluecolored.bluemap.api.BlueMapAPI;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.kosma.mapchameleon.integration.BlueMapIntegration;
import pl.kosma.mapchameleon.integration.JourneyMapServerPlugin;
import pl.kosma.mapchameleon.network.*;
import pl.kosma.mapchameleon.storage.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * Map-Chameleon Server — main entry point.
 */
public class MapChameleonServer implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    public static final String MOD_ID = "map-chameleon-server";

    private static MapChameleonConfig config;
    private static MapStorageEngine storage;
    private static BlueMapIntegration blueMap;
    private static final ExecutorService ASYNC = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "MapChameleon-Worker");
        t.setDaemon(true);
        return t;
    });

    // Track recent shares per player for cooldown
    private static final Map<UUID, Long> shareCooldowns = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        var configDir = FabricLoader.getInstance().getConfigDir();
        config = MapChameleonConfig.load(configDir);

        // ── Storage ──
        var storageConfig = new StorageFactory.MapStorageConfig(
            config.databaseType, config.databaseHost, config.databasePort,
            config.databaseName, config.databaseUsername, config.databasePassword
        );
        storage = StorageFactory.create(storageConfig, configDir);
        storage.initialize().join();

        // ── BlueMap ──
        blueMap = new BlueMapIntegration();
        if (config.blueMapEnabled && FabricLoader.getInstance().isModLoaded("bluemap")) {
            BlueMapAPI.onEnable(blueMap::onEnable);
            BlueMapAPI.onDisable(blueMap::onDisable);
        }

        // ── Network: World Names ──
        WorldNameHandler.register(config.serverId);

        // ── Network: Waypoint Sync ──
        PayloadTypeRegistry.playC2S().register(WaypointSharePayload.ID, WaypointSharePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WaypointDeletePayload.ID, WaypointDeletePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(WaypointSyncPayload.ID, WaypointSyncPayload.CODEC);

        // Handle waypoint share from client
        ServerPlayNetworking.registerGlobalReceiver(WaypointSharePayload.ID,
            (payload, context) -> handleWaypointShare(context.player(), payload));

        // Handle waypoint delete from client
        ServerPlayNetworking.registerGlobalReceiver(WaypointDeletePayload.ID,
            (payload, context) -> handleWaypointDelete(context.player(), payload));

        // ── Events ──
        ServerLifecycleEvents.SERVER_STARTED.register(server -> ServerHolder.set(server));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            storage.close();
            ASYNC.shutdown();
        });

        // On player join: send XaeroMap world name + existing public waypoints
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            // Send XaeroMap world name
            int worldId = Math.abs(player.getServerWorld().getRegistryKey().getValue().hashCode());
            WorldNameHandler.sendXaeroWorldName(player, worldId);

            // Push existing public waypoints to the newly joined player
            pushWaypointsToPlayer(player, server);
        });

        // On world change: re-send XaeroMap world name
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {}); // handled by mixin

        LOGGER.info("[MapChameleon] Server mod initialized. serverId={}, storage={}",
            config.serverId, config.databaseType);
    }

    // ── Waypoint handling ──

    private void handleWaypointShare(ServerPlayerEntity player, WaypointSharePayload payload) {
        if (!checkCooldown(player)) return;

        String worldId = payload.worldId();
        if (worldId.isEmpty()) {
            worldId = player.getServerWorld().getRegistryKey().getValue().toString();
        }

        SharedWaypoint wp = new SharedWaypoint(
            config.serverId, worldId, payload.name(),
            payload.x(), payload.y(), payload.z(),
            player.getUuid(), player.getName().getString()
        );

        CompletableFuture.runAsync(() -> {
            // 1. Save to storage
            storage.saveMarker(wp).join();

            // 2. Sync to BlueMap
            if (blueMap.isEnabled()) {
                blueMap.addMarker(wp).join();
            }

            // 3. Push to all online players
            broadcastWaypoint(wp, false);

            LOGGER.info("[MapChameleon] Waypoint shared: {} by {} at {}, {}, {}",
                wp.getName(), wp.getOwnerName(), wp.getX(), wp.getY(), wp.getZ());
        }, ASYNC);
    }

    private void handleWaypointDelete(ServerPlayerEntity player, WaypointDeletePayload payload) {
        CompletableFuture.runAsync(() -> {
            String worldId = payload.worldId().isEmpty()
                ? player.getServerWorld().getRegistryKey().getValue().toString()
                : payload.worldId();

            // Find and delete
            storage.getMarkersForWorld(config.serverId, worldId).thenAccept(markers -> {
                for (SharedWaypoint wp : markers) {
                    if (wp.getName().equals(payload.name()) &&
                        wp.getX() == payload.x() &&
                        wp.getY() == payload.y() &&
                        wp.getZ() == payload.z() &&
                        wp.getOwnerUuid().equals(player.getUuid().toString())) {

                        storage.deleteMarker(wp.getId()).join();

                        // Remove from BlueMap
                        if (blueMap.isEnabled()) {
                            blueMap.removeMarker(wp).join();
                        }

                        // Broadcast deletion
                        wp.setUpdatedAt(System.currentTimeMillis());
                        broadcastWaypoint(wp, true);

                        LOGGER.info("[MapChameleon] Waypoint deleted: {} by {}",
                            wp.getName(), wp.getOwnerName());
                        break;
                    }
                }
            });
        }, ASYNC);
    }

    // ── Push / Broadcast ──

    private void pushWaypointsToPlayer(ServerPlayerEntity player, MinecraftServer server) {
        CompletableFuture.runAsync(() -> {
            storage.getMarkersForWorld(config.serverId,
                player.getServerWorld().getRegistryKey().getValue().toString()
            ).thenAccept(markers -> {
                for (SharedWaypoint wp : markers) {
                    WaypointSyncPayload sync = new WaypointSyncPayload(
                        wp.getId(), wp.getName(), wp.getX(), wp.getY(), wp.getZ(),
                        wp.getWorldId(), wp.getOwnerName(), false
                    );
                    server.execute(() -> ServerPlayNetworking.send(player, sync));
                }
            });
        }, ASYNC);
    }

    private void broadcastWaypoint(SharedWaypoint wp, boolean deleted) {
        WaypointSyncPayload sync = new WaypointSyncPayload(
            wp.getId(), wp.getName(), wp.getX(), wp.getY(), wp.getZ(),
            wp.getWorldId(), wp.getOwnerName(), deleted
        );

        // Get server from any online player (hacky but works)
        for (ServerPlayerEntity player : getServerPlayers()) {
            ServerPlayNetworking.send(player, sync);
        }
    }

    private Collection<ServerPlayerEntity> getServerPlayers() {
        // Access via static holder
        return ServerHolder.getServer().getPlayerManager().getPlayerList();
    }

    // ── Cooldown ──

    private boolean checkCooldown(ServerPlayerEntity player) {
        long now = System.currentTimeMillis();
        Long last = shareCooldowns.get(player.getUuid());
        if (last != null && (now - last) < config.waypointShareCooldownSeconds * 1000L) {
            return false;
        }
        shareCooldowns.put(player.getUuid(), now);
        return true;
    }

    // ── Server holder for getting player list ──
    public static class ServerHolder {
        private static MinecraftServer server;
        public static void set(MinecraftServer s) { server = s; }
        public static MinecraftServer getServer() { return server; }
    }
}
