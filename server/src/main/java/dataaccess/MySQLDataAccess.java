package dataaccess;

import chess.ChessGame;
import com.google.gson.Gson;
import model.AuthData;
import model.GameData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MySQLDataAccess implements DataAccess {

    private final Gson gson = new Gson();

    public MySQLDataAccess() throws DataAccessException {
        configureDatabase();
    }

    private void configureDatabase() throws DataAccessException {
        try {
            DatabaseManager.createDatabase();

            try (var conn = DatabaseManager.getConnection()) {
                try (var stmt = conn.createStatement()) {

                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS user (
                            username VARCHAR(255) NOT NULL PRIMARY KEY,
                            password VARCHAR(255) NOT NULL,
                            email VARCHAR(255) NOT NULL
                        )
                    """);

                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS auth (
                            authToken VARCHAR(255) NOT NULL PRIMARY KEY,
                            username VARCHAR(255) NOT NULL
                        )
                    """);

                    stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS game (
                            gameID INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                            whiteUsername VARCHAR(255),
                            blackUsername VARCHAR(255),
                            gameName VARCHAR(255) NOT NULL,
                            gameData TEXT NOT NULL
                        )
                    """);

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new DataAccessException("Unable to configure database: " + e.getMessage());
        }
    }

    @Override
    public void clear() throws DataAccessException {
        String[] statements = {
                "DELETE FROM auth",
                "DELETE FROM game",
                "DELETE FROM user"
        };

        try (var conn = DatabaseManager.getConnection()) {
            for (String sql : statements) {
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            throw new DataAccessException("database error");
        }
    }

    // USER

    @Override
    public void createUser(UserData user) throws DataAccessException {

        String sql = "INSERT INTO user (username, password, email) VALUES (?, ?, ?)";

        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.username());
            stmt.setString(2, hashedPassword);
            stmt.setString(3, user.email());

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Unable to create user");
        }
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {

        String sql = "SELECT username, password, email FROM user WHERE username=?";

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    return new UserData(
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("email")
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("Unable to get user");
        }
    }

    // AUTH

    @Override
    public void createAuth(AuthData auth) throws DataAccessException {

        String sql = "INSERT INTO auth (authToken, username) VALUES (?, ?)";

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, auth.authToken());
            stmt.setString(2, auth.username());

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("Unable to create auth");
        }
    }

    @Override
    public AuthData getAuth(String authToken) throws DataAccessException {
        String sql = "SELECT authToken, username FROM auth WHERE authToken=?";

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AuthData(
                            rs.getString("authToken"),
                            rs.getString("username")
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("database error");
        }
    }

    @Override
    public void deleteAuth(String authToken) throws DataAccessException {
        String sql = "DELETE FROM auth WHERE authToken=?";

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authToken);
            stmt.executeUpdate();

        } catch (Exception e) {
            throw new DataAccessException("database error");
        }
    }

    // GAME

    @Override
    public int createGame(String gameName) throws DataAccessException {

        String sql = "INSERT INTO game (whiteUsername, blackUsername, gameName, gameData) VALUES (?, ?, ?, ?)";

        ChessGame game = new ChessGame();
        String gameJson = gson.toJson(game);

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, null);
            stmt.setString(2, null);
            stmt.setString(3, gameName);
            stmt.setString(4, gameJson);

            stmt.executeUpdate();

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

            throw new DataAccessException("Unable to create game");

        } catch (Exception e) {
            throw new DataAccessException("Unable to create game");
        }
    }

    @Override
    public GameData getGame(int gameID) throws DataAccessException {

        String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameData FROM game WHERE gameID=?";

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, gameID);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {

                    ChessGame game = gson.fromJson(rs.getString("gameData"), ChessGame.class);

                    return new GameData(
                            rs.getInt("gameID"),
                            rs.getString("whiteUsername"),
                            rs.getString("blackUsername"),
                            rs.getString("gameName"),
                            game
                    );
                }
            }

            return null;

        } catch (Exception e) {
            throw new DataAccessException("Unable to get game");
        }
    }

    @Override
    public List<GameData> listGames() throws DataAccessException {

        String sql = "SELECT gameID, whiteUsername, blackUsername, gameName, gameData FROM game";

        List<GameData> games = new ArrayList<>();

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                ChessGame game = gson.fromJson(rs.getString("gameData"), ChessGame.class);

                games.add(new GameData(
                        rs.getInt("gameID"),
                        rs.getString("whiteUsername"),
                        rs.getString("blackUsername"),
                        rs.getString("gameName"),
                        game
                ));
            }

            return games;

        } catch (Exception e) {
            throw new DataAccessException("Unable to list games");
        }
    }

    @Override
    public void updateGame(GameData game) throws DataAccessException {

        String sql = """
            UPDATE game
            SET whiteUsername=?, blackUsername=?, gameName=?, gameData=?
            WHERE gameID=?
        """;

        try (var conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, game.whiteUsername());
            stmt.setString(2, game.blackUsername());
            stmt.setString(3, game.gameName());
            stmt.setString(4, gson.toJson(game.game()));
            stmt.setInt(5, game.gameID());

            int rows = stmt.executeUpdate();

            if (rows == 0) {
                throw new DataAccessException("Game not found");
            }

        } catch (Exception e) {
            throw new DataAccessException("Unable to update game");
        }
    }
}