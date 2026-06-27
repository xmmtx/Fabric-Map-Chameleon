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
        level,  // 服务器 level-name
        custom,      // 自定义名称 (参见 name)
        random       // 随机数字 (参见 length)
    }

    public WorldNameMode mode = WorldNameMode.level;
    public String name = "world";
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
                    props.getProperty("mode", "level").trim());
            } catch (IllegalArgumentException e) {
                LOGGER.warn("[MapChameleon] Invalid mode '{}', using level",
                    props.getProperty("mode"));
                config.mode = WorldNameMode.level;
            }
            config.name = props.getProperty("name", "world").trim();
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
                writer.write("# Edit, save, then restart the server. | 修改后保存，重启服务器即可。\n");
                writer.write("# mode: level | custom | random\n");
                writer.write("#   level  = use server.properties level-name | 使用服务器 level-name\n");
                writer.write("#   custom = use the name below            | 使用下方 name 的值\n");
                writer.write("#   random = random digits (see length)    | 随机数字 (参见 length)\n");
                writer.write("mode=" + mode.name() + "\n");
                writer.write("\n");
                writer.write("# name: custom world name (used when mode=custom)\n");
                writer.write("#       自定义世界名称 (mode=custom 时生效)\n");
                writer.write("name=" + name + "\n");
                writer.write("\n");
                writer.write("# length: number of digits for random name (used when mode=random)\n");
                writer.write("#         随机数字位数 (mode=random 时生效)\n");
                writer.write("length=" + length + "\n");
            }
            LOGGER.info("[MapChameleon] Saved default config to {}", configFile);
        } catch (IOException e) {
            LOGGER.error("[MapChameleon] Failed to save config", e);
        }
    }
}
