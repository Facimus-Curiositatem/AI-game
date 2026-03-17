package backgammon.model;

/**
 * Represents a single move in Backgammon.
 *
 * 'from' and 'to' use 0-based indexing for board points (0 = point 1, 23 = point 24).
 * Special values:
 *   from = BAR (-1) means entering a piece from the bar.
 *   to   = BEAR_OFF (-1) means bearing a piece off the board.
 */
public class Move {

    /** Constant indicating a piece enters from the bar. */
    public static final int BAR = -1;

    /** Constant indicating a piece is borne off. */
    public static final int BEAR_OFF = -1;

    private final int from;
    private final int to;

    /**
     * Creates a new move.
     * @param from source index (0-23 for board, BAR for entering from bar)
     * @param to   destination index (0-23 for board, BEAR_OFF for bearing off)
     */
    public Move(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int getFrom() {
        return from;
    }

    public int getTo() {
        return to;
    }

    /** Whether this move enters a piece from the bar. */
    public boolean isEnterFromBar() {
        return from == BAR;
    }

    /** Whether this move bears a piece off the board. */
    public boolean isBearOff() {
        return to == BEAR_OFF;
    }

    
    public String describe() {
        String fromStr = isEnterFromBar() ? "BAR" : String.valueOf(from + 1);
        String toStr = isBearOff() ? "OFF" : String.valueOf(to + 1);
        return fromStr + " -> " + toStr;
    }

    @Override
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Move)) return false;
        Move other = (Move) obj;
        return this.from == other.from && this.to == other.to;
    }

    @Override
    public int hashCode() {
        return 31 * from + to;
    }
}
