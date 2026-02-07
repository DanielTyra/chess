package chess;

import java.util.Collection;
import java.util.Objects;
import java.util.ArrayList;

/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private TeamColor teamTurn;
    private ChessBoard board;

    public ChessGame() {
        teamTurn = TeamColor.WHITE;
        board = new ChessBoard();
        board.resetBoard();
    }

    /**
     * @return Which team's turn it is
     */
    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    /**
     * Set's which teams turn it is
     *
     * @param team the team whose turn it is
     */
    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    /**
     * Enum identifying the 2 possible teams in a chess game
     */
    public enum TeamColor {
        WHITE,
        BLACK
    }

    /**
     * Gets a valid moves for a piece at the given location
     *
     * @param startPosition the piece to get valid moves for
     * @return Set of valid moves for requested piece, or null if no piece at
     * startPosition
     */
    public Collection<ChessMove> validMoves(ChessPosition startPosition) {
        ChessPiece piece = board.getPiece(startPosition);

        if (piece == null) {
            return null;
        }

        Collection<ChessMove> pseudoMoves = piece.pieceMoves(board, startPosition);
        java.util.Collection<ChessMove> legalMoves = new java.util.ArrayList<>();

        for (ChessMove move : pseudoMoves) {

            ChessBoard copy = new ChessBoard();
            for (int r = 1; r <= 8; r++) {
                for (int c = 1; c <= 8; c++) {
                    ChessPosition pos = new ChessPosition(r, c);
                    copy.addPiece(pos, board.getPiece(pos));
                }
            }

            ChessPiece moving = copy.getPiece(move.getStartPosition());
            copy.addPiece(move.getStartPosition(), null);

            if (move.getPromotionPiece() != null) {
                copy.addPiece(move.getEndPosition(),
                        new ChessPiece(moving.getTeamColor(), move.getPromotionPiece()));
            } else {
                copy.addPiece(move.getEndPosition(), moving);
            }

            boolean leavesKingInCheck = false;

            // Find our king on the copied board
            ChessPosition kingPosition = null;
            for (int r = 1; r <= 8; r++) {
                for (int c = 1; c <= 8; c++) {
                    ChessPosition pos = new ChessPosition(r, c);
                    ChessPiece p = copy.getPiece(pos);
                    if (p != null &&
                            p.getTeamColor() == piece.getTeamColor() &&
                            p.getPieceType() == ChessPiece.PieceType.KING) {
                        kingPosition = pos;
                    }
                }
            }

            // If king exists, see if enemy attacks it
            if (kingPosition != null) {
                TeamColor enemy = (piece.getTeamColor() == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;

                for (int r = 1; r <= 8 && !leavesKingInCheck; r++) {
                    for (int c = 1; c <= 8 && !leavesKingInCheck; c++) {
                        ChessPosition pos = new ChessPosition(r, c);
                        ChessPiece enemyPiece = copy.getPiece(pos);
                        if (enemyPiece != null && enemyPiece.getTeamColor() == enemy) {
                            for (ChessMove enemyMove : enemyPiece.pieceMoves(copy, pos)) {
                                if (enemyMove.getEndPosition().equals(kingPosition)) {
                                    leavesKingInCheck = true;
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (!leavesKingInCheck) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    /**
     * Makes a move in a chess game
     *
     * @param move chess move to perform
     * @throws InvalidMoveException if move is invalid
     */
    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null) throw new InvalidMoveException("null move");

        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);

        if (piece == null) throw new InvalidMoveException("no piece");

        if (piece.getTeamColor() != teamTurn)
            throw new InvalidMoveException("wrong turn");

        Collection<ChessMove> legal = validMoves(start);

        boolean ok = false;
        for (ChessMove m : legal) {
            if (m.equals(move)) {
                ok = true;
                break;
            }
        }

        if (!ok) throw new InvalidMoveException("illegal");

        board.addPiece(start, null);

        if (move.getPromotionPiece() != null) {
            board.addPiece(move.getEndPosition(), new ChessPiece(piece.getTeamColor(), move.getPromotionPiece()));
        } else {
            board.addPiece(move.getEndPosition(), piece);
        }

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }



    /**
     * Determines if the given team is in check
     *
     * @param teamColor which team to check for check
     * @return True if the specified team is in check
     */
    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = null;

        for (int r = 1; r <= 8; r++){ //finding the king's position on the board
            for (int c = 1; c <= 8; c++){
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);

                if (piece != null && piece.getTeamColor() == teamColor && piece.getPieceType() == ChessPiece.PieceType.KING){
                    kingPosition = position;
                }
            }
        }

        if (kingPosition == null) return false;

        TeamColor enemy = (teamColor == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE; //declares the enemy color as the opposite of the king

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);

                if (piece != null && piece.getTeamColor() == enemy) {
                    for (ChessMove m : piece.pieceMoves(board, position)) {
                        if (m.getEndPosition().equals(kingPosition)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Determines if the given team is in checkmate
     *
     * @param teamColor which team to check for checkmate
     * @return True if the specified team is in checkmate
     */
    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) return false;

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition position = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(position);

                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(position);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Determines if the given team is in stalemate, which here is defined as having
     * no valid moves while not in check.
     *
     * @param teamColor which team to check for stalemate
     * @return True if the specified team is in stalemate, otherwise false
     */
    public boolean isInStalemate(TeamColor teamColor) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
    }

    /**
     * Gets the current chessboard
     *
     * @return the chessboard
     */
    public ChessBoard getBoard() {
        return board;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessGame chessGame = (ChessGame) o;
        return teamTurn == chessGame.teamTurn && Objects.equals(board, chessGame.board);
    }

    @Override
    public int hashCode() {
        return Objects.hash(teamTurn, board);
    }
}
