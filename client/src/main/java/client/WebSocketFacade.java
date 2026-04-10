package client;

import com.google.gson.Gson;
import websocket.commands.UserGameCommand;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;

import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

@ClientEndpoint
public class WebSocketFacade {
    private Session session;
    private final Object openLock = new Object();
    private boolean open = false;
    private final ServerMessageHandler messageHandler;

    public WebSocketFacade(String url, ServerMessageObserver observer) throws ResponseException {
        try {
            this.messageHandler = new ServerMessageHandler(observer);
            String wsURL = url.replace("http://", "ws://").replace("https://", "wss://") + "/ws";
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.connectToServer(this, URI.create(wsURL));

            synchronized (openLock) {
                long end = System.currentTimeMillis() + 3000;
                while (!open && System.currentTimeMillis() < end) {
                    openLock.wait(100);
                }
            }

            if (session == null || !open) {
                throw new ResponseException(500, "WebSocket connection did not open.");
            }
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException(500, e.getMessage());
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        synchronized (openLock) {
            open = true;
            openLock.notifyAll();
        }
    }

    @OnMessage
    public void onMessage(String message) {
        messageHandler.onMessage(message);
    }

    public void sendCommand(UserGameCommand command) throws ResponseException {
        try {
            if (session == null || !session.isOpen()) {
                throw new ResponseException(500, "WebSocket is not connected.");
            }
            session.getBasicRemote().sendText(new Gson().toJson(command));
        } catch (IOException e) {
            throw new ResponseException(500, e.getMessage());
        }
    }
}