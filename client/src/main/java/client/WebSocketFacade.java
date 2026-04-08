package client;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;

@ClientEndpoint
public class WebSocketFacade {

    private Session session;
    private final ServerMessageObserver observer;

    public WebSocketFacade(String url, ServerMessageObserver observer) throws ResponseException {
        try {
            this.observer = observer;

            String wsURL = url.replace("http://", "ws://").replace("https://", "wss://") + "/ws";
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(wsURL));
        } catch (Exception e) {
            throw new ResponseException(500, e.getMessage());
        }
    }

    @jakarta.websocket.OnOpen
    public void onOpen(Session session, EndpointConfig config) {
        this.session = session;
        this.session.addMessageHandler((MessageHandler.Whole<String>) message ->
                new ServerMessageHandler(observer).onMessage(message));
    }

    public void sendCommand(UserGameCommand command) throws ResponseException {
        try {
            session.getBasicRemote().sendText(new Gson().toJson(command));
        } catch (IOException e) {
            throw new ResponseException(500, e.getMessage());
        }
    }
}