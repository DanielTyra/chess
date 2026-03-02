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
        javalin = Javalin.create(config -> config.staticFiles.add("web"));

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
    }



    public int run(int desiredPort) {
        javalin.start(desiredPort);
        return javalin.port();
    }

    public void stop() {
        javalin.stop();
    }
}