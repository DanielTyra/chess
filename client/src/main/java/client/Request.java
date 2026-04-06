package client;

record LoginRequest(String username, String password) { }

record RegisterRequest(String username, String password, String email) { }

record CreateGameRequest(String gameName) { }

record JoinGameRequest(String playerColor, Integer gameID) { }