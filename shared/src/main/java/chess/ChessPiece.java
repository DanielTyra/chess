package chess;

import java.util.ArrayList;
import java.util.Collection;
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
            int[][] directions = {{1,1}, {1,-1}, {-1,1}, {-1,-1}}; //all directions bishop can move to
            for (int[]direction: directions) { //loop through each direction
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                while (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) { //if there is no piece on square add to possible moves
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) { //if there is an enemy on square add to possible moves but stop extra movement past
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                        break;
                    } else if (targetPiece.getTeamColor() == pieceColor) { //if there's a teammate on square stop movement in direction
                        break;
                    }
                    r+=direction[0];
                    c+=direction[1];
                }
            }
        } else if (piece.getPieceType() == PieceType.ROOK){
            int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}}; //all directions rook can move to
            for (int[]direction: directions) { //loop through each direction
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                while (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) { //if there is no piece on square add to possible moves
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) { //if there is an enemy on square add to possible moves but stop extra movement past
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                        break;
                    } else if (targetPiece.getTeamColor() == pieceColor) { //if there's a teammate on square stop movement in direction
                        break;
                    }
                    r+=direction[0];
                    c+=direction[1];
                }
            }
        } else if (piece.getPieceType() == PieceType.QUEEN) {
            int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}}; //all directions queen can move to
            for (int[]direction: directions) { //loop through each direction
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                while (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) { //if there is no piece on square add to possible moves
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) { //if there is an enemy on square add to possible moves but stop extra movement past
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                        break;
                    } else if (targetPiece.getTeamColor() == pieceColor) { //if there's a teammate on square stop movement in direction
                        break;
                    }
                    r+=direction[0];
                    c+=direction[1];
                }
            }
        } else if (piece.getPieceType() == PieceType.KNIGHT){
            int[][] directions = {{1,2}, {-1,2}, {2,1}, {2,-1}, {1,-2}, {-1,-2}, {-2,1}, {-2,-1}}; //all directions knight can move to
            for (int[]direction: directions) { //loop through each direction
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                if (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) { //if there is no piece on square add to possible moves
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) { //if there is an enemy on square add to possible moves but stop extra movement past
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    }
                }
            }
        } else if (piece.getPieceType() == PieceType.KING){
            int[][] directions = {{1,0}, {-1,0}, {0,1}, {0,-1}, {1,1}, {1,-1}, {-1,1}, {-1,-1}}; //all directions king can move to
            for (int[]direction: directions) { //loop through each direction
                int r = myPosition.getRow() + direction[0];
                int c = myPosition.getColumn() + direction[1];
                if (r >= 1 && r <= 8 && c >= 1 && c <=8) {
                    ChessPosition targetPosition = new ChessPosition(r, c);
                    ChessPiece targetPiece = board.getPiece(targetPosition);
                    if (targetPiece == null) { //if there is no piece on square add to possible moves
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    } else if (targetPiece.getTeamColor() != pieceColor) { //if there is an enemy on square add to possible moves but stop extra movement past
                        moves.add(new ChessMove(myPosition, targetPosition, null));
                    }
                }
            }
        } else if (piece.getPieceType() == PieceType.PAWN) {
            int r = myPosition.getRow();
            int c = myPosition.getColumn();
            if (piece.getTeamColor() == ChessGame.TeamColor.WHITE){ //moves for white pawn
                if (r <= 6) { //single space moves and captures
                    if (board.getPiece(new ChessPosition(r+1, c)) == null) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r + 1, c), null));
                    }
                    if (c != 1 && board.getPiece(new ChessPosition(r+1, c-1)) != null && (board.getPiece(new ChessPosition(r+1, c-1)).getTeamColor() == ChessGame.TeamColor.BLACK)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c-1), null));
                    }

                    if (c != 8 && board.getPiece(new ChessPosition(r+1, c+1)) != null && (board.getPiece(new ChessPosition(r+1, c+1)).getTeamColor() == ChessGame.TeamColor.BLACK)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c+1), null));
                    }
                }
                if (r == 2 && board.getPiece(new ChessPosition(r+2, c)) == null && board.getPiece(new ChessPosition(r+1, c)) == null) { //double space move
                    moves.add(new ChessMove(myPosition, new ChessPosition(r+2, c), null));
                }

                if (r == 7) { //promotion moves and promotion captures
                    if (board.getPiece(new ChessPosition(r+1, c)) == null) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r + 1, c), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r + 1, c), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r + 1, c), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r + 1, c), PieceType.KNIGHT));
                    }
                    if (c != 1 && board.getPiece(new ChessPosition(r+1, c-1)) != null && (board.getPiece(new ChessPosition(r+1, c-1)).getTeamColor() == ChessGame.TeamColor.BLACK)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c-1), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c-1), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c-1), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c-1), PieceType.KNIGHT));
                    }
                    if (c != 8 && board.getPiece(new ChessPosition(r+1, c+1)) != null && (board.getPiece(new ChessPosition(r+1, c+1)).getTeamColor() == ChessGame.TeamColor.BLACK)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c+1), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c+1), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c+1), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r+1, c+1), PieceType.KNIGHT));
                    }
                }
            }
            if (piece.getTeamColor() == ChessGame.TeamColor.BLACK) { //moves for black pawn
                if (r >= 3) { //single space moves and captures
                    if (board.getPiece(new ChessPosition(r - 1, c)) == null) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c), null));
                    }
                    if (c != 1 && board.getPiece(new ChessPosition(r - 1, c - 1)) != null && (board.getPiece(new ChessPosition(r - 1, c - 1)).getTeamColor() == ChessGame.TeamColor.WHITE)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c - 1), null));
                    }

                    if (c != 8 && board.getPiece(new ChessPosition(r - 1, c + 1)) != null && (board.getPiece(new ChessPosition(r - 1, c + 1)).getTeamColor() == ChessGame.TeamColor.WHITE)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c + 1), null));
                    }
                }
                if (r == 7 && board.getPiece(new ChessPosition(r - 2, c)) == null && board.getPiece(new ChessPosition(r - 1, c)) == null) { //double space move
                    moves.add(new ChessMove(myPosition, new ChessPosition(r - 2, c), null));
                }

                if (r == 2) { //promotion moves and promotion captures
                    if (board.getPiece(new ChessPosition(r - 1, c)) == null) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c), PieceType.KNIGHT));
                    }
                    if (c != 1 && board.getPiece(new ChessPosition(r - 1, c - 1)) != null && (board.getPiece(new ChessPosition(r - 1, c - 1)).getTeamColor() == ChessGame.TeamColor.WHITE)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c - 1), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c - 1), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c - 1), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c - 1), PieceType.KNIGHT));
                    }
                    if (c != 8 && board.getPiece(new ChessPosition(r - 1, c + 1)) != null && (board.getPiece(new ChessPosition(r - 1, c + 1)).getTeamColor() == ChessGame.TeamColor.WHITE)) {
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c + 1), PieceType.ROOK));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c + 1), PieceType.BISHOP));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c + 1), PieceType.QUEEN));
                        moves.add(new ChessMove(myPosition, new ChessPosition(r - 1, c + 1), PieceType.KNIGHT));
                    }
                }
            }
        }
        return moves;
    }
}
