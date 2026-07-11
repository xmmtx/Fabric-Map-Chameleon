package pl.kosma.mapchameleon.integration;

import journeymap.api.v2.common.JourneyMapPlugin;
import journeymap.api.v2.server.IServerAPI;
import journeymap.api.v2.server.IServerPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * JourneyMap IServerPlugin — detected and loaded by JourneyMap at runtime.
 * Uses only UUID-based APIs to avoid Mojang/Yarn mapping conflicts.
 */
@JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyMapServerPlugin implements IServerPlugin {
    private static final Logger LOGGER = LogManager.getLogger();
    private static IServerAPI api;

    public static IServerAPI getApi() { return api; }
    public static boolean isEnabled() { return api != null; }

    @Override
    public void initialize(IServerAPI jmServerApi) {
        api = jmServerApi;
        LOGGER.info("[JourneyMap] Server plugin initialized");
    }

    @Override
    public String getModId() {
        return "map-chameleon-server";
    }

    /** Get all known global waypoint GUIDs for cleanup purposes. */
    public static java.util.List<String> getGlobalWaypointIds() {
        if (api == null) return java.util.List.of();
        return api.getGlobalWaypoints().stream()
            .map(w -> {
                try {
                    // Use reflection to get GUID since Waypoint interface varies by version
                    return (String) w.getClass().getMethod("getId").invoke(w);
                } catch (Exception e) { return ""; }
            })
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /** Remove a global waypoint by GUID. */
    public static void removeGlobalWaypoint(String guid) {
        if (api != null) {
            try { api.deleteGlobalWaypoint(guid); } catch (Exception ignored) {}
        }
    }
}
