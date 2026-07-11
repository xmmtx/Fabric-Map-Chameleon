package pl.kosma.mapchameleon.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

/**
 * Factory to create the appropriate MapStorageEngine based on config.
 */
public class StorageFactory {
    private static final Logger LOGGER = LogManager.getLogger();

    public static MapStorageEngine create(MapStorageConfig config, Path configDir) {
        String type = config.type().toLowerCase();
        LOGGER.info("[StorageFactory] Creating storage engine: {}", type);

        return switch (type) {
            case "mysql", "mariadb" -> new MySqlStorageEngine(
                config.host(), config.port(), config.name(),
                config.username(), config.password()
            );
            case "sqlite" -> new SQLiteStorageEngine(configDir);
            case "h2" -> new H2StorageEngine(configDir);
            case "file" -> new FileStorageEngine(configDir);
            default -> {
                LOGGER.warn("[StorageFactory] Unknown type '{}', falling back to File", type);
                yield new FileStorageEngine(configDir);
            }
        };
    }

    /** Simple config record for passing storage parameters. */
    public record MapStorageConfig(
        String type, String host, int port, String name,
        String username, String password
    ) {}
}
