package backgammon.model;

import java.util.Arrays;

/**
 * Represents the complete state of a Backgammon game.
 *
 * Board convention (0-based indexing, index 0 = point 1, index 23 = point 24):
 *   Positive values → White pieces on that point
 *   Negative values → Black pieces on that point
 *   Zero            → empty point
 *
 * WHITE moves from high indices toward low indices (24 → 1).
 * BLACK moves from low indices toward high indices (1 → 24).
 */
public class BackgammonState {

    /** Number of points on the board. */
    public static final int NUM_POINTS = 24;

    /** Total pieces each player starts with. */
    public static final int PIECES_PER_PLAYER = 15;

    // --- Board ---
    private int[] board;

    // --- Bar (captured pieces) ---
    private int barWhite;
    private int barBlack;

    // --- Borne off pieces ---
    private int borneOffWhite;
    private int borneOffBlack;

    // --- Turn info ---
    private Player currentPlayer;
    private int dieRoll;

    /**
     * Creates an empty state. Use initStartingPosition() to set up the standard layout.
     */
    public BackgammonState() {
        this.board = new int[NUM_POINTS];
        this.barWhite = 0;
        this.barBlack = 0;
        this.borneOffWhite = 0;
        this.borneOffBlack = 0;
        this.currentPlayer = Player.WHITE;
        this.dieRoll = 0;
    }

    /**
     * Initializes the standard Backgammon starting position.
     *
     * White's perspective (point numbers 1-based):
     *   Point  1 (index  0): 2 White
     *   Point  6 (index  5): 5 Black
     *   Point  8 (index  7): 3 Black
     *   Point 12 (index 11): 5 White
     *   Point 13 (index 12): 5 Black
     *   Point 17 (index 16): 3 White
     *   Point 19 (index 18): 5 White
     *   Point 24 (index 23): 2 Black
     */
    public void initStartingPosition() {
        Arrays.fill(board, 0);

        // White pieces (positive)
        board[0]  =  2;   // Point 1
        board[11] =  5;   // Point 12
        board[16] =  3;   // Point 17
        board[18] =  5;   // Point 19

        // Black pieces (negative)
        board[5]  = -5;   // Point 6
        board[7]  = -3;   // Point 8
        board[12] = -5;   // Point 13
        board[23] = -2;   // Point 24

        barWhite = 0;
        barBlack = 0;
        borneOffWhite = 0;
        borneOffBlack = 0;
    }

    /**
     * Creates a deep copy of this state, safe for search-tree exploration.
     */
    public BackgammonState deepCopy() {
        BackgammonState copy = new BackgammonState();
        copy.board = Arrays.copyOf(this.board, NUM_POINTS);
        copy.barWhite = this.barWhite;
        copy.barBlack = this.barBlack;
        copy.borneOffWhite = this.borneOffWhite;
        copy.borneOffBlack = this.borneOffBlack;
        copy.currentPlayer = this.currentPlayer;
        copy.dieRoll = this.dieRoll;
        return copy;
    }

    // ====================== Getters and Setters ======================

    public int[] getBoard() {
        return board;
    }

    public int getPiecesAt(int index) {
        return board[index];
    }

    public void setPiecesAt(int index, int value) {
        board[index] = value;
    }

    public int getBar(Player player) {
        return player == Player.WHITE ? barWhite : barBlack;
    }

    public void setBar(Player player, int value) {
        if (player == Player.WHITE) {
            barWhite = value;
        } else {
            barBlack = value;
        }
    }

    public int getBorneOff(Player player) {
        return player == Player.WHITE ? borneOffWhite : borneOffBlack;
    }

    public void setBorneOff(Player player, int value) {
        if (player == Player.WHITE) {
            borneOffWhite = value;
        } else {
            borneOffBlack = value;
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
    }

    public int getDieRoll() {
        return dieRoll;
    }

    public void setDieRoll(int dieRoll) {
        this.dieRoll = dieRoll;
    }

    // ====================== Utility Methods ======================

    /**
     * Returns the number of pieces the given player has on a specific point.
     * Returns 0 if the point belongs to the opponent or is empty.
     */
    public int getPlayerPiecesAt(int index, Player player) {
        int val = board[index];
        if (player == Player.WHITE) {
            return val > 0 ? val : 0;
        } else {
            return val < 0 ? -val : 0;
        }
    }

    /**
     * Checks whether the given point is owned by the specified player
     * (has at least one of that player's pieces).
     */
    public boolean isOwnedBy(int index, Player player) {
        return getPlayerPiecesAt(index, player) > 0;
    }

    /**
     * Checks whether the given point is blocked by the opponent
     * (has 2 or more opponent pieces).
     */
    public boolean isBlockedBy(int index, Player player) {
        // Blocked if the opponent has 2+ pieces
        Player opponent = player.opponent();
        return getPlayerPiecesAt(index, opponent) >= 2;
    }

    /**
     * Checks whether the given point has exactly one opponent piece (a blot).
     */
    public boolean isBlot(int index, Player player) {
        Player opponent = player.opponent();
        return getPlayerPiecesAt(index, opponent) == 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Board: ").append(Arrays.toString(board));
        sb.append(" | Bar W:").append(barWhite).append(" B:").append(barBlack);
        sb.append(" | Off W:").append(borneOffWhite).append(" B:").append(borneOffBlack);
        sb.append(" | Turn: ").append(currentPlayer).append(" Die: ").append(dieRoll);
        return sb.toString();
    }
}
