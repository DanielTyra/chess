package chess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Represents a single chess piece
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessPiece {

    private final ChessGame.TeamColor pieceColor;
    private final PieceType type;

    public ChessPiece(ChessGame.TeamColor pieceColor, ChessPiece.PieceType type) {
        this.pieceColor = pieceColor;
        this.type = type;
    }

    /**
     * The various different chess piece options
     */
    public enum PieceType {
        KING,
        QUEEN,
        BISHOP,
        KNIGHT,
        ROOK,
        PAWN
    }

    /**
     * @return Which team this chess piece belongs to
     */
    public ChessGame.TeamColor getTeamColor() {
        return pieceColor;
    }

    /**
     * @return which type of chess piece this piece is
     */
    public PieceType getPieceType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ChessPiece piece = (ChessPiece) o;
        return pieceColor == piece.pieceColor && type == piece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pieceColor, type);
    }

    /**
     * Calculates all the positions a chess piece can move to
     * Does not take into account moves that are illegal due to leaving the king in
     * danger
     *
     * @return Collection of valid moves
     */
    public Collection<ChessMove> pieceMoves(ChessBoard board, ChessPosition myPosition) {
        ChessPiece piece = board.getPiece(myPosition);
        Collection<ChessMove> moves = new ArrayList<>();
        if (piece.getPieceType() == PieceType.BISHOP) {
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}};
            for (int[]direction: directions) {
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                while (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) {
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) {
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                        break;
                    } else if (targetPiece.getTeamColor() == pieceColor) {
                        break;
                    }
                    r+=direction[0];
                    c+=direction[1];
                }
            }


        }


        return moves;
    }
}
