package dataaccess;

import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import static org.junit.jupiter.api.Assertions.*;

public class MySQLUserAuthDAOTest {

    private MySQLDataAccess dao;

    @BeforeEach
    public void setup() throws Exception {
        dao = new MySQLDataAccess();
        dao.clear();
    }

    @Test
    public void createUserPositive() throws Exception {

        UserData user = new UserData("bob", "password", "bob@email.com");

        dao.createUser(user);

        UserData result = dao.getUser("bob");

        assertNotNull(result);
        assertEquals("bob", result.username());
        assertTrue(BCrypt.checkpw("password", result.password()));
    }

    @Test
    public void createUserNegativeDuplicate() throws Exception {

        UserData user = new UserData("bob", "password", "bob@email.com");

        dao.createUser(user);

        assertThrows(DataAccessException.class, () -> dao.createUser(user));
    }

    @Test
    public void getUserPositive() throws Exception {

        dao.createUser(new UserData("alice", "123", "alice@email.com"));

        UserData user = dao.getUser("alice");

        assertNotNull(user);
        assertEquals("alice", user.username());
    }

    @Test
    public void getUserNegativeMissing() throws Exception {

        UserData user = dao.getUser("missing");

        assertNull(user);
    }

    @Test
    public void createAuthPositive() throws Exception {

        AuthData auth = new AuthData("token123", "bob");

        dao.createAuth(auth);

        AuthData result = dao.getAuth("token123");

        assertNotNull(result);
        assertEquals("bob", result.username());
    }

    @Test
    public void createAuthNegativeDuplicate() throws Exception {

        AuthData auth = new AuthData("token123", "bob");

        dao.createAuth(auth);

        assertThrows(DataAccessException.class, () -> dao.createAuth(auth));
    }

    @Test
    public void getAuthPositive() throws Exception {

        dao.createAuth(new AuthData("token123", "bob"));

        AuthData auth = dao.getAuth("token123");

        assertNotNull(auth);
        assertEquals("bob", auth.username());
    }

    @Test
    public void getAuthNegativeMissing() throws Exception {

        AuthData auth = dao.getAuth("missing");

        assertNull(auth);
    }

    @Test
    public void deleteAuthPositive() throws Exception {

        dao.createAuth(new AuthData("token123", "bob"));

        dao.deleteAuth("token123");

        assertNull(dao.getAuth("token123"));
    }

    @Test
    public void deleteAuthNegativeMissing() {

        assertDoesNotThrow(() -> dao.deleteAuth("missing"));
    }

    @Test
    public void clearPositive() throws Exception {

        dao.createUser(new UserData("bob", "pass", "email"));
        dao.createAuth(new AuthData("token", "bob"));
        dao.createGame("test");

        dao.clear();

        assertNull(dao.getUser("bob"));
        assertNull(dao.getAuth("token"));
        assertEquals(0, dao.listGames().size());
    }
}