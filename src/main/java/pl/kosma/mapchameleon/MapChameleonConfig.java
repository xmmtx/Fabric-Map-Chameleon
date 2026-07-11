package pl.kosma.mapchameleon;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Unified YAML configuration for Map-Chameleon Server.
 * Config file: config/map-chameleon/config.yml
 */
public class MapChameleonConfig {
    private static final Logger LOGGER = LogManager.getLogger();

    // ── Database ──
    public String databaseType = "File";
    public String databaseHost = "127.0.0.1";
    public int databasePort = 3306;
    public String databaseName = "map_chameleon";
    public String databaseUsername = "root";
    public String databasePassword = "";

    // ── Server Settings ──
    public String serverId = "default";

    // ── Features ──
    public boolean voxelMapEnabled = true;
    public boolean xaeroMapEnabled = true;
    public boolean journeyMapEnabled = true;
    public boolean blueMapEnabled = true;

    // ── Waypoint ──
    public int waypointShareCooldownSeconds = 3;

    // ── Internals ──
    private final Path configFile;

    public MapChameleonConfig(Path configDir) {
        this.configFile = configDir.resolve("map-chameleon").resolve("config.yml");
    }

    public static MapChameleonConfig load(Path configDir) {
        MapChameleonConfig config = new MapChameleonConfig(configDir);

        if (Files.exists(config.configFile)) {
            try (Reader reader = Files.newBufferedReader(config.configFile)) {
                Yaml yaml = new Yaml();
                @SuppressWarnings("unchecked")
                Map<String, Object> data = yaml.load(reader);
                if (data != null) {
                    parseSection(config, data);
                }
            } catch (Exception e) {
                LOGGER.error("[MapChameleon] Failed to load config, using defaults", e);
            }
        } else {
            config.save();
        }

        LOGGER.info("[MapChameleon] Config loaded: serverId={}, database={}",
                config.serverId, config.databaseType);
        return config;
    }

    @SuppressWarnings("unchecked")
    private static void parseSection(MapChameleonConfig cfg, Map<String, Object> data) {
        Map<String, Object> db = (Map<String, Object>) data.getOrDefault("database", Map.of());
        cfg.databaseType = String.valueOf(db.getOrDefault("type", cfg.databaseType));
        cfg.databaseHost = String.valueOf(db.getOrDefault("host", cfg.databaseHost));
        cfg.databasePort = toInt(db.getOrDefault("port", cfg.databasePort));
        cfg.databaseName = String.valueOf(db.getOrDefault("name", cfg.databaseName));
        cfg.databaseUsername = String.valueOf(db.getOrDefault("username", cfg.databaseUsername));
        cfg.databasePassword = String.valueOf(db.getOrDefault("password", cfg.databasePassword));

        Map<String, Object> srv = (Map<String, Object>) data.getOrDefault("server_settings", Map.of());
        cfg.serverId = String.valueOf(srv.getOrDefault("server_id", cfg.serverId));

        Map<String, Object> feat = (Map<String, Object>) data.getOrDefault("features", Map.of());
        cfg.voxelMapEnabled = toBool(feat.getOrDefault("voxelmap", cfg.voxelMapEnabled));
        cfg.xaeroMapEnabled = toBool(feat.getOrDefault("xaeromap", cfg.xaeroMapEnabled));
        cfg.journeyMapEnabled = toBool(feat.getOrDefault("journeymap", cfg.journeyMapEnabled));
        cfg.blueMapEnabled = toBool(feat.getOrDefault("bluemap", cfg.blueMapEnabled));

        Map<String, Object> wp = (Map<String, Object>) data.getOrDefault("waypoint", Map.of());
        cfg.waypointShareCooldownSeconds = toInt(wp.getOrDefault("share_cooldown_seconds", cfg.waypointShareCooldownSeconds));
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            DumperOptions opts = new DumperOptions();
            opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            opts.setPrettyFlow(true);
            Yaml yaml = new Yaml(opts);

            Map<String, Object> data = new LinkedHashMap<>();
            Map<String, Object> db = new LinkedHashMap<>();
            db.put("type", databaseType);
            db.put("host", databaseHost);
            db.put("port", databasePort);
            db.put("name", databaseName);
            db.put("username", databaseUsername);
            db.put("password", databasePassword);
            data.put("database", db);

            Map<String, Object> srv = new LinkedHashMap<>();
            srv.put("server_id", serverId);
            data.put("server_settings", srv);

            Map<String, Object> feat = new LinkedHashMap<>();
            feat.put("voxelmap", voxelMapEnabled);
            feat.put("xaeromap", xaeroMapEnabled);
            feat.put("journeymap", journeyMapEnabled);
            feat.put("bluemap", blueMapEnabled);
            data.put("features", feat);

            Map<String, Object> wp = new LinkedHashMap<>();
            wp.put("share_cooldown_seconds", waypointShareCooldownSeconds);
            data.put("waypoint", wp);

            try (Writer writer = Files.newBufferedWriter(configFile)) {
                writer.write("# Map-Chameleon Server Configuration\n");
                yaml.dump(data, writer);
            }
            LOGGER.info("[MapChameleon] Saved config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("[MapChameleon] Failed to save config", e);
        }
    }

    private static int toInt(Object val) {
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(String.valueOf(val)); } catch (Exception e) { return 0; }
    }

    private static boolean toBool(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        return Boolean.parseBoolean(String.valueOf(val));
    }
}
