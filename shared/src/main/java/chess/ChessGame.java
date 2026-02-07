package chess;

import java.util.Collection;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;


/**
 * For a class that can manage a chess game, making moves on a board
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessGame {

    private TeamColor teamTurn;
    private ChessBoard board;
    private boolean whiteKingMoved, blackKingMoved;
    private boolean whiteARookMoved, whiteHRookMoved;
    private boolean blackARookMoved, blackHRookMoved;
    private ChessPosition enPassantTarget;


    public ChessGame() {
        teamTurn = TeamColor.WHITE;
        board = new ChessBoard();
        board.resetBoard();
        whiteKingMoved = blackKingMoved = false;
        whiteARookMoved = whiteHRookMoved = false;
        blackARookMoved = blackHRookMoved = false;
        enPassantTarget = null;
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

        List<ChessMove> candidates = new ArrayList<>(piece.pieceMoves(board, startPosition));


        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            addEnPassantMove(candidates, startPosition, piece.getTeamColor());
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            addCastlingMoves(candidates, startPosition, piece.getTeamColor());
        }

        Collection<ChessMove> legalMoves = new ArrayList<>();

        for (ChessMove move : candidates) {

            // Copy board (your style, inline)
            ChessBoard copy = new ChessBoard();
            for (int r = 1; r <= 8; r++) {
                for (int c = 1; c <= 8; c++) {
                    ChessPosition pos = new ChessPosition(r, c);
                    copy.addPiece(pos, board.getPiece(pos));
                }
            }

            applyMoveWithSpecialRules(copy, move);

            boolean leavesKingInCheck = false;

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

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (piece.getTeamColor() == TeamColor.WHITE) whiteKingMoved = true;
            else blackKingMoved = true;
        } else if (piece.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                if (start.getRow() == 1 && start.getColumn() == 1) whiteARookMoved = true;
                if (start.getRow() == 1 && start.getColumn() == 8) whiteHRookMoved = true;
            } else {
                if (start.getRow() == 8 && start.getColumn() == 1) blackARookMoved = true;
                if (start.getRow() == 8 && start.getColumn() == 8) blackHRookMoved = true;
            }
        }

        ChessPosition nextEnPassant = null;
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            int sr = start.getRow();
            int er = move.getEndPosition().getRow();
            if (Math.abs(er - sr) == 2) {
                int mid = (sr + er) / 2;
                nextEnPassant = new ChessPosition(mid, start.getColumn());
            }
        }

        applyMoveWithSpecialRules(board, move);

        enPassantTarget = nextEnPassant;

        teamTurn = (teamTurn == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private TeamColor opposite(TeamColor t) {
        return (t == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private boolean isSquareAttackedBy(ChessBoard b, ChessPosition square, TeamColor attackerColor) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r, c);
                ChessPiece p = b.getPiece(from);
                if (p == null || p.getTeamColor() != attackerColor) continue;

                if (p.getPieceType() == ChessPiece.PieceType.PAWN) {
                    int dir = (attackerColor == TeamColor.WHITE) ? 1 : -1;
                    int ar = r + dir;
                    if (ar >= 1 && ar <= 8) {
                        if (c - 1 >= 1 && square.equals(new ChessPosition(ar, c - 1))) return true;
                        if (c + 1 <= 8 && square.equals(new ChessPosition(ar, c + 1))) return true;
                    }
                } else {
                    for (ChessMove m : p.pieceMoves(b, from)) {
                        if (square.equals(m.getEndPosition())) return true;
                    }
                }
            }
        }
        return false;
    }

    private void addEnPassantMove(List<ChessMove> moves, ChessPosition pawnPos, TeamColor pawnColor) {
        if (enPassantTarget == null) return;

        int dir = (pawnColor == TeamColor.WHITE) ? 1 : -1;

        if (enPassantTarget.getRow() != pawnPos.getRow() + dir) return;
        if (Math.abs(enPassantTarget.getColumn() - pawnPos.getColumn()) != 1) return;

        ChessPosition adjacent = new ChessPosition(pawnPos.getRow(), enPassantTarget.getColumn());
        ChessPiece adj = board.getPiece(adjacent);
        if (adj == null) return;
        if (adj.getPieceType() != ChessPiece.PieceType.PAWN) return;
        if (adj.getTeamColor() == pawnColor) return;

        moves.add(new ChessMove(pawnPos, enPassantTarget, null));
    }

    private void addCastlingMoves(List<ChessMove> moves, ChessPosition kingPos, TeamColor kingColor) {
        // must be on starting square and king must not have moved
        if (kingColor == TeamColor.WHITE) {
            if (!(kingPos.getRow() == 1 && kingPos.getColumn() == 5)) return;
            if (whiteKingMoved) return;
        } else {
            if (!(kingPos.getRow() == 8 && kingPos.getColumn() == 5)) return;
            if (blackKingMoved) return;
        }

        // cannot castle out of check
        if (isInCheck(kingColor)) return;

        int row = kingPos.getRow();
        TeamColor enemy = opposite(kingColor);

        // kingside: rook at h, squares f/g empty, and f/g not attacked
        if (canCastleKingside(row, kingColor)) {
            ChessPosition f = new ChessPosition(row, 6);
            ChessPosition g = new ChessPosition(row, 7);
            if (!isSquareAttackedBy(board, f, enemy) && !isSquareAttackedBy(board, g, enemy)) {
                moves.add(new ChessMove(kingPos, g, null));
            }
        }

        // queenside: rook at a, squares b/c/d empty, and d/c not attacked
        if (canCastleQueenside(row, kingColor)) {
            ChessPosition d = new ChessPosition(row, 4);
            ChessPosition c = new ChessPosition(row, 3);
            if (!isSquareAttackedBy(board, d, enemy) && !isSquareAttackedBy(board, c, enemy)) {
                moves.add(new ChessMove(kingPos, c, null));
            }
        }
    }

    private boolean canCastleKingside(int row, TeamColor kingColor) {
        ChessPiece rook = board.getPiece(new ChessPosition(row, 8));
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != kingColor) return false;

        if (kingColor == TeamColor.WHITE && whiteHRookMoved) return false;
        if (kingColor == TeamColor.BLACK && blackHRookMoved) return false;

        if (board.getPiece(new ChessPosition(row, 6)) != null) return false;
        if (board.getPiece(new ChessPosition(row, 7)) != null) return false;

        return true;
    }

    private boolean canCastleQueenside(int row, TeamColor kingColor) {
        ChessPiece rook = board.getPiece(new ChessPosition(row, 1));
        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != kingColor) return false;

        if (kingColor == TeamColor.WHITE && whiteARookMoved) return false;
        if (kingColor == TeamColor.BLACK && blackARookMoved) return false;

        if (board.getPiece(new ChessPosition(row, 2)) != null) return false;
        if (board.getPiece(new ChessPosition(row, 3)) != null) return false;
        if (board.getPiece(new ChessPosition(row, 4)) != null) return false;

        return true;
    }

    private void applyMoveWithSpecialRules(ChessBoard b, ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();

        ChessPiece moving = b.getPiece(start);
        if (moving == null) return;

        boolean isCastle = moving.getPieceType() == ChessPiece.PieceType.KING
                && start.getRow() == end.getRow()
                && Math.abs(end.getColumn() - start.getColumn()) == 2;

        boolean isEnPassant = moving.getPieceType() == ChessPiece.PieceType.PAWN
                && enPassantTarget != null
                && end.equals(enPassantTarget)
                && b.getPiece(end) == null
                && start.getColumn() != end.getColumn();

        // clear start
        b.addPiece(start, null);

        // en passant capture removes pawn behind target
        if (isEnPassant) {
            int capturedRow = (moving.getTeamColor() == TeamColor.WHITE) ? end.getRow() - 1 : end.getRow() + 1;
            b.addPiece(new ChessPosition(capturedRow, end.getColumn()), null);
        }

        // place piece / promotion
        if (move.getPromotionPiece() != null) {
            b.addPiece(end, new ChessPiece(moving.getTeamColor(), move.getPromotionPiece()));
        } else {
            b.addPiece(end, moving);
        }

        // rook movement for castling
        if (isCastle) {
            int row = start.getRow();
            if (end.getColumn() == 7) { // kingside
                ChessPosition rookFrom = new ChessPosition(row, 8);
                ChessPosition rookTo = new ChessPosition(row, 6);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            } else if (end.getColumn() == 3) { // queenside
                ChessPosition rookFrom = new ChessPosition(row, 1);
                ChessPosition rookTo = new ChessPosition(row, 4);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            }
        }
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
        if (isInCheck(teamColor)) return false;

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece p = board.getPiece(pos);

                if (p != null && p.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(pos);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Sets this game's chessboard with a given board
     *
     * @param board the new board to use
     */
    public void setBoard(ChessBoard board) {
        this.board = board;
        whiteKingMoved = blackKingMoved = false;
        whiteARookMoved = whiteHRookMoved = false;
        blackARookMoved = blackHRookMoved = false;
        enPassantTarget = null;
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
