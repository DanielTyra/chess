package client;

import com.google.gson.Gson;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

public class ServerMessageHandler {

    private final ServerMessageObserver observer;

    public ServerMessageHandler(ServerMessageObserver observer) {
        this.observer = observer;
    }

    public void onMessage(String message) {
        Gson gson = new Gson();

        ServerMessage baseMessage = gson.fromJson(message, ServerMessage.class);
        if (baseMessage == null || baseMessage.getServerMessageType() == null) {
            return;
        }

        ServerMessage fullMessage;
        switch (baseMessage.getServerMessageType()) {
            case LOAD_GAME:
                fullMessage = gson.fromJson(message, LoadGameMessage.class);
                break;
            case ERROR:
                fullMessage = gson.fromJson(message, ErrorMessage.class);
                break;
            case NOTIFICATION:
                fullMessage = gson.fromJson(message, NotificationMessage.class);
                break;
            default:
                return;
        }

        observer.notify(fullMessage);
    }
}