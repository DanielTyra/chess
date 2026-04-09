package client;

import chess.ChessBoard;
import chess.ChessGame;
import chess.ChessPiece;
import chess.ChessPosition;

import java.util.Set;

public class ChessBoardRenderer {

    private static final String EMPTY_TILE = EscapeSequences.EMPTY;

    public String drawBoard(ChessGame game, ChessGame.TeamColor perspective) {
        return drawBoard(game, perspective, Set.of());
    }

    public String drawBoard(ChessGame game, ChessGame.TeamColor perspective, Set<ChessPosition> highlightedSquares) {
        ChessBoard board = game.getBoard();

        StringBuilder out = new StringBuilder();

        appendFileLabels(out, perspective);

        if (perspective == ChessGame.TeamColor.WHITE) {
            for (int row = 8; row >= 1; row--) {
                appendBoardRow(out, board, row, true, highlightedSquares);
            }
        } else {
            for (int row = 1; row <= 8; row++) {
                appendBoardRow(out, board, row, false, highlightedSquares);
            }
        }

        appendFileLabels(out, perspective);
        return out.toString();
    }

    private void appendFileLabels(StringBuilder out, ChessGame.TeamColor perspective) {
        out.append(EscapeSequences.SET_BG_COLOR_BLACK)
                .append(EscapeSequences.SET_TEXT_COLOR_WHITE)
                .append("   ");

        if (perspective == ChessGame.TeamColor.WHITE) {
            for (char file = 'a'; file <= 'h'; file++) {
                out.append(" ").append(file).append("\u2003");
            }
        } else {
            for (char file = 'h'; file >= 'a'; file--) {
                out.append(" ").append(file).append("\u2003");
            }
        }

        out.append("   ")
                .append(EscapeSequences.RESET_BG_COLOR)
                .append(EscapeSequences.RESET_TEXT_COLOR)
                .append("\n");
    }

    private void appendBoardRow(StringBuilder out, ChessBoard board, int row, boolean whitePerspective,
                                Set<ChessPosition> highlightedSquares) {
        appendRankLabel(out, row);

        if (whitePerspective) {
            for (int col = 1; col <= 8; col++) {
                appendSquare(out, board, row, col, highlightedSquares);
            }
        } else {
            for (int col = 8; col >= 1; col--) {
                appendSquare(out, board, row, col, highlightedSquares);
            }
        }

        appendRankLabel(out, row);
        out.append(EscapeSequences.RESET_BG_COLOR)
                .append(EscapeSequences.RESET_TEXT_COLOR)
                .append("\n");
    }

    private void appendRankLabel(StringBuilder out, int row) {
        out.append(EscapeSequences.SET_BG_COLOR_BLACK)
                .append(EscapeSequences.SET_TEXT_COLOR_WHITE)
                .append(" ")
                .append(row)
                .append(" ")
                .append(EscapeSequences.RESET_BG_COLOR)
                .append(EscapeSequences.RESET_TEXT_COLOR);
    }

    private void appendSquare(StringBuilder out, ChessBoard board, int row, int col, Set<ChessPosition> highlightedSquares) {
        ChessPosition position = new ChessPosition(row, col);

        String bgColor;
        if (highlightedSquares.contains(position)) {
            bgColor = EscapeSequences.SET_BG_COLOR_GREEN;
        } else {
            boolean lightSquare = (row + col) % 2 == 0;
            bgColor = lightSquare
                    ? EscapeSequences.SET_BG_COLOR_LIGHT_GREY
                    : EscapeSequences.SET_BG_COLOR_DARK_GREY;
        }

        ChessPiece piece = board.getPiece(position);

        out.append(bgColor);

        if (piece == null) {
            out.append(EMPTY_TILE);
        } else {
            out.append(getPieceString(piece));
        }

        out.append(EscapeSequences.RESET_BG_COLOR)
                .append(EscapeSequences.RESET_TEXT_COLOR);
    }

    private String getPieceString(ChessPiece piece) {
        return switch (piece.getTeamColor()) {
            case WHITE -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_KING;
                case QUEEN -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_QUEEN;
                case BISHOP -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_BISHOP;
                case KNIGHT -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_KNIGHT;
                case ROOK -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_ROOK;
                case PAWN -> EscapeSequences.SET_TEXT_COLOR_BLUE + EscapeSequences.WHITE_PAWN;
            };
            case BLACK -> switch (piece.getPieceType()) {
                case KING -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_KING;
                case QUEEN -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_QUEEN;
                case BISHOP -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_BISHOP;
                case KNIGHT -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_KNIGHT;
                case ROOK -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_ROOK;
                case PAWN -> EscapeSequences.SET_TEXT_COLOR_RED + EscapeSequences.BLACK_PAWN;
            };
        };
    }
}