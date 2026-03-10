package dataaccess;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

public class DatabaseManager {

    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static Properties properties = new Properties();

    static {
        try {
            new DatabaseManager().loadPropertiesFromResources();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load database properties", e);
        }
    }

    private void loadProperties(Properties props) {
        properties = new Properties();
        properties.putAll(props);
    }

    private void loadPropertiesFromResources() throws Exception {
        Properties props = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE)) {
            if (in == null) {
                throw new RuntimeException("db.properties not found");
            }
            props.load(in);
        }
        loadProperties(props);
    }

    public static Connection getConnection() throws Exception {
        String host = properties.getProperty("db.host");
        String port = properties.getProperty("db.port");
        String database = properties.getProperty("db.name");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        String url = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
        return DriverManager.getConnection(url, user, password);
    }

    public static void createDatabase() throws Exception {
        String host = properties.getProperty("db.host");
        String port = properties.getProperty("db.port");
        String database = properties.getProperty("db.name");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");

        String url = String.format("jdbc:mysql://%s:%s", host, port);

        try (Connection conn = DriverManager.getConnection(url, user, password);
             PreparedStatement stmt = conn.prepareStatement("CREATE DATABASE IF NOT EXISTS " + database)) {
            stmt.executeUpdate();
        }
    }

    public static void createTables() throws Exception {
        try (Connection conn = getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS user (
                        username VARCHAR(255) NOT NULL PRIMARY KEY,
                        password VARCHAR(255) NOT NULL,
                        email VARCHAR(255) NOT NULL
                    )
                    """)) {
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS auth (
                        authToken VARCHAR(255) NOT NULL PRIMARY KEY,
                        username VARCHAR(255) NOT NULL
                    )
                    """)) {
                stmt.executeUpdate();
            }

            try (PreparedStatement stmt = conn.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS game (
                        gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                        whiteUsername VARCHAR(255),
                        blackUsername VARCHAR(255),
                        gameName VARCHAR(255) NOT NULL,
                        gameData TEXT NOT NULL
                    )
                    """)) {
                stmt.executeUpdate();
            }
        }
    }

    public static void initializeDatabase() throws Exception {
        createDatabase();
        createTables();
    }
}