package pl.kosma.mapchameleon.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * H2 embedded database storage engine.
 * Data stored at: config/map-chameleon/h2_waypoints
 */
public class H2StorageEngine extends JdbcStorageEngine {
    private final Path dbPath;

    public H2StorageEngine(Path configDir) {
        this.dbPath = configDir.resolve("map-chameleon").resolve("h2_waypoints");
    }

    @Override
    protected String getDriverName() { return "H2"; }

    @Override
    protected Connection createConnection() throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(
            "jdbc:h2:file:" + dbPath.toAbsolutePath() + ";DB_CLOSE_DELAY=-1;MODE=MySQL"
        );
    }
}
