package client;

import com.google.gson.Gson;

import java.util.Map;

public class ServerFacade {
    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public AuthResult register(String username, String password, String email) throws ResponseException {
        throw new ResponseException(501, "Register not implemented yet");
    }

    public AuthResult login(String username, String password) throws ResponseException {
        throw new ResponseException(501, "Login not implemented yet");
    }

    public void logout(String authToken) throws ResponseException {
        throw new ResponseException(501, "Logout not implemented yet");
    }

    public CreateGameResult createGame(String authToken, String gameName) throws ResponseException {
        throw new ResponseException(501, "Create game not implemented yet");
    }

    public ListGamesResult listGames(String authToken) throws ResponseException {
        throw new ResponseException(501, "List games not implemented yet");
    }

    public void joinGame(String authToken, Integer gameID, String playerColor) throws ResponseException {
        throw new ResponseException(501, "Join game not implemented yet");
    }

    public void observeGame(String authToken, Integer gameID) throws ResponseException {
        throw new ResponseException(501, "Observe game not implemented yet");
    }

    protected <T> T makeRequest(String method, String path, Object requestBody, String authToken, Class<T> responseClass)
            throws ResponseException {
        return HttpClientHelper.makeRequest(serverUrl, method, path, requestBody, authToken, responseClass);
    }

    protected void makeRequest(String method, String path, Object requestBody, String authToken)
            throws ResponseException {
        HttpClientHelper.makeRequest(serverUrl, method, path, requestBody, authToken, null);
    }

    protected Gson getGson() {
        return gson;
    }

    protected Map<String, String> errorBody(String message) {
        return Map.of("message", message);
    }
}