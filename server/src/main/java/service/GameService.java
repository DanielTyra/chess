package service;

import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import model.*;

import java.util.List;

public class GameService {

    private final DataAccess dao;

    public GameService(DataAccess dao) {
        this.dao = dao;
    }

    public List<GameData> listGames(String authToken) throws DataAccessException {
        if (dao.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        return dao.listGames();
    }

    public int createGame(String authToken, String gameName) throws DataAccessException {

        if (gameName == null) {
            throw new DataAccessException("bad request");
        }

        if (dao.getAuth(authToken) == null) {
            throw new DataAccessException("unauthorized");
        }

        return dao.createGame(gameName);
    }

    public void joinGame(String authToken, int gameID, String playerColor) throws DataAccessException {

        AuthData auth = dao.getAuth(authToken);
        if (auth == null) {
            throw new DataAccessException("unauthorized");
        }

        GameData game = dao.getGame(gameID);
        if (game == null) {
            throw new DataAccessException("bad request");
        }

        if ("WHITE".equals(playerColor)) {

            if (game.whiteUsername() != null) {
                throw new DataAccessException("already taken");
            }

            dao.updateGame(new GameData(
                    gameID,
                    auth.username(),
                    game.blackUsername(),
                    game.gameName(),
                    game.game()
            ));

        } else if ("BLACK".equals(playerColor)) {

            if (game.blackUsername() != null) {
                throw new DataAccessException("already taken");
            }

            dao.updateGame(new GameData(
                    gameID,
                    game.whiteUsername(),
                    auth.username(),
                    game.gameName(),
                    game.game()
            ));

        } else {
            throw new DataAccessException("bad request");
        }
    }
}