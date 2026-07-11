package pl.kosma.mapchameleon.storage;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;

/**
 * SQLite storage engine.
 * Data stored at: config/map-chameleon/waypoints.db
 */
public class SQLiteStorageEngine extends JdbcStorageEngine {
    private final Path dbPath;

    public SQLiteStorageEngine(Path configDir) {
        this.dbPath = configDir.resolve("map-chameleon").resolve("waypoints.db");
    }

    @Override
    protected String getDriverName() { return "SQLite"; }

    @Override
    protected Connection createConnection() throws Exception {
        Class.forName("org.sqlite.JDBC");
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath.toAbsolutePath());
    }
}
