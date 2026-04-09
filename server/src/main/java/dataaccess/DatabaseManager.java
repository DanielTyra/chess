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

}