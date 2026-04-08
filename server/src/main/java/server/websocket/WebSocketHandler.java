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
                    break;
                case LEAVE:
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

    private void sendError(WsMessageContext ctx, String errorText) {
        ctx.send(GSON.toJson(new ErrorMessage(errorText)));
    }
}