package pl.kosma.mapchameleon.integration;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import com.flowpowered.math.vector.Vector3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.kosma.mapchameleon.storage.SharedWaypoint;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Syncs shared waypoints to BlueMap as POIMarkers.
 */
public class BlueMapIntegration {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String MARKER_SET_ID = "map_chameleon";
    private static final String MARKER_SET_LABEL = "Shared Waypoints";

    private volatile BlueMapAPI api;

    public void onEnable(BlueMapAPI api) {
        this.api = api;
        LOGGER.info("[BlueMap] Integration enabled");
    }

    public void onDisable(BlueMapAPI api) {
        this.api = null;
        LOGGER.info("[BlueMap] Integration disabled");
    }

    /**
     * Add or update a POI marker on all BlueMap maps for the given world.
     */
    public CompletableFuture<Void> addMarker(SharedWaypoint wp) {
        return CompletableFuture.runAsync(() -> {
            BlueMapAPI currentApi = this.api;
            if (currentApi == null) return;

            currentApi.getWorld(wp.getWorldId()).ifPresent(world -> {
                POIMarker marker = POIMarker.builder()
                    .label(wp.getDisplayName())
                    .position(new Vector3d(wp.getX(), wp.getY(), wp.getZ()))
                    .maxDistance(10000)
                    .build();

                MarkerSet markerSet = getOrCreateMarkerSet(world.getMaps());
                markerSet.getMarkers().put(wp.getId(), marker);
                LOGGER.debug("[BlueMap] Marker added: {} at {}, {}, {}", wp.getName(), wp.getX(), wp.getY(), wp.getZ());
            });
        });
    }

    /**
     * Remove a marker from all BlueMap maps.
     */
    public CompletableFuture<Void> removeMarker(SharedWaypoint wp) {
        return CompletableFuture.runAsync(() -> {
            BlueMapAPI currentApi = this.api;
            if (currentApi == null) return;

            currentApi.getWorld(wp.getWorldId()).ifPresent(world -> {
                for (BlueMapMap map : world.getMaps()) {
                    MarkerSet set = map.getMarkerSets().get(MARKER_SET_ID);
                    if (set != null) {
                        set.getMarkers().remove(wp.getId());
                    }
                }
                LOGGER.debug("[BlueMap] Marker removed: {}", wp.getName());
            });
        });
    }

    private MarkerSet getOrCreateMarkerSet(java.util.Collection<BlueMapMap> maps) {
        for (BlueMapMap map : maps) {
            Map<String, MarkerSet> sets = map.getMarkerSets();
            if (!sets.containsKey(MARKER_SET_ID)) {
                MarkerSet newSet = MarkerSet.builder()
                    .label(MARKER_SET_LABEL)
                    .build();
                sets.put(MARKER_SET_ID, newSet);
            }
        }
        // Return a representative marker set from the first map
        return maps.iterator().next().getMarkerSets().get(MARKER_SET_ID);
    }

    public boolean isEnabled() {
        return api != null;
    }
}
