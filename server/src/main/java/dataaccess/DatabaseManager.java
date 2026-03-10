package dataaccess;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

public class DatabaseManager {

    private static final String DB_PROPERTIES_FILE = "db.properties";
    private static final Properties properties = new Properties();
    private static boolean propertiesLoaded = false;

    private static void loadProperties(Properties props) throws Exception {
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(DB_PROPERTIES_FILE)) {
            if (in == null) {
                throw new RuntimeException("db.properties not found");
            }
            props.load(in);
        }
    }

    private static Properties loadPropertiesFromResources() throws Exception {
        if (!propertiesLoaded) {
            loadProperties(properties);
            propertiesLoaded = true;
        }
        return properties;
    }

    public static Connection getConnection() throws Exception {
        Properties props = loadPropertiesFromResources();

        String host = props.getProperty("MYSQL_HOST");
        String port = props.getProperty("MYSQL_PORT");
        String database = props.getProperty("MYSQL_DATABASE");
        String user = props.getProperty("MYSQL_USER");
        String password = props.getProperty("MYSQL_PASSWORD");

        String url = String.format("jdbc:mysql://%s:%s/%s", host, port, database);
        return DriverManager.getConnection(url, user, password);
    }

    public static void createDatabase() throws Exception {
        Properties props = loadPropertiesFromResources();

        String host = props.getProperty("MYSQL_HOST");
        String port = props.getProperty("MYSQL_PORT");
        String database = props.getProperty("MYSQL_DATABASE");
        String user = props.getProperty("MYSQL_USER");
        String password = props.getProperty("MYSQL_PASSWORD");

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