package model;

import chess.ChessGame;

public record GameData(
        Integer gameID,
        String whiteUsername,
        String blackUsername,
        String gameName,
        ChessGame game,
        boolean gameOver
) {
    public GameData(Integer gameID, String whiteUsername, String blackUsername, String gameName, ChessGame game) {
        this(gameID, whiteUsername, blackUsername, gameName, game, false);
    }
}