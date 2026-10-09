package library.management.system.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton database connection manager.
 *
 * The connection settings are loaded from db.properties
 * instead of being hard-coded in Java.
 */
public final class DBConnection {

    private static DBConnection instance;

    private final String url;
    private final String user;
    private final String password;

    private DBConnection() {
        Properties properties = new Properties();

        try (InputStream input = DBConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                throw new IllegalStateException(
                        "db.properties file was not found.");
            }

            properties.load(input);

            url = properties.getProperty("db.url");
            user = properties.getProperty("db.user");
            password = properties.getProperty("db.password");

            if (url == null || user == null || password == null) {
                throw new IllegalStateException(
                        "Database properties are incomplete.");
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to load database configuration.", e);
        }
    }

    /**
     * Synchronized Singleton accessor.
     *
     * Synchronization prevents two threads from creating
     * separate DBConnection instances at the same time.
     */
    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }

        return instance;
    }

    /**
     * Creates a new JDBC connection.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                url,
                user,
                password);
    }
}