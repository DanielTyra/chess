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

    // ---------- AUTH TESTS ----------
    @Test
    void registerPositive() throws Exception {
        var authData = facade.register("player1", "password", "p1@email.com");

        assertNotNull(authData);
        assertEquals("player1", authData.username());
        assertNotNull(authData.authToken());
    }

    @Test
    void loginPositive() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        var authData = facade.login("player1", "password");

        assertNotNull(authData);
        assertEquals("player1", authData.username());
    }

    // ---------- CREATE GAME ----------
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

    // ---------- LIST GAMES ----------
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
}