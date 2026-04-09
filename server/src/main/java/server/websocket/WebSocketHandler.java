package server.websocket;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MySQLDataAccess;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsMessageContext;
import model.AuthData;
import model.GameData;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import chess.ChessGame;
import chess.ChessPosition;

public class WebSocketHandler {

    private static final Gson GSON = new Gson();
    private static final ConnectionManager CONNECTIONS = new ConnectionManager();

    private final DataAccess dao;

    public WebSocketHandler() {
        try {
            dao = new MySQLDataAccess();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void onMessage(WsMessageContext ctx) {
        try {
            String message = ctx.message();
            UserGameCommand command = GSON.fromJson(message, UserGameCommand.class);

            if (command == null || command.getCommandType() == null) {
                sendError(ctx, "Error: invalid command");
                return;
            }

            switch (command.getCommandType()) {
                case CONNECT:
                    handleConnect(ctx, command);
                    break;
                case MAKE_MOVE:
                    handleMakeMove(ctx, message);
                    break;
                case LEAVE:
                    handleLeave(ctx, command);
                    break;
                case RESIGN:
                    break;
                default:
                    sendError(ctx, "Error: unknown command");
                    break;
            }

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    public void onClose(WsCloseContext ctx) {
    }

    private void handleConnect(WsMessageContext ctx, UserGameCommand command) {
        try {
            // Validate auth
            AuthData auth = dao.getAuth(command.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            String username = auth.username();
            int gameID = command.getGameID();

            // Load game
            GameData gameData = dao.getGame(gameID);
            if (gameData == null) {
                sendError(ctx, "Error: game not found");
                return;
            }

            // Add connection
            CONNECTIONS.add(username, gameID, ctx);

            // Send game to this user
            LoadGameMessage loadMsg = new LoadGameMessage(gameData.game());
            ctx.send(GSON.toJson(loadMsg));

            // Determine role
            String role;
            if (username.equals(gameData.whiteUsername())) {
                role = "white";
            } else if (username.equals(gameData.blackUsername())) {
                role = "black";
            } else {
                role = "observer";
            }

            // Notify others
            String notification;
            if (role.equals("observer")) {
                notification = username + " joined as an observer";
            } else {
                notification = username + " joined as " + role;
            }

            NotificationMessage note = new NotificationMessage(notification);
            CONNECTIONS.broadcastExcept(gameID, username, GSON.toJson(note));

        } catch (DataAccessException e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private void handleMakeMove(WsMessageContext ctx, String rawMessage) {
        try {
            var moveCmd = GSON.fromJson(rawMessage, websocket.commands.MakeMoveCommand.class);

            // Validate auth
            var auth = dao.getAuth(moveCmd.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            String username = auth.username();
            int gameID = moveCmd.getGameID();

            var gameData = dao.getGame(gameID);
            if (gameData == null) {
                sendError(ctx, "Error: game not found");
                return;
            }

            var game = gameData.game();

            // Determine player color
            ChessGame.TeamColor playerColor = null;
            if (username.equals(gameData.whiteUsername())) {
                playerColor = ChessGame.TeamColor.WHITE;
            } else if (username.equals(gameData.blackUsername())) {
                playerColor = ChessGame.TeamColor.BLACK;
            } else {
                sendError(ctx, "Error: observers cannot move");
                return;
            }

            // Check turn
            if (game.getTeamTurn() != playerColor) {
                sendError(ctx, "Error: not your turn");
                return;
            }

            var move = moveCmd.getMove();

            // Validate move
            var validMoves = game.validMoves(move.getStartPosition());
            if (validMoves == null || !validMoves.contains(move)) {
                sendError(ctx, "Error: invalid move");
                return;
            }

            // Make move
            game.makeMove(move);

            // Save game
            var updatedGame = new model.GameData(
                    gameID,
                    gameData.whiteUsername(),
                    gameData.blackUsername(),
                    gameData.gameName(),
                    game
            );
            dao.updateGame(updatedGame);

            // Broadcast updated game
            var loadMsg = new websocket.messages.LoadGameMessage(game);
            CONNECTIONS.broadcast(gameID, GSON.toJson(loadMsg));

            // Notify move
            String moveText = username + " moved " +
                    positionToString(move.getStartPosition()) + " to " +
                    positionToString(move.getEndPosition());

            var note = new websocket.messages.NotificationMessage(moveText);
            CONNECTIONS.broadcastExcept(gameID, username, GSON.toJson(note));

            // Check notifications
            ChessGame.TeamColor opponent =
                    (playerColor == ChessGame.TeamColor.WHITE)
                            ? ChessGame.TeamColor.BLACK
                            : ChessGame.TeamColor.WHITE;

            if (game.isInCheckmate(opponent)) {
                CONNECTIONS.broadcast(gameID,
                        GSON.toJson(new websocket.messages.NotificationMessage(
                                opponent + " is in checkmate"
                        )));
            } else if (game.isInCheck(opponent)) {
                CONNECTIONS.broadcast(gameID,
                        GSON.toJson(new websocket.messages.NotificationMessage(
                                opponent + " is in check"
                        )));
            }

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }

    private String positionToString(chess.ChessPosition pos) {
        char file = (char) ('a' + pos.getColumn() - 1);
        int rank = pos.getRow();
        return "" + file + rank;
    }

    private void sendError(WsMessageContext ctx, String errorText) {
        ctx.send(GSON.toJson(new ErrorMessage(errorText)));
    }

    private void handleLeave(WsMessageContext ctx, UserGameCommand command) {
        try {
            var auth = dao.getAuth(command.getAuthToken());
            if (auth == null) {
                sendError(ctx, "Error: unauthorized");
                return;
            }

            String username = auth.username();
            int gameID = command.getGameID();

            var gameData = dao.getGame(gameID);
            if (gameData == null) {
                sendError(ctx, "Error: game not found");
                return;
            }

            boolean wasWhite = username.equals(gameData.whiteUsername());
            boolean wasBlack = username.equals(gameData.blackUsername());
            boolean wasObserver = !wasWhite && !wasBlack;

            if (wasWhite || wasBlack) {
                var updatedGame = new model.GameData(
                        gameID,
                        wasWhite ? null : gameData.whiteUsername(),
                        wasBlack ? null : gameData.blackUsername(),
                        gameData.gameName(),
                        gameData.game()
                );
                dao.updateGame(updatedGame);
            }

            CONNECTIONS.remove(username, gameID);

            String message;
            if (wasObserver) {
                message = username + " left the game";
            } else {
                message = username + " left the game";
            }

            var note = new websocket.messages.NotificationMessage(message);
            CONNECTIONS.broadcast(gameID, GSON.toJson(note));

        } catch (Exception e) {
            sendError(ctx, "Error: " + e.getMessage());
        }
    }
}