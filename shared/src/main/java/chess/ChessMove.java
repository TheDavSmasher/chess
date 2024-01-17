package chess;

/**
 * Represents moving a chess piece on a chessboard
 * <p>
 * Note: You can add to this class, but you may not alter
 * signature of the existing methods.
 */
public class ChessMove {
    private final ChessPosition startPosition;
    private final ChessPosition endPosition;
    private final ChessPiece.PieceType promotionPiece;

    public ChessMove(ChessPosition startPosition, ChessPosition endPosition,
                     ChessPiece.PieceType promotionPiece) {
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.promotionPiece = promotionPiece;
    }

    /**
     * @return ChessPosition of starting location
     */
    public ChessPosition getStartPosition() {
        return startPosition;
    }

    /**
     * @return ChessPosition of ending location
     */
    public ChessPosition getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the type of piece to promote a pawn to if pawn promotion is part of this
     * chess move
     *
     * @return Type of piece to promote a pawn to, or null if no promotion
     */
    public ChessPiece.PieceType getPromotionPiece() {
        if (endPosition.getRow() == 0 || endPosition.getRow() == 7) {
            return ChessPiece.PieceType.QUEEN; //FIXME only set like this for the moment.
        }
        return null;
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        builder.append(startPosition);
        builder.append(" -> ");
        builder.append(endPosition);
        if (getPromotionPiece() != null) {
            builder.append('(');
            builder.append(promotionPiece);
            builder.append(')');
        }
        builder.append(']');
        return builder.toString();
    }
}
