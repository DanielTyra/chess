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
import dataaccess.MySQLDataAccess;

public class Server {

    private final Javalin javalin;
    private final Gson gson = new Gson();

    private final DataAccess dao;
    private final UserService userService;
    private final GameService gameService;

    public Server() {
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

        try {
            dao = new MySQLDataAccess();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        userService = new UserService(dao);
        gameService = new GameService(dao);

        registerClear();
        registerUser();
        registerSession();
        registerGame();
    }

    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }

    private void registerClear() {
        javalin.delete("/db", ctx -> {
            try {
                dao.clear();
                okEmpty(ctx);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });
    }

    private void registerUser() {
        javalin.post("/user", ctx -> {
            try {
                UserData user = gson.fromJson(ctx.body(), UserData.class);
                AuthData auth = userService.register(user);

                okJson(ctx, auth);
            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });
    }

    private void registerSession() {

        // Login (POST /session)
        javalin.post("/session", ctx -> {
            try {
                UserData loginRequest = gson.fromJson(ctx.body(), UserData.class);

                if (loginRequest == null ||
                        loginRequest.username() == null ||
                        loginRequest.password() == null) {
                    badRequest(ctx);
                    return;
                }

                AuthData auth = userService.login(loginRequest.username(), loginRequest.password());
                okJson(ctx, auth);

            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });

        // Logout (DELETE /session)
        javalin.delete("/session", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                userService.logout(authToken);

                okEmpty(ctx);
            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });
    }

    private void registerGame() {

        // List games (GET /game)
        javalin.get("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                var games = gameService.listGames(authToken);

                okJson(ctx, new GamesResponse(games));
            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });

        // Create game (POST /game)
        javalin.post("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                CreateGameRequest request = gson.fromJson(ctx.body(), CreateGameRequest.class);

                if (request == null || request.gameName() == null) {
                    badRequest(ctx);
                    return;
                }

                int gameID = gameService.createGame(authToken, request.gameName());
                okJson(ctx, new CreateGameResponse(gameID));

            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });

        // Join game (PUT /game)
        javalin.put("/game", ctx -> {
            try {
                String authToken = ctx.header("authorization");
                JoinGameRequest request = gson.fromJson(ctx.body(), JoinGameRequest.class);

                if (request == null ||
                        request.playerColor() == null ||
                        (!"WHITE".equals(request.playerColor()) && !"BLACK".equals(request.playerColor()))) {
                    badRequest(ctx);
                    return;
                }

                gameService.joinGame(authToken, request.gameID(), request.playerColor());
                okEmpty(ctx);

            } catch (DataAccessException e) {
                handleDataAccess(ctx, e);
            } catch (Exception e) {
                serverError(ctx, e);
            }
        });
    }

    private void okEmpty(Context ctx) {
        ctx.status(200).result("{}");
    }

    private void okJson(Context ctx, Object body) {
        ctx.status(200).result(gson.toJson(body));
    }

    private void badRequest(Context ctx) {
        ctx.status(400).result(gson.toJson(new ErrorResponse("Error: bad request")));
    }

    private void unauthorized(Context ctx) {
        ctx.status(401).result(gson.toJson(new ErrorResponse("Error: unauthorized")));
    }

    private void alreadyTaken(Context ctx) {
        ctx.status(403).result(gson.toJson(new ErrorResponse("Error: already taken")));
    }

    private void serverError(Context ctx, Exception e) {
        ctx.status(500).result(gson.toJson(new ErrorResponse("Error: " + e.getMessage())));
    }

    private void handleDataAccess(Context ctx, DataAccessException e) {
        String msg = e.getMessage();

        if ("bad request".equals(msg)) {
            badRequest(ctx);
        } else if ("unauthorized".equals(msg)) {
            unauthorized(ctx);
        } else if ("already taken".equals(msg)) {
            alreadyTaken(ctx);
        } else {
            ctx.status(500).result(gson.toJson(new ErrorResponse("Error: " + msg)));
        }
    }
}