package backgammon.model;

/**
 * Represents the two players in a Backgammon game.
 * WHITE moves from point 24 toward point 1 (counterclockwise).
 * BLACK moves from point 1 toward point 24 (clockwise).
 *
 * Convention on the board array (0-based, index 0 = point 1):
 *   Positive values = WHITE pieces
 *   Negative values = BLACK pieces
 */
public enum Player {
    WHITE, BLACK;

    /**
     * Returns the opponent of this player.
     */
    public Player opponent() {
        return this == WHITE ? BLACK : WHITE;
    }

    /**
     * Returns the sign used on the board array.
     * WHITE = +1, BLACK = -1.
     */
    public int sign() {
        return this == WHITE ? 1 : -1;
    }

    /**
     * Returns a display-friendly name.
     */
    public String displayName() {
        return this == WHITE ? "White (Human)" : "Black (AI)";
    }

    /**
     * Returns a single character for compact board display.
     */
    public char symbol() {
        return this == WHITE ? 'O' : 'X';
    }
}
