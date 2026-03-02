package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

    @Test
    public void registerPositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        AuthData auth = service.register(new UserData("bob", "pass", "bob@email.com"));

        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
    }

    @Test
    public void registerNegativeAlreadyTaken() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        service.register(new UserData("bob", "pass", "bob@email.com"));

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                service.register(new UserData("bob", "pass2", "bob2@email.com"))
        );

        assertEquals("already taken", ex.getMessage());
    }

    @Test
    public void loginPositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        service.register(new UserData("bob", "pass", "bob@email.com"));

        AuthData auth = service.login("bob", "pass");

        assertNotNull(auth);
        assertNotNull(auth.authToken());
        assertEquals("bob", auth.username());
    }

    @Test
    public void loginNegativeUnauthorized() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        service.register(new UserData("bob", "pass", "bob@email.com"));

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                service.login("bob", "WRONG")
        );

        assertEquals("unauthorized", ex.getMessage());
    }

    @Test
    public void logoutPositive() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        AuthData auth = service.register(new UserData("bob", "pass", "bob@email.com"));

        assertDoesNotThrow(() -> service.logout(auth.authToken()));
    }

    @Test
    public void logoutNegativeUnauthorized() throws Exception {
        DataAccess dao = new MemoryDataAccess();
        UserService service = new UserService(dao);

        DataAccessException ex = assertThrows(DataAccessException.class, () ->
                service.logout("not-a-real-token")
        );

        assertEquals("unauthorized", ex.getMessage());
    }
}