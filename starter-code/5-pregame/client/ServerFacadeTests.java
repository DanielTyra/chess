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
        assertFalse(authData.authToken().isBlank());
    }

    @Test
    void registerNegativeDuplicateUsername() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        assertThrows(ResponseException.class, () ->
                facade.register("player1", "password2", "p2@email.com"));
    }

    @Test
    void loginPositive() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        var authData = facade.login("player1", "password");

        assertNotNull(authData);
        assertEquals("player1", authData.username());
        assertNotNull(authData.authToken());
        assertFalse(authData.authToken().isBlank());
    }

    @Test
    void loginNegativeWrongPassword() throws Exception {
        facade.register("player1", "password", "p1@email.com");

        assertThrows(ResponseException.class, () ->
                facade.login("player1", "wrongPassword"));
    }

    @Test
    void logoutPositive() throws Exception {
        var authData = facade.register("player1", "password", "p1@email.com");

        assertDoesNotThrow(() -> facade.logout(authData.authToken()));
    }

    @Test
    void logoutNegativeBadAuthToken() {
        assertThrows(ResponseException.class, () ->
                facade.logout("bad-auth-token"));
    }
}