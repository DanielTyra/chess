package client;

import org.junit.jupiter.api.*;
import server.Server;

import static org.junit.jupiter.api.Assertions.*;

public class ServerFacadeTests {

    private static Server server;
    private static ServerFacade facade;

    @BeforeAll
    public static void init() {
        server = new Server();
        var port = server.run(0);
        System.out.println("Started test HTTP server on " + port);
        facade = new ServerFacade(port);
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void clearDatabase() throws Exception {
        facade.clear();
    }

    @Test
    void registerPositive() throws Exception {
        var authData = facade.register("player1", "password", "p1@email.com");

        assertNotNull(authData);
        assertEquals("player1", authData.username());
        assertNotNull(authData.authToken());
    }

    @Test
    void registerNegativeDuplicateUsername() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        assertThrows(ResponseException.class, () ->
                facade.register("player1", "differentPassword", "other@email.com"));
    }

    @Test
    void loginPositive() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        var authData = facade.login("player1", "password");

        assertNotNull(authData);
        assertEquals("player1", authData.username());
        assertNotNull(authData.authToken());
    }

    @Test
    void loginNegativeWrongPassword() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        assertThrows(ResponseException.class, () ->
                facade.login("player1", "wrongPassword"));
    }

    @Test
    void logoutPositive() throws Exception {
        var auth = facade.register("player1", "password", "p1@email.com");

        assertDoesNotThrow(() -> facade.logout(auth.authToken()));
    }

    @Test
    void logoutNegativeBadAuthToken() {
        assertThrows(ResponseException.class, () ->
                facade.logout("bad-auth-token"));
    }

    @Test
    void createGamePositive() throws Exception {
        var auth = facade.register("player1", "password", "email");

        var result = facade.createGame(auth.authToken(), "MyGame");

        assertNotNull(result);
        assertNotNull(result.gameID());
    }

    @Test
    void createGameNegativeNoAuth() {
        assertThrows(ResponseException.class, () ->
                facade.createGame(null, "BadGame"));
    }

    @Test
    void listGamesPositive() throws Exception {
        var auth = facade.register("player1", "password", "email");

        facade.createGame(auth.authToken(), "Game1");

        var result = facade.listGames(auth.authToken());

        assertNotNull(result);
        assertNotNull(result.games());
        assertTrue(result.games().size() >= 1);
    }

    @Test
    void listGamesNegativeNoAuth() {
        assertThrows(ResponseException.class, () ->
                facade.listGames(null));
    }

    @Test
    void joinGamePositive() throws Exception {
        var auth = facade.register("player1", "password", "email");

        var game = facade.createGame(auth.authToken(), "game");

        assertDoesNotThrow(() ->
                facade.joinGame(auth.authToken(), game.gameID(), "WHITE"));
    }

    @Test
    void joinGameNegativeBadGame() throws Exception {
        var auth = facade.register("player1", "password", "email");

        assertThrows(ResponseException.class, () ->
                facade.joinGame(auth.authToken(), 9999, "WHITE"));
    }
}