package pl.kosma.mapchameleon.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * File-based storage engine using JSON.
 * Data stored at: config/map-chameleon/waypoints.json
 */
public class FileStorageEngine implements MapStorageEngine {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type LIST_TYPE = new TypeToken<List<SharedWaypoint>>(){}.getType();

    private final Path dataFile;
    private final Object lock = new Object();
    private List<SharedWaypoint> cache;

    public FileStorageEngine(Path configDir) {
        this.dataFile = configDir.resolve("map-chameleon").resolve("waypoints.json");
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            synchronized (lock) {
                try {
                    Files.createDirectories(dataFile.getParent());
                    if (Files.exists(dataFile)) {
                        try (Reader reader = Files.newBufferedReader(dataFile)) {
                            cache = GSON.fromJson(reader, LIST_TYPE);
                        }
                    }
                    if (cache == null) cache = new ArrayList<>();
                    LOGGER.info("[FileStorage] Initialized, {} waypoints loaded", cache.size());
                } catch (IOException e) {
                    LOGGER.error("[FileStorage] Init failed", e);
                    cache = new ArrayList<>();
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveMarker(SharedWaypoint waypoint) {
        return CompletableFuture.runAsync(() -> {
            synchronized (lock) {
                cache.removeIf(w -> w.getId().equals(waypoint.getId()));
                cache.add(waypoint);
                flush();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteMarker(String id) {
        return CompletableFuture.runAsync(() -> {
            synchronized (lock) {
                cache.removeIf(w -> w.getId().equals(id));
                flush();
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteMarker(String serverId, String worldId,
                                                 String name, String ownerUuid) {
        return CompletableFuture.runAsync(() -> {
            synchronized (lock) {
                cache.removeIf(w ->
                    w.getServerId().equals(serverId) &&
                    w.getWorldId().equals(worldId) &&
                    w.getName().equals(name) &&
                    w.getOwnerUuid().equals(ownerUuid)
                );
                flush();
            }
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getMarkersForServer(String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                return cache.stream()
                    .filter(w -> w.getServerId().equals(serverId))
                    .collect(Collectors.toList());
            }
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getMarkersForWorld(String serverId, String worldId) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                return cache.stream()
                    .filter(w -> w.getServerId().equals(serverId) && w.getWorldId().equals(worldId))
                    .collect(Collectors.toList());
            }
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getAllMarkers() {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (lock) {
                return new ArrayList<>(cache);
            }
        });
    }

    @Override
    public void close() {
        flush();
    }

    private void flush() {
        try (Writer writer = Files.newBufferedWriter(dataFile)) {
            GSON.toJson(cache, writer);
        } catch (IOException e) {
            LOGGER.error("[FileStorage] Flush failed", e);
        }
    }
}
