package server.websocket;

import io.javalin.websocket.WsContext;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {

    private final Map<Integer, Map<String, WsContext>> connections = new ConcurrentHashMap<>();

    public void add(String username, Integer gameID, WsContext ctx) {
        connections.computeIfAbsent(gameID, k -> new ConcurrentHashMap<>()).put(username, ctx);
    }

    public void remove(String username, Integer gameID) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections != null) {
            gameConnections.remove(username);
            if (gameConnections.isEmpty()) {
                connections.remove(gameID);
            }
        }
    }

    public Set<String> getConnectedUsers(Integer gameID) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null) {
            return Set.of();
        }
        return gameConnections.keySet();
    }

    public void broadcast(Integer gameID, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null){
            return;
        }

        for (WsContext ctx : gameConnections.values()) {
            ctx.send(message);
        }
    }

    public void broadcastExcept(Integer gameID, String excludedUser, String message) {
        Map<String, WsContext> gameConnections = connections.get(gameID);
        if (gameConnections == null){
            return;
        }

        for (Map.Entry<String, WsContext> entry : gameConnections.entrySet()) {
            if (!entry.getKey().equals(excludedUser)) {
                entry.getValue().send(message);
            }
        }
    }
}