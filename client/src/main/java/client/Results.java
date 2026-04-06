package client;

import java.util.List;

record AuthResult(String username, String authToken) { }

record CreateGameResult(Integer gameID) { }

record GameData(Integer gameID, String whiteUsername, String blackUsername, String gameName) { }

record ListGamesResult(List<GameData> games) { }