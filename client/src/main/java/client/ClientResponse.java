package client;

public record ClientResponse(boolean success, String message) {
    public static ClientResponse success(String message) {
        return new ClientResponse(true, message);
    }

    public static ClientResponse error(String message) {
        return new ClientResponse(false, message);
    }
}