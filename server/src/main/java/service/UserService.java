package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.AuthData;
import model.UserData;
import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class UserService {

    private final DataAccess dao;

    public UserService(DataAccess dao) {
        this.dao = dao;
    }

    public AuthData register(UserData user) throws DataAccessException {

        if (user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("bad request");
        }

        if (dao.getUser(user.username()) != null) {
            throw new DataAccessException("already taken");
        }

        dao.createUser(user);

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, user.username());

        dao.createAuth(auth);

        return auth;
    }

    public AuthData login(String username, String password) throws DataAccessException {

        UserData user = dao.getUser(username);

        if (user == null || !BCrypt.checkpw(password, user.password())) {
            throw new DataAccessException("unauthorized");
        }

        String token = UUID.randomUUID().toString();
        AuthData auth = new AuthData(token, username);

        dao.createAuth(auth);

        return auth;
    }

    public void logout(String authToken) throws DataAccessException {

        if (dao.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        dao.deleteAuth(authToken);
    }
}