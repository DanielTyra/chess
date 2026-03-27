package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

public class ChessBoardRenderer {
    private static final String RESET = "\u001B[0m";

    private static final String LIGHT_BG = "\u001B[47m";
    private static final String DARK_BG = "\u001B[100m";

    private static final String WHITE_PIECE = "\u001B[31m";
    private static final String BLACK_PIECE = "\u001B[34m";

    private static final String LABEL_BG = "\u001B[40m";
    private static final String LABEL_FG = "\u001B[37m";

    private static final String EMPTY = "   ";

    public String drawBoard(ChessGame.TeamColor perspective) {
        ChessBoard board = new ChessBoard();
        board.resetBoard();

        StringBuilder out = new StringBuilder();

        appendColumnLabels(out, perspective);

        if (perspective == ChessGame.TeamColor.BLACK) {
            for (int row = 1; row <= 8; row++) {
                appendBoardRow(out, board, row, false);
            }
        } else {
            for (int row = 8; row >= 1; row--) {
                appendBoardRow(out, board, row, true);
            }
        }

        appendColumnLabels(out, perspective);

        return out.toString();
    }

    private void appendBoardRow(StringBuilder out, ChessBoard board, int row, boolean whitePerspective) {
        appendRowLabel(out, row);

        if (whitePerspective) {
            for (int col = 1; col <= 8; col++) {
                appendSquare(out, board, row, col);
            }
        } else {
            for (int col = 8; col >= 1; col--) {
                appendSquare(out, board, row, col);
            }
        }

        appendRowLabel(out, row);
        out.append(RESET).append(System.lineSeparator());
    }

    private void appendSquare(StringBuilder out, ChessBoard board, int row, int col) {
        boolean lightSquare = (row + col) % 2 == 1;
        String bg = lightSquare ? LIGHT_BG : DARK_BG;

        ChessPiece piece = board.getPiece(new ChessPosition(row, col));

        out.append(bg);
        if (piece == null) {
            out.append(EMPTY);
        } else {
            out.append(" ").append(pieceString(piece)).append(" ");
        }
        out.append(RESET);
    }

    private String pieceString(ChessPiece piece) {
        String color = piece.getTeamColor() == ChessGame.TeamColor.WHITE ? WHITE_PIECE : BLACK_PIECE;
        return color + switch (piece.getPieceType()) {
            case KING -> "K";
            case QUEEN -> "Q";
            case BISHOP -> "B";
            case KNIGHT -> "N";
            case ROOK -> "R";
            case PAWN -> "P";
        } + RESET;
    }

    private void appendColumnLabels(StringBuilder out, ChessGame.TeamColor perspective) {
        out.append(LABEL_BG).append(LABEL_FG).append("   ");

        if (perspective == ChessGame.TeamColor.BLACK) {
            for (char c = 'h'; c >= 'a'; c--) {
                out.append(" ").append(c).append(" ");
            }
        } else {
            for (char c = 'a'; c <= 'h'; c++) {
                out.append(" ").append(c).append(" ");
            }
        }

        out.append("   ").append(RESET).append(System.lineSeparator());
    }

    private void appendRowLabel(StringBuilder out, int row) {
        out.append(LABEL_BG)
                .append(LABEL_FG)
                .append(" ")
                .append(row)
                .append(" ")
                .append(RESET);
    }
}