package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

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

    public TeamColor getTeamTurn() {
        return teamTurn;
    }

    public void setTeamTurn(TeamColor team) {
        teamTurn = team;
    }

    public enum TeamColor {
        WHITE,
        BLACK
    }

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
            ChessBoard copy = copyBoard(board);
            applyMoveWithSpecialRules(copy, move);

            ChessPosition kingPosition = findKing(copy, piece.getTeamColor());
            if (kingPosition == null) {
                return null;
            }

            TeamColor enemy = opposite(piece.getTeamColor());
            boolean leavesKingInCheck = isSquareAttackedBy(copy, kingPosition, enemy);

            if (!leavesKingInCheck) {
                legalMoves.add(move);
            }
        }

        return legalMoves;
    }

    public void makeMove(ChessMove move) throws InvalidMoveException {
        if (move == null) {
            throw new InvalidMoveException("null move");
        }

        ChessPosition start = move.getStartPosition();
        ChessPiece piece = board.getPiece(start);

        if (piece == null) {
            throw new InvalidMoveException("no piece");
        }

        if (piece.getTeamColor() != teamTurn) {
            throw new InvalidMoveException("wrong turn");
        }

        Collection<ChessMove> legal = validMoves(start);
        boolean ok = false;

        for (ChessMove m : legal) {
            if (m.equals(move)) {
                ok = true;
                break;
            }
        }

        if (!ok) {
            throw new InvalidMoveException("illegal");
        }

        if (piece.getPieceType() == ChessPiece.PieceType.KING) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                whiteKingMoved = true;
            } else {
                blackKingMoved = true;
            }
        } else if (piece.getPieceType() == ChessPiece.PieceType.ROOK) {
            if (piece.getTeamColor() == TeamColor.WHITE) {
                if (start.getRow() == 1 && start.getColumn() == 1) {
                    whiteARookMoved = true;
                }
                if (start.getRow() == 1 && start.getColumn() == 8) {
                    whiteHRookMoved = true;
                }
            } else {
                if (start.getRow() == 8 && start.getColumn() == 1) {
                    blackARookMoved = true;
                }
                if (start.getRow() == 8 && start.getColumn() == 8) {
                    blackHRookMoved = true;
                }
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
        teamTurn = opposite(teamTurn);
    }

    private TeamColor opposite(TeamColor t) {
        return (t == TeamColor.WHITE) ? TeamColor.BLACK : TeamColor.WHITE;
    }

    private ChessBoard copyBoard(ChessBoard original) {
        ChessBoard copy = new ChessBoard();
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                copy.addPiece(pos, original.getPiece(pos));
            }
        }
        return copy;
    }

    private ChessPosition findKing(ChessBoard b, TeamColor color) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = b.getPiece(pos);

                if (piece != null
                        && piece.getTeamColor() == color
                        && piece.getPieceType() == ChessPiece.PieceType.KING) {
                    return pos;
                }
            }
        }
        return null;
    }

    private boolean isSquareAttackedBy(ChessBoard b, ChessPosition square, TeamColor attackerColor) {
        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition from = new ChessPosition(r, c);
                ChessPiece piece = b.getPiece(from);

                if (piece == null || piece.getTeamColor() != attackerColor) {
                    continue;
                }

                if (attacksSquare(b, from, piece, square, attackerColor)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attacksSquare(ChessBoard b, ChessPosition from, ChessPiece piece,
                                  ChessPosition target, TeamColor attackerColor) {
        if (piece.getPieceType() == ChessPiece.PieceType.PAWN) {
            return pawnAttacksSquare(from, target, attackerColor);
        }

        return nonPawnAttacksSquare(b, from, piece, target);
    }

    private boolean pawnAttacksSquare(ChessPosition from, ChessPosition target, TeamColor attackerColor) {
        int dir = (attackerColor == TeamColor.WHITE) ? 1 : -1;
        int attackRow = from.getRow() + dir;

        if (attackRow < 1 || attackRow > 8) {
            return false;
        }

        ChessPosition leftAttack = new ChessPosition(attackRow, from.getColumn() - 1);
        ChessPosition rightAttack = new ChessPosition(attackRow, from.getColumn() + 1);

        return (isValidBoardPosition(leftAttack) && leftAttack.equals(target))
                || (isValidBoardPosition(rightAttack) && rightAttack.equals(target));
    }

    private boolean nonPawnAttacksSquare(ChessBoard b, ChessPosition from, ChessPiece piece,
                                         ChessPosition target) {
        for (ChessMove move : piece.pieceMoves(b, from)) {
            if (target.equals(move.getEndPosition())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidBoardPosition(ChessPosition position) {
        return position.getRow() >= 1 && position.getRow() <= 8
                && position.getColumn() >= 1 && position.getColumn() <= 8;
    }

    private void addEnPassantMove(List<ChessMove> moves, ChessPosition pawnPos, TeamColor pawnColor) {
        if (enPassantTarget == null) {
            return;
        }

        int dir = (pawnColor == TeamColor.WHITE) ? 1 : -1;

        if (enPassantTarget.getRow() != pawnPos.getRow() + dir) {
            return;
        }

        if (Math.abs(enPassantTarget.getColumn() - pawnPos.getColumn()) != 1) {
            return;
        }

        ChessPosition adjacent = new ChessPosition(pawnPos.getRow(), enPassantTarget.getColumn());
        ChessPiece adj = board.getPiece(adjacent);

        if (adj == null) {
            return;
        }

        if (adj.getPieceType() != ChessPiece.PieceType.PAWN) {
            return;
        }

        if (adj.getTeamColor() == pawnColor) {
            return;
        }

        moves.add(new ChessMove(pawnPos, enPassantTarget, null));
    }

    private void addCastlingMoves(List<ChessMove> moves, ChessPosition kingPos, TeamColor kingColor) {
        if (kingColor == TeamColor.WHITE) {
            if (!(kingPos.getRow() == 1 && kingPos.getColumn() == 5)) {
                return;
            }
            if (whiteKingMoved) {
                return;
            }
        } else {
            if (!(kingPos.getRow() == 8 && kingPos.getColumn() == 5)) {
                return;
            }
            if (blackKingMoved) {
                return;
            }
        }

        if (isInCheck(kingColor)) {
            return;
        }

        int row = kingPos.getRow();
        TeamColor enemy = opposite(kingColor);

        if (canCastleKingside(row, kingColor)) {
            ChessPosition f = new ChessPosition(row, 6);
            ChessPosition g = new ChessPosition(row, 7);

            if (!isSquareAttackedBy(board, f, enemy) && !isSquareAttackedBy(board, g, enemy)) {
                moves.add(new ChessMove(kingPos, g, null));
            }
        }

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

        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != kingColor) {
            return false;
        }

        if (kingColor == TeamColor.WHITE && whiteHRookMoved) {
            return false;
        }

        if (kingColor == TeamColor.BLACK && blackHRookMoved) {
            return false;
        }

        if (board.getPiece(new ChessPosition(row, 6)) != null) {
            return false;
        }

        if (board.getPiece(new ChessPosition(row, 7)) != null) {
            return false;
        }

        return true;
    }

    private boolean canCastleQueenside(int row, TeamColor kingColor) {
        ChessPiece rook = board.getPiece(new ChessPosition(row, 1));

        if (rook == null || rook.getPieceType() != ChessPiece.PieceType.ROOK || rook.getTeamColor() != kingColor) {
            return false;
        }

        if (kingColor == TeamColor.WHITE && whiteARookMoved) {
            return false;
        }

        if (kingColor == TeamColor.BLACK && blackARookMoved) {
            return false;
        }

        if (board.getPiece(new ChessPosition(row, 2)) != null) {
            return false;
        }

        if (board.getPiece(new ChessPosition(row, 3)) != null) {
            return false;
        }

        if (board.getPiece(new ChessPosition(row, 4)) != null) {
            return false;
        }

        return true;
    }

    private void applyMoveWithSpecialRules(ChessBoard b, ChessMove move) {
        ChessPosition start = move.getStartPosition();
        ChessPosition end = move.getEndPosition();
        ChessPiece moving = b.getPiece(start);

        if (moving == null) {
            return;
        }

        boolean isCastle = moving.getPieceType() == ChessPiece.PieceType.KING
                && start.getRow() == end.getRow()
                && Math.abs(end.getColumn() - start.getColumn()) == 2;

        boolean isEnPassant = moving.getPieceType() == ChessPiece.PieceType.PAWN
                && enPassantTarget != null
                && end.equals(enPassantTarget)
                && b.getPiece(end) == null
                && start.getColumn() != end.getColumn();

        b.addPiece(start, null);

        if (isEnPassant) {
            int capturedRow = (moving.getTeamColor() == TeamColor.WHITE) ? end.getRow() - 1 : end.getRow() + 1;
            b.addPiece(new ChessPosition(capturedRow, end.getColumn()), null);
        }

        if (move.getPromotionPiece() != null) {
            b.addPiece(end, new ChessPiece(moving.getTeamColor(), move.getPromotionPiece()));
        } else {
            b.addPiece(end, moving);
        }

        if (isCastle) {
            int row = start.getRow();

            if (end.getColumn() == 7) {
                ChessPosition rookFrom = new ChessPosition(row, 8);
                ChessPosition rookTo = new ChessPosition(row, 6);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            } else if (end.getColumn() == 3) {
                ChessPosition rookFrom = new ChessPosition(row, 1);
                ChessPosition rookTo = new ChessPosition(row, 4);
                ChessPiece rook = b.getPiece(rookFrom);
                b.addPiece(rookFrom, null);
                b.addPiece(rookTo, rook);
            }
        }
    }

    public boolean isInCheck(TeamColor teamColor) {
        ChessPosition kingPosition = findKing(board, teamColor);

        if (kingPosition == null) {
            return false;
        }

        TeamColor enemy = opposite(teamColor);
        return isSquareAttackedBy(board, kingPosition, enemy);
    }

    public boolean isInCheckmate(TeamColor teamColor) {
        if (!isInCheck(teamColor)) {
            return false;
        }

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

    public boolean isInStalemate(TeamColor teamColor) {
        if (isInCheck(teamColor)) {
            return false;
        }

        for (int r = 1; r <= 8; r++) {
            for (int c = 1; c <= 8; c++) {
                ChessPosition pos = new ChessPosition(r, c);
                ChessPiece piece = board.getPiece(pos);

                if (piece != null && piece.getTeamColor() == teamColor) {
                    Collection<ChessMove> moves = validMoves(pos);
                    if (moves != null && !moves.isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public void setBoard(ChessBoard board) {
        this.board = board;
        whiteKingMoved = blackKingMoved = false;
        whiteARookMoved = whiteHRookMoved = false;
        blackARookMoved = blackHRookMoved = false;
        enPassantTarget = null;
    }

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