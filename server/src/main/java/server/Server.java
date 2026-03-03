package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
import io.javalin.http.Context;
import model.AuthData;
import model.UserData;
import service.GameService;
import service.UserService;

public class Server {

    private final Javalin javalin;

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("resources/web"));

        // Shared dependencies
        DataAccess dao = new MemoryDataAccess();
        UserService userService = new UserService(dao);
        GameService gameService = new GameService(dao);

        // Clear application (DELETE /db)
        javalin.delete("/db", (Context ctx) -> {
            try {
                dao.clear();
                ctx.status(200).result("{}");
            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
            }
        });

        // Register (POST /user)
        javalin.post("/user", (Context ctx) -> {
            try {
                UserData user = new Gson().fromJson(ctx.body(), UserData.class);

                AuthData auth = userService.register(user);

                // Must return: { "username":"", "authToken":"" }
                ctx.status(200);
                ctx.result(new Gson().toJson(auth));

            } catch (DataAccessException e) {
                // Map to required HTTP codes
                if ("bad request".equals(e.getMessage())) {
                    ctx.status(400);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: bad request")));
                } else if ("already taken".equals(e.getMessage())) {
                    ctx.status(403);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: already taken")));
                } else {
                    ctx.status(500);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
                }
            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
            }
        });

        // Login (POST /session)
        javalin.post("/session", ctx -> {
            try {
                var loginRequest = new Gson().fromJson(ctx.body(), UserData.class);
                if (loginRequest == null ||
                        loginRequest.username() == null ||
                        loginRequest.password() == null) {

                    ctx.status(400);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: bad request")));
                    return;
                }
                AuthData auth = userService.login(
                        loginRequest.username(),
                        loginRequest.password()
                );

                ctx.status(200);
                ctx.result(new Gson().toJson(auth));

            } catch (DataAccessException e) {

                if ("unauthorized".equals(e.getMessage())) {
                    ctx.status(401);
                    ctx.result(new Gson().toJson(
                            new ErrorResponse("Error: unauthorized")
                    ));
                } else {
                    ctx.status(500);
                    ctx.result(new Gson().toJson(
                            new ErrorResponse("Error: " + e.getMessage())
                    ));
                }

            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(
                        new ErrorResponse("Error: " + e.getMessage())
                ));
            }
        });

        // Logout (DELETE /session)
        javalin.delete("/session", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                userService.logout(authToken);

                ctx.status(200).result("{}");

            } catch (DataAccessException e) {

                if ("unauthorized".equals(e.getMessage())) {
                    ctx.status(401);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: unauthorized")));
                } else {
                    ctx.status(500);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
                }

            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
            }
        });

        // List games (GET /game)
        javalin.get("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                var games = gameService.listGames(authToken);

                ctx.status(200);
                ctx.result(new Gson().toJson(new GamesResponse(games)));

            } catch (DataAccessException e) {

                if ("unauthorized".equals(e.getMessage())) {
                    ctx.status(401);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: unauthorized")));
                } else {
                    ctx.status(500);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
                }

            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(new ErrorResponse("Error: " + e.getMessage())));
            }
        });

        // Create game (POST /game)
        javalin.post("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");

                var request = new Gson().fromJson(ctx.body(), CreateGameRequest.class);

                int gameID = gameService.createGame(authToken, request.gameName());

                ctx.status(200);
                ctx.result(new Gson().toJson(new CreateGameResponse(gameID)));

            } catch (DataAccessException e) {

                switch (e.getMessage()) {
                    case "unauthorized" -> ctx.status(401);
                    case "bad request" -> ctx.status(400);
                    default -> ctx.status(500);
                }

                ctx.result(new Gson().toJson(
                        new ErrorResponse("Error: " + e.getMessage())
                ));

            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(
                        new ErrorResponse("Error: " + e.getMessage())
                ));
            }
        });

        // Join game (PUT /game)
        javalin.put("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");

                var request = new Gson().fromJson(ctx.body(), JoinGameRequest.class);
                if (request == null ||
                        request.playerColor() == null ||
                        (!request.playerColor().equals("WHITE") &&
                                !request.playerColor().equals("BLACK"))) {

                    ctx.status(400);
                    ctx.result(new Gson().toJson(new ErrorResponse("Error: bad request")));
                    return;
                }
                gameService.joinGame(
                        authToken,
                        request.gameID(),
                        request.playerColor()
                );

                ctx.status(200).result("{}");

            } catch (DataAccessException e) {

                switch (e.getMessage()) {
                    case "unauthorized" -> ctx.status(401);
                    case "bad request" -> ctx.status(400);
                    case "already taken" -> ctx.status(403);
                    default -> ctx.status(500);
                }

                ctx.result(new Gson().toJson(
                        new ErrorResponse("Error: " + e.getMessage())
                ));

            } catch (Exception e) {
                ctx.status(500);
                ctx.result(new Gson().toJson(
                        new ErrorResponse("Error: " + e.getMessage())
                ));
            }
        });
    }



    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}