package pl.kosma.mapchameleon.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Base class for JDBC-based storage engines (MySQL, SQLite, H2).
 * Uses a single table: shared_waypoints
 */
public abstract class JdbcStorageEngine implements MapStorageEngine {
    private static final Logger LOGGER = LogManager.getLogger();

    protected Connection connection;

    protected abstract Connection createConnection() throws Exception;
    protected abstract String getDriverName();

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.runAsync(() -> {
            try {
                connection = createConnection();
                try (Statement stmt = connection.createStatement()) {
                    stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS shared_waypoints (" +
                        "  id VARCHAR(36) PRIMARY KEY," +
                        "  server_id VARCHAR(64) NOT NULL," +
                        "  world_id VARCHAR(128) NOT NULL," +
                        "  name VARCHAR(256) NOT NULL," +
                        "  x INT NOT NULL," +
                        "  y INT NOT NULL," +
                        "  z INT NOT NULL," +
                        "  owner_uuid VARCHAR(36) NOT NULL," +
                        "  owner_name VARCHAR(64) NOT NULL," +
                        "  created_at BIGINT NOT NULL," +
                        "  updated_at BIGINT NOT NULL," +
                        "  INDEX idx_server (server_id)," +
                        "  INDEX idx_server_world (server_id, world_id)" +
                        ")"
                    );
                }
                LOGGER.info("[{}] Initialized successfully", getDriverName());
            } catch (Exception e) {
                LOGGER.error("[{}] Init failed", getDriverName(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveMarker(SharedWaypoint w) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                "MERGE INTO shared_waypoints (id, server_id, world_id, name, x, y, z, owner_uuid, owner_name, created_at, updated_at) " +
                "KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
            )) {
                setParams(ps, w);
                ps.executeUpdate();
            } catch (SQLException e) {
                // Fallback: delete + insert for databases without MERGE
                try {
                    try (PreparedStatement del = connection.prepareStatement(
                        "DELETE FROM shared_waypoints WHERE id = ?")) {
                        del.setString(1, w.getId());
                        del.executeUpdate();
                    }
                    try (PreparedStatement ins = connection.prepareStatement(
                        "INSERT INTO shared_waypoints (id, server_id, world_id, name, x, y, z, owner_uuid, owner_name, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                        setParams(ins, w);
                        ins.executeUpdate();
                    }
                } catch (SQLException e2) {
                    LOGGER.error("[{}] Save marker failed", getDriverName(), e2);
                }
            }
        });
    }

    private void setParams(PreparedStatement ps, SharedWaypoint w) throws SQLException {
        ps.setString(1, w.getId());
        ps.setString(2, w.getServerId());
        ps.setString(3, w.getWorldId());
        ps.setString(4, w.getName());
        ps.setInt(5, w.getX());
        ps.setInt(6, w.getY());
        ps.setInt(7, w.getZ());
        ps.setString(8, w.getOwnerUuid());
        ps.setString(9, w.getOwnerName());
        ps.setLong(10, w.getCreatedAt());
        ps.setLong(11, w.getUpdatedAt());
    }

    @Override
    public CompletableFuture<Void> deleteMarker(String id) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM shared_waypoints WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("[{}] Delete marker failed", getDriverName(), e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> deleteMarker(String serverId, String worldId,
                                                 String name, String ownerUuid) {
        return CompletableFuture.runAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                "DELETE FROM shared_waypoints WHERE server_id = ? AND world_id = ? AND name = ? AND owner_uuid = ?")) {
                ps.setString(1, serverId);
                ps.setString(2, worldId);
                ps.setString(3, name);
                ps.setString(4, ownerUuid);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("[{}] Delete marker failed", getDriverName(), e);
            }
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getMarkersForServer(String serverId) {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedWaypoint> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM shared_waypoints WHERE server_id = ?")) {
                ps.setString(1, serverId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            } catch (SQLException e) {
                LOGGER.error("[{}] Query failed", getDriverName(), e);
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getMarkersForWorld(String serverId, String worldId) {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedWaypoint> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM shared_waypoints WHERE server_id = ? AND world_id = ?")) {
                ps.setString(1, serverId);
                ps.setString(2, worldId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            } catch (SQLException e) {
                LOGGER.error("[{}] Query failed", getDriverName(), e);
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<List<SharedWaypoint>> getAllMarkers() {
        return CompletableFuture.supplyAsync(() -> {
            List<SharedWaypoint> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                "SELECT * FROM shared_waypoints");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            } catch (SQLException e) {
                LOGGER.error("[{}] Query all failed", getDriverName(), e);
            }
            return list;
        });
    }

    @Override
    public void close() {
        try { if (connection != null) connection.close(); } catch (SQLException ignored) {}
    }

    private SharedWaypoint mapRow(ResultSet rs) throws SQLException {
        SharedWaypoint w = new SharedWaypoint();
        w.setId(rs.getString("id"));
        w.setServerId(rs.getString("server_id"));
        w.setWorldId(rs.getString("world_id"));
        w.setName(rs.getString("name"));
        w.setX(rs.getInt("x"));
        w.setY(rs.getInt("y"));
        w.setZ(rs.getInt("z"));
        w.setOwnerUuid(rs.getString("owner_uuid"));
        w.setOwnerName(rs.getString("owner_name"));
        w.setCreatedAt(rs.getLong("created_at"));
        w.setUpdatedAt(rs.getLong("updated_at"));
        return w;
    }
}
