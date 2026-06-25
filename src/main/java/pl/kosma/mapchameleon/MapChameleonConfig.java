package pl.kosma.mapchameleon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Configuration for Map Chameleon mod.
 * Config file: config/map-chameleon.properties
 */
public class MapChameleonConfig {

    private static final Logger LOGGER = LogManager.getLogger();

    public enum WorldNameMode {
        LEVEL_NAME,  // 服务器 level-name
        CUSTOM,      // 自定义名称 (参见 name)
        RANDOM       // 随机数字 (参见 length)
    }

    public WorldNameMode mode = WorldNameMode.LEVEL_NAME;
    public String name = "My World";
    public int length = 12;

    // --- Load / Save ---

    public static MapChameleonConfig load(Path configDir) {
        Path configFile = configDir.resolve("map-chameleon.properties");
        MapChameleonConfig config = new MapChameleonConfig();

        if (Files.exists(configFile)) {
            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(configFile)) {
                props.load(reader);
            } catch (IOException e) {
                LOGGER.error("[MapChameleon] Failed to read config, using defaults", e);
                return config;
            }
            try {
                config.mode = WorldNameMode.valueOf(
                    props.getProperty("mode", "LEVEL_NAME").trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("[MapChameleon] Invalid mode '{}', using LEVEL_NAME",
                    props.getProperty("mode"));
                config.mode = WorldNameMode.LEVEL_NAME;
            }
            config.name = props.getProperty("name", "My World").trim();
            try {
                config.length = Integer.parseInt(
                    props.getProperty("length", "12").trim());
            } catch (NumberFormatException e) {
                LOGGER.warn("[MapChameleon] Invalid length, using 12");
                config.length = 12;
            }
            LOGGER.info("[MapChameleon] Loaded config: mode={}, name={}, length={}",
                config.mode, config.name, config.length);
            return config;
        }

        // Config file doesn't exist — save defaults
        config.save(configDir);
        return config;
    }

    public void save(Path configDir) {
        Path configFile = configDir.resolve("map-chameleon.properties");
        try {
            Files.createDirectories(configDir);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                writer.write("# Map Chameleon Configuration\n");
                writer.write("# 修改后保存，重启服务器即可\n");
                writer.write("# mode: LEVEL_NAME | CUSTOM | RANDOM\n");
                writer.write("mode=" + mode.name() + "\n");
                writer.write("\n");
                writer.write("# name: 自定义世界名称 (mode=CUSTOM 时生效)\n");
                writer.write("name=" + name + "\n");
                writer.write("\n");
                writer.write("# length: 随机数字位数 (mode=RANDOM 时生效)\n");
                writer.write("length=" + length + "\n");
            }
            LOGGER.info("[MapChameleon] Saved default config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("[MapChameleon] Failed to save config", e);
        }
    }
}
