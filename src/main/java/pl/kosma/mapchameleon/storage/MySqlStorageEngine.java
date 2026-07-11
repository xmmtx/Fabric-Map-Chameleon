package pl.kosma.mapchameleon.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;

/**
 * MySQL / MariaDB storage engine using HikariCP connection pool.
 */
public class MySqlStorageEngine extends JdbcStorageEngine {
    private final String host, database, username, password;
    private final int port;
    private HikariDataSource dataSource;

    public MySqlStorageEngine(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    protected String getDriverName() { return "MySQL"; }

    @Override
    protected Connection createConnection() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        dataSource = new HikariDataSource(config);
        return dataSource.getConnection();
    }

    @Override
    public void close() {
        if (dataSource != null) dataSource.close();
    }
}
