package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameServiceTest {

    private AuthData registerAndLogin(DataAccess dao) throws Exception {
        UserService userService = new UserService(dao);
        return userService.register(new UserData("bob", "pass", "bob@email.com"));
    }

    @Test
    public void createGamePositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        AuthData auth = registerAndLogin(dao);

        int gameID = gameService.createGame(auth.authToken(), "Test Game");

        assertTrue(gameID > 0);
    }

    @Test
    public void createGameNegativeUnauthorized() {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                gameService.createGame("bad-token", "Test Game")
        );

        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void listGamesPositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        AuthData auth = registerAndLogin(dao);
        gameService.createGame(auth.authToken(), "Game 1");

        var games = gameService.listGames(auth.authToken());

        assertEquals(1, games.size());
    }

    @Test
    public void listGamesNegativeUnauthorized() {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                gameService.listGames("bad-token")
        );

        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void joinGamePositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        AuthData auth = registerAndLogin(dao);
        int gameID = gameService.createGame(auth.authToken(), "Test Game");

        assertDoesNotThrow(() ->
                gameService.joinGame(auth.authToken(), gameID, "WHITE")
        );
    }

    @Test
    public void joinGameNegativeAlreadyTaken() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        GameService gameService = new GameService(dao);

        AuthData auth = registerAndLogin(dao);
        int gameID = gameService.createGame(auth.authToken(), "Test Game");

        gameService.joinGame(auth.authToken(), gameID, "WHITE");

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                gameService.joinGame(auth.authToken(), gameID, "WHITE")
        );

        assertEquals("already taken", ex.getMessage());
    }
}