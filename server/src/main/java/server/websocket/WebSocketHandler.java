package server.websocket;

import com.google.gson.Gson;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsMessageContext;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;

public class WebSocketHandler {

    private static final Gson GSON = new Gson();
    private static final ConnectionManager CONNECTIONS = new ConnectionManager();

    public static ConnectionManager getConnectionManager() {
        return CONNECTIONS;
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

    private void sendError(WsMessageContext ctx, String errorText) {
        ctx.send(GSON.toJson(new ErrorMessage(errorText)));
    }
}