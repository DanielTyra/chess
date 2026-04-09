package client;

import chess.ChessGame;
import chess.ChessMove;
import chess.ChessPosition;
import websocket.commands.MakeMoveCommand;
import websocket.commands.UserGameCommand;
import websocket.messages.ErrorMessage;
import websocket.messages.LoadGameMessage;
import websocket.messages.NotificationMessage;
import websocket.messages.ServerMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;

public class Repl implements ServerMessageObserver {
    private final String serverUrl;
    private final Scanner scanner = new Scanner(System.in);
    private final ServerFacade serverFacade;
    private final ChessBoardRenderer boardRenderer = new ChessBoardRenderer();

    private boolean running = true;
    private ClientState state = ClientState.LOGGED_OUT;
    private String authToken = null;
    private String username = null;
    private List<GameData> lastGameList = new ArrayList<>();

    private Integer currentGameID = null;
    private ChessGame.TeamColor currentPerspective = ChessGame.TeamColor.WHITE;
    private boolean observing = false;
    private WebSocketFacade webSocket;
    private ChessGame currentGame = null;
    private Set<ChessPosition> highlightedSquares = new HashSet<>();

    public Repl(String serverUrl) {
        this.serverUrl = serverUrl;
        this.serverFacade = new ServerFacade(serverUrl);
    }

    public void run() {
        printWelcome();

        while (running) {
            try {
                printPrompt();
                String line = scanner.nextLine().trim();
                ClientResponse response = eval(line);
                if (response.message() != null && !response.message().isBlank()) {
                    System.out.println(response.message());
                }
            } catch (Exception e) {
                System.out.println("Error: unable to process command.");
            }
        }

        System.out.println("Goodbye!");
    }

    private void printWelcome() {
        System.out.println("♕ 240 Chess Client");
        System.out.println("Connected to " + serverUrl);
        System.out.println("Type 'help' to begin.");
    }

    private void printPrompt() {
        System.out.printf("[%s", state);
        if (username != null) {
            System.out.printf(":%s", username);
        }
        System.out.print("] >>> ");
    }

    public ClientResponse eval(String input) {
        if (input == null || input.isBlank()) {
            return ClientResponse.success(helpText());
        }

        String[] tokens = input.trim().split("\\s+");
        String command = tokens[0].toLowerCase(Locale.ROOT);

        return switch (state) {
            case LOGGED_OUT -> evalLoggedOut(command, tokens);
            case LOGGED_IN -> evalLoggedIn(command, tokens);
            case GAMEPLAY -> evalGameplay(command, tokens);
        };
    }

    private ClientResponse evalLoggedOut(String command, String[] tokens) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "quit", "exit" -> quit();
            case "register" -> handleRegister(tokens);
            case "login" -> handleLogin(tokens);
            default -> ClientResponse.error("Unknown command. Type 'help'.");
        };
    }

    private ClientResponse handleRegister(String[] tokens) {
        if (tokens.length != 4) {
            return ClientResponse.error("Usage: register <username> <password> <email>");
        }

        try {
            var result = serverFacade.register(tokens[1], tokens[2], tokens[3]);
            authToken = result.authToken();
            username = result.username();
            state = ClientState.LOGGED_IN;
            return ClientResponse.success("Registered and logged in as " + username + ".");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleLogin(String[] tokens) {
        if (tokens.length != 3) {
            return ClientResponse.error("Usage: login <username> <password>");
        }

        try {
            var result = serverFacade.login(tokens[1], tokens[2]);
            authToken = result.authToken();
            username = result.username();
            state = ClientState.LOGGED_IN;
            return ClientResponse.success("Logged in as " + username + ".");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse evalLoggedIn(String command, String[] tokens) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "logout" -> handleLogout(tokens);
            case "create" -> handleCreate(tokens);
            case "list" -> handleList(tokens);
            case "join" -> handleJoin(tokens);
            case "observe" -> handleObserve(tokens);
            case "quit", "exit" -> quit();
            default -> ClientResponse.error("Unknown command. Type 'help'.");
        };
    }

    private ClientResponse handleLogout(String[] tokens) {
        if (tokens.length != 1) {
            return ClientResponse.error("Usage: logout");
        }

        try {
            serverFacade.logout(authToken);
            clearSession();
            return ClientResponse.success("Logged out.");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleCreate(String[] tokens) {
        if (tokens.length < 2) {
            return ClientResponse.error("Usage: create <game name>");
        }

        String gameName = joinTokens(tokens, 1);

        try {
            var result = serverFacade.createGame(authToken, gameName);
            return ClientResponse.success("Game created (ID: " + result.gameID() + ")");
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleList(String[] tokens) {
        if (tokens.length != 1) {
            return ClientResponse.error("Usage: list");
        }

        try {
            var result = serverFacade.listGames(authToken);
            lastGameList = result.games() == null ? new ArrayList<>() : result.games();

            if (lastGameList.isEmpty()) {
                return ClientResponse.success("No games available.");
            }

            StringBuilder output = new StringBuilder();
            for (int i = 0; i < lastGameList.size(); i++) {
                var g = lastGameList.get(i);
                String white = g.whiteUsername() == null ? "-" : g.whiteUsername();
                String black = g.blackUsername() == null ? "-" : g.blackUsername();

                output.append(String.format(
                        "%d: %s (White: %s, Black: %s)%n",
                        i + 1,
                        g.gameName(),
                        white,
                        black
                ));
            }

            return ClientResponse.success(output.toString().trim());
        } catch (ResponseException e) {
            return ClientResponse.error(e.getMessage());
        }
    }

    private ClientResponse handleJoin(String[] tokens) {
        if (tokens.length != 3) {
            return ClientResponse.error("Usage: join <number> <WHITE|BLACK>");
        }

        try {
            int index = Integer.parseInt(tokens[1]) - 1;

            if (index < 0 || index >= lastGameList.size()) {
                return ClientResponse.error("Invalid game number.");
            }

            String color = tokens[2].toUpperCase(Locale.ROOT);
            Integer gameID = lastGameList.get(index).gameID();

            serverFacade.joinGame(authToken, gameID, color);

            webSocket = new WebSocketFacade(serverUrl, this);
            UserGameCommand connectCmd = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authToken,
                    gameID
            );
            webSocket.sendCommand(connectCmd);

            currentGameID = gameID;
            currentPerspective = color.equals("BLACK")
                    ? ChessGame.TeamColor.BLACK
                    : ChessGame.TeamColor.WHITE;
            observing = false;
            highlightedSquares.clear();
            state = ClientState.GAMEPLAY;

            return ClientResponse.success("Joined game.");
        } catch (Exception e) {
            return ClientResponse.error("Failed to join game.");
        }
    }

    private ClientResponse handleObserve(String[] tokens) {
        if (tokens.length != 2) {
            return ClientResponse.error("Usage: observe <number>");
        }

        try {
            int index = Integer.parseInt(tokens[1]) - 1;

            if (index < 0 || index >= lastGameList.size()) {
                return ClientResponse.error("Invalid game number.");
            }

            Integer gameID = lastGameList.get(index).gameID();

            webSocket = new WebSocketFacade(serverUrl, this);
            UserGameCommand connectCmd = new UserGameCommand(
                    UserGameCommand.CommandType.CONNECT,
                    authToken,
                    gameID
            );
            webSocket.sendCommand(connectCmd);

            currentGameID = gameID;
            currentPerspective = ChessGame.TeamColor.WHITE;
            observing = true;
            highlightedSquares.clear();
            state = ClientState.GAMEPLAY;

            return ClientResponse.success("Observing game.");
        } catch (NumberFormatException e) {
            return ClientResponse.error("Game number must be a number.");
        } catch (Exception e) {
            return ClientResponse.error("Failed to observe game.");
        }
    }

    private ClientResponse evalGameplay(String command, String[] tokens) {
        return switch (command) {
            case "help" -> ClientResponse.success(helpText());
            case "redraw" -> ClientResponse.success(redrawBoard());
            case "leave" -> handleLeave();
            case "move" -> handleMove(tokens);
            case "resign" -> handleResign(tokens);
            case "highlight" -> handleHighlight(tokens);
            case "quit", "exit" -> quit();
            default -> ClientResponse.error("Unknown command.");
        };
    }

    private String redrawBoard() {
        if (currentGame == null) {
            return "No game loaded.";
        }
        return boardRenderer.drawBoard(currentGame, currentPerspective, highlightedSquares);
    }

    private ClientResponse handleMove(String[] tokens) {
        if (tokens.length < 3) {
            return ClientResponse.error("Usage: move <from> <to>");
        }

        if (webSocket == null) {
            return ClientResponse.error("Not connected to a game.");
        }

        try {
            ChessPosition from = parsePosition(tokens[1]);
            ChessPosition to = parsePosition(tokens[2]);

            ChessMove move = new ChessMove(from, to, null);

            MakeMoveCommand cmd = new MakeMoveCommand(
                    authToken,
                    currentGameID,
                    move
            );

            webSocket.sendCommand(cmd);

            return ClientResponse.success("Move sent.");
        } catch (Exception e) {
            return ClientResponse.error("Invalid move format.");
        }
    }

    private ClientResponse handleHighlight(String[] tokens) {
        if (tokens.length != 2) {
            return ClientResponse.error("Usage: highlight <square>");
        }

        if (currentGame == null) {
            return ClientResponse.error("No game loaded.");
        }

        try {
            ChessPosition position = parsePosition(tokens[1]);
            var piece = currentGame.getBoard().getPiece(position);

            if (piece == null) {
                return ClientResponse.error("No piece at that square.");
            }

            var validMoves = currentGame.validMoves(position);
            highlightedSquares = new HashSet<>();
            highlightedSquares.add(position);

            if (validMoves != null) {
                for (ChessMove move : validMoves) {
                    highlightedSquares.add(move.getEndPosition());
                }
            }

            return ClientResponse.success(boardRenderer.drawBoard(currentGame, currentPerspective, highlightedSquares));
        } catch (Exception e) {
            return ClientResponse.error("Invalid square.");
        }
    }

    private ChessPosition parsePosition(String input) {
        if (input.length() != 2) {
            throw new IllegalArgumentException();
        }

        char file = input.toLowerCase().charAt(0);
        char rank = input.charAt(1);

        int col = file - 'a' + 1;
        int row = rank - '0';

        if (col < 1 || col > 8 || row < 1 || row > 8) {
            throw new IllegalArgumentException();
        }

        return new ChessPosition(row, col);
    }

    private ClientResponse handleLeave() {
        if (webSocket != null && currentGameID != null) {
            try {
                UserGameCommand leaveCmd = new UserGameCommand(
                        UserGameCommand.CommandType.LEAVE,
                        authToken,
                        currentGameID
                );
                webSocket.sendCommand(leaveCmd);
            } catch (Exception e) {
                return ClientResponse.error("Failed to leave game.");
            }
        }

        currentGameID = null;
        currentGame = null;
        observing = false;
        highlightedSquares.clear();
        webSocket = null;
        state = ClientState.LOGGED_IN;
        return ClientResponse.success("Left game.");
    }

    private ClientResponse handleResign(String[] tokens) {
        if (tokens.length != 1) {
            return ClientResponse.error("Usage: resign");
        }

        if (webSocket == null || currentGameID == null) {
            return ClientResponse.error("Not connected to a game.");
        }

        try {
            UserGameCommand resignCmd = new UserGameCommand(
                    UserGameCommand.CommandType.RESIGN,
                    authToken,
                    currentGameID
            );
            webSocket.sendCommand(resignCmd);
            return ClientResponse.success("Resign request sent.");
        } catch (Exception e) {
            return ClientResponse.error("Failed to resign.");
        }
    }

    private ClientResponse quit() {
        running = false;
        return ClientResponse.success("Exiting...");
    }

    private void clearSession() {
        authToken = null;
        username = null;
        lastGameList = new ArrayList<>();
        currentGameID = null;
        currentGame = null;
        observing = false;
        highlightedSquares.clear();
        webSocket = null;
        state = ClientState.LOGGED_OUT;
    }

    private String joinTokens(String[] tokens, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < tokens.length; i++) {
            if (i > start) {
                sb.append(" ");
            }
            sb.append(tokens[i]);
        }
        return sb.toString();
    }

    private String helpText() {
        return switch (state) {
            case LOGGED_OUT -> """
                    help
                    register <username> <password> <email>
                    login <username> <password>
                    quit
                    """;
            case LOGGED_IN -> """
                    help
                    logout
                    create <game name>
                    list
                    join <number> <WHITE|BLACK>
                    observe <number>
                    quit
                    """;
            case GAMEPLAY -> """
                    help
                    redraw
                    move <from> <to>
                    highlight <square>
                    resign
                    leave
                    quit
                    """;
        };
    }

    @Override
    public void notify(ServerMessage message) {
        System.out.println();

        switch (message.getServerMessageType()) {
            case LOAD_GAME -> {
                LoadGameMessage loadGameMessage = (LoadGameMessage) message;
                currentGame = loadGameMessage.getGame();
                highlightedSquares.clear();
                System.out.println(boardRenderer.drawBoard(currentGame, currentPerspective, highlightedSquares));
            }
            case NOTIFICATION -> {
                NotificationMessage notificationMessage = (NotificationMessage) message;
                System.out.println(notificationMessage.getMessage());
            }
            case ERROR -> {
                ErrorMessage errorMessage = (ErrorMessage) message;
                System.out.println(errorMessage.getErrorMessage());
            }
        }

        printPrompt();
    }
}