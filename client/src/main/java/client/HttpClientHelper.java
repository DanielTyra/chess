package client;

import com.google.gson.Gson;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class HttpClientHelper {
    private static final Gson GSON = new Gson();

    public static <T> T makeRequest(
            String serverUrl,
            String method,
            String path,
            Object requestBody,
            String authToken,
            Class<T> responseClass
    ) throws ResponseException {
        try {
            URI uri = new URI(serverUrl + path);
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setDoInput(true);

            if (authToken != null && !authToken.isBlank()) {
                connection.addRequestProperty("Authorization", authToken);
            }

            if (requestBody != null) {
                connection.setDoOutput(true);
                connection.addRequestProperty("Content-Type", "application/json");

                String json = GSON.toJson(requestBody);
                try (OutputStream body = connection.getOutputStream()) {
                    body.write(json.getBytes(StandardCharsets.UTF_8));
                }
            }

            connection.connect();

            int statusCode = connection.getResponseCode();
            if (statusCode / 100 != 2) {
                throw buildResponseException(connection, statusCode);
            }

            if (responseClass == null) {
                return null;
            }

            try (InputStream responseBody = connection.getInputStream()) {
                if (responseBody == null) {
                    return null;
                }
                InputStreamReader reader = new InputStreamReader(responseBody, StandardCharsets.UTF_8);
                return GSON.fromJson(reader, responseClass);
            }
        } catch (ResponseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseException(500, "Network error: " + e.getMessage());
        }
    }

    private static ResponseException buildResponseException(HttpURLConnection connection, int statusCode) {
        try (InputStream errorStream = connection.getErrorStream()) {
            if (errorStream != null) {
                InputStreamReader reader = new InputStreamReader(errorStream, StandardCharsets.UTF_8);
                ErrorMessage error = GSON.fromJson(reader, ErrorMessage.class);
                if (error != null && error.message() != null && !error.message().isBlank()) {
                    return new ResponseException(statusCode, error.message());
                }
            }
        } catch (Exception ignored) {
        }

        return new ResponseException(statusCode, "Request failed with status code " + statusCode);
    }

    private record ErrorMessage(String message) {
    }
}