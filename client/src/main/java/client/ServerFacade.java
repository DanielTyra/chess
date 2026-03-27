package client;

public class ServerFacade {
    private final String serverUrl;

    public ServerFacade(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public ServerFacade(int port) {
        this("http://localhost:" + port);
    }

    public AuthResult register(String username, String password, String email) throws ResponseException {
        var request = new RegisterRequest(username, password, email);
        return makeRequest("POST", "/user", request, null, AuthResult.class);
    }

    public AuthResult login(String username, String password) throws ResponseException {
        var request = new LoginRequest(username, password);
        return makeRequest("POST", "/session", request, null, AuthResult.class);
    }

    public void logout(String authToken) throws ResponseException {
        makeRequest("DELETE", "/session", null, authToken);
    }

    public CreateGameResult createGame(String authToken, String gameName) throws ResponseException {
        var request = new CreateGameRequest(gameName);
        return makeRequest("POST", "/game", request, authToken, CreateGameResult.class);
    }

    public ListGamesResult listGames(String authToken) throws ResponseException {
        return makeRequest("GET", "/game", null, authToken, ListGamesResult.class);
    }

    // 🔥 IMPLEMENTED
    public void joinGame(String authToken, Integer gameID, String playerColor) throws ResponseException {
        var request = new JoinGameRequest(playerColor, gameID);
        makeRequest("PUT", "/game", request, authToken);
    }

    public void observeGame(String authToken, Integer gameID) throws ResponseException {
        var request = new JoinGameRequest(null, gameID);
        makeRequest("PUT", "/game", request, authToken);
    }

    public void clear() throws ResponseException {
        makeRequest("DELETE", "/db", null, null);
    }

    private <T> T makeRequest(String method, String path, Object requestBody, String authToken, Class<T> responseClass)
            throws ResponseException {
        return HttpClientHelper.makeRequest(serverUrl, method, path, requestBody, authToken, responseClass);
    }

    private void makeRequest(String method, String path, Object requestBody, String authToken)
            throws ResponseException {
        HttpClientHelper.makeRequest(serverUrl, method, path, requestBody, authToken, null);
    }
}