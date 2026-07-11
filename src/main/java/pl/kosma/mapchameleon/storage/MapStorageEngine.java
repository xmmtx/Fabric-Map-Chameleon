package pl.kosma.mapchameleon.storage;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Abstract storage engine for shared waypoints.
 * All methods return CompletableFuture — must be called off the main thread.
 */
public interface MapStorageEngine extends AutoCloseable {

    /** Initialize the storage (create tables/files if needed). */
    CompletableFuture<Void> initialize();

    /** Save or update a waypoint. */
    CompletableFuture<Void> saveMarker(SharedWaypoint waypoint);

    /** Delete a waypoint by its unique ID. */
    CompletableFuture<Void> deleteMarker(String id);

    /** Delete a waypoint by server, world, name, and owner. */
    CompletableFuture<Void> deleteMarker(String serverId, String worldId,
                                          String name, String ownerUuid);

    /** Get all waypoints for a specific server. */
    CompletableFuture<List<SharedWaypoint>> getMarkersForServer(String serverId);

    /** Get all waypoints for a specific server and world. */
    CompletableFuture<List<SharedWaypoint>> getMarkersForWorld(String serverId, String worldId);

    /** Get all waypoints (across all servers). */
    CompletableFuture<List<SharedWaypoint>> getAllMarkers();

    /** Close the storage engine. */
    @Override
    void close();
}
