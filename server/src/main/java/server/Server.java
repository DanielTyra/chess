package server;

import com.google.gson.Gson;
import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import io.javalin.Javalin;
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
        javalin.delete("/db", ctx -> {
            try {
                dao.clear();
                ctx.status(200).result("{}");
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