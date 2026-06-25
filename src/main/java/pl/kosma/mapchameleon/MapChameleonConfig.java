package pl.kosma.mapchameleon;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for Map Chameleon mod.
 * Config file: config/map-chameleon.json
 */
public class MapChameleonConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public enum WorldNameMode {
        /** Use the server's level-name from server.properties */
        LEVEL_NAME,
        /** Use the custom name from {@link #customWorldName} */
        CUSTOM,
        /** Generate a random 12-digit integer on each world change */
        RANDOM
    }

    @SerializedName("worldNameMode")
    public WorldNameMode worldNameMode = WorldNameMode.LEVEL_NAME;

    @SerializedName("customWorldName")
    public String customWorldName = "My World";

    @SerializedName("randomNameLength")
    public int randomNameLength = 12;

    // --- Load / Save ---

    public static MapChameleonConfig load(Path configDir) {
        Path configFile = configDir.resolve("map-chameleon.json");
        MapChameleonConfig config = new MapChameleonConfig();

        if (Files.exists(configFile)) {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                config = GSON.fromJson(reader, MapChameleonConfig.class);
                MapChameleonMod.LOGGER.info("[MapChameleon] Loaded config: mode={}, customName='{}', randomLen={}",
                    config.worldNameMode, config.customWorldName, config.randomNameLength);
                return config;
            } catch (IOException e) {
                MapChameleonMod.LOGGER.error("[MapChameleon] Failed to load config, using defaults", e);
            }
        }

        // Config file doesn't exist — save defaults
        config.save(configDir);
        return config;
    }

    public void save(Path configDir) {
        Path configFile = configDir.resolve("map-chameleon.json");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(this, writer);
            }
            MapChameleonMod.LOGGER.info("[MapChameleon] Saved default config to {}", configFile);
        } catch (IOException e) {
            MapChameleonMod.LOGGER.error("[MapChameleon] Failed to save config", e);
        }
    }
}
