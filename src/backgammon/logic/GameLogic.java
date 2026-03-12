package backgammon.logic;

import backgammon.model.BackgammonState;
import backgammon.model.Move;
import backgammon.model.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains all game rules for simplified Backgammon (single die, no doubling cube).
 *
 * Responsibilities:
 *   - Generate all legal moves for a given state and die roll
 *   - Apply a move to produce a new state
 *   - Check terminal conditions (game over)
 *   - Determine if a player can bear off
 */
public class GameLogic {

    // ====================== Legal Move Generation ======================

    /**
     * Returns all legal moves for the current player given the state's die roll.
     * If no moves are possible, returns an empty list (turn forfeited).
     */
    public List<Move> getLegalMoves(BackgammonState state) {
        Player player = state.getCurrentPlayer();
        int die = state.getDieRoll();
        List<Move> moves = new ArrayList<>();

        // If the player has pieces on the bar, they MUST re-enter first
        if (state.getBar(player) > 0) {
            generateBarEntryMoves(state, player, die, moves);
            return moves;
        }

        // Check if the player can bear off (all pieces in home board)
        if (canBearOff(state, player)) {
            generateBearingOffMoves(state, player, die, moves);
        }

        // Generate normal moves (moving pieces along the board)
        generateNormalMoves(state, player, die, moves);

        return moves;
    }

    // ---------------------- Bar Entry Moves ----------------------

    /**
     * Generates moves for re-entering a piece from the bar.
     * WHITE enters on Black's home board (points 24-19, indices 23-18).
     * BLACK enters on White's home board (points 1-6, indices 0-5).
     */
    private void generateBarEntryMoves(BackgammonState state, Player player,
                                       int die, List<Move> moves) {
        int entryIndex;
        if (player == Player.WHITE) {
            // White enters on point (25 - die), which is index (24 - die)
            entryIndex = 24 - die;
        } else {
            // Black enters on point (die), which is index (die - 1)
            entryIndex = die - 1;
        }

        // Check if the entry point is not blocked
        if (!state.isBlockedBy(entryIndex, player)) {
            moves.add(new Move(Move.BAR, entryIndex));
        }
    }

    // ---------------------- Normal Moves ----------------------

    /**
     * Generates all normal moves (moving pieces along the board, not bearing off).
     */
    private void generateNormalMoves(BackgammonState state, Player player,
                                     int die, List<Move> moves) {
        for (int i = 0; i < BackgammonState.NUM_POINTS; i++) {
            if (state.getPlayerPiecesAt(i, player) > 0) {
                int dest = getDestination(i, die, player);

                // Destination must be on the board (0-23) for a normal move
                if (dest >= 0 && dest < BackgammonState.NUM_POINTS) {
                    if (!state.isBlockedBy(dest, player)) {
                        moves.add(new Move(i, dest));
                    }
                }
            }
        }
    }

    // ---------------------- Bearing Off Moves ----------------------

    /**
     * Generates bearing-off moves and internal home-board moves.
     * Called only when canBearOff() is true.
     */
    private void generateBearingOffMoves(BackgammonState state, Player player,
                                         int die, List<Move> moves) {
        if (player == Player.WHITE) {
            generateBearingOffWhite(state, die, moves);
        } else {
            generateBearingOffBlack(state, die, moves);
        }
    }

    /**
     * Bearing off logic for WHITE.
     * White's home board = points 1-6, indices 0-5.
     * White bears off by moving below index 0.
     * The "point number" for bearing off purposes: index + 1 (1-based).
     */
    private void generateBearingOffWhite(BackgammonState state, int die, List<Move> moves) {
        // Exact bear-off: piece on index (die - 1) can bear off
        int exactIndex = die - 1;
        if (exactIndex >= 0 && exactIndex < 6 && state.getPlayerPiecesAt(exactIndex, Player.WHITE) > 0) {
            moves.add(new Move(exactIndex, Move.BEAR_OFF));
        }

        // Higher roll than highest occupied point: bear off from highest occupied
        int highestOccupied = -1;
        for (int i = 5; i >= 0; i--) {
            if (state.getPlayerPiecesAt(i, Player.WHITE) > 0) {
                highestOccupied = i;
                break;
            }
        }

        if (highestOccupied >= 0 && die > (highestOccupied + 1)) {
            // Can bear off from the highest occupied point (if not already added as exact)
            if (highestOccupied != exactIndex) {
                moves.add(new Move(highestOccupied, Move.BEAR_OFF));
            }
        }
    }

    /**
     * Bearing off logic for BLACK.
     * Black's home board = points 19-24, indices 18-23.
     * Black bears off by moving beyond index 23.
     * The "point number" for bearing off purposes: 24 - index (distance from edge).
     */
    private void generateBearingOffBlack(BackgammonState state, int die, List<Move> moves) {
        // Exact bear-off: piece on index (24 - die) can bear off
        int exactIndex = 24 - die;
        if (exactIndex >= 18 && exactIndex <= 23 && state.getPlayerPiecesAt(exactIndex, Player.BLACK) > 0) {
            moves.add(new Move(exactIndex, Move.BEAR_OFF));
        }

        // Higher roll than highest occupied point (farthest from bearing off edge)
        // For Black, "highest" means the lowest index in 18-23
        int highestOccupied = -1;
        for (int i = 18; i <= 23; i++) {
            if (state.getPlayerPiecesAt(i, Player.BLACK) > 0) {
                highestOccupied = i;
                break;  // first found from index 18 is the farthest from edge
            }
        }

        if (highestOccupied >= 0) {
            int distFromEdge = 24 - highestOccupied;  // how many points from bearing off
            if (die > distFromEdge) {
                if (highestOccupied != exactIndex) {
                    moves.add(new Move(highestOccupied, Move.BEAR_OFF));
                }
            }
        }
    }

    // ====================== Move Application ======================

    /**
     * Applies a move to the given state and returns a NEW state.
     * The original state is not modified.
     */
    public BackgammonState applyMove(BackgammonState state, Move move) {
        BackgammonState newState = state.deepCopy();
        Player player = newState.getCurrentPlayer();
        int sign = player.sign();

        // --- Remove piece from source ---
        if (move.isEnterFromBar()) {
            newState.setBar(player, newState.getBar(player) - 1);
        } else {
            newState.setPiecesAt(move.getFrom(), newState.getPiecesAt(move.getFrom()) - sign);
        }

        // --- Place piece at destination ---
        if (move.isBearOff()) {
            newState.setBorneOff(player, newState.getBorneOff(player) + 1);
        } else {
            int dest = move.getTo();

            // Check for hitting an opponent's blot
            if (newState.isBlot(dest, player)) {
                Player opponent = player.opponent();
                // Remove opponent's piece from the point
                newState.setPiecesAt(dest, 0);
                // Place it on the bar
                newState.setBar(opponent, newState.getBar(opponent) + 1);
            }

            // Place our piece
            newState.setPiecesAt(dest, newState.getPiecesAt(dest) + sign);
        }

        return newState;
    }

    // ====================== Terminal Test ======================

    /**
     * Checks if the game is over (either player has borne off all 15 pieces).
     */
    public boolean isTerminal(BackgammonState state) {
        return state.getBorneOff(Player.WHITE) == BackgammonState.PIECES_PER_PLAYER
            || state.getBorneOff(Player.BLACK) == BackgammonState.PIECES_PER_PLAYER;
    }

    /**
     * Returns the winner if the game is terminal, or null if still in progress.
     */
    public Player getWinner(BackgammonState state) {
        if (state.getBorneOff(Player.WHITE) == BackgammonState.PIECES_PER_PLAYER) {
            return Player.WHITE;
        }
        if (state.getBorneOff(Player.BLACK) == BackgammonState.PIECES_PER_PLAYER) {
            return Player.BLACK;
        }
        return null;
    }

    // ====================== Bearing Off Eligibility ======================

    /**
     * Checks whether the given player can begin bearing off.
     * This requires ALL of the player's pieces to be in their home board
     * (none on the bar, none outside the home board).
     *
     * White's home board: indices 0-5 (points 1-6).
     * Black's home board: indices 18-23 (points 19-24).
     */
    public boolean canBearOff(BackgammonState state, Player player) {
        // Must have no pieces on the bar
        if (state.getBar(player) > 0) {
            return false;
        }

        // Count pieces outside the home board
        if (player == Player.WHITE) {
            for (int i = 6; i < BackgammonState.NUM_POINTS; i++) {
                if (state.getPlayerPiecesAt(i, Player.WHITE) > 0) {
                    return false;
                }
            }
        } else {
            for (int i = 0; i < 18; i++) {
                if (state.getPlayerPiecesAt(i, Player.BLACK) > 0) {
                    return false;
                }
            }
        }

        return true;
    }

    // ====================== Helper Methods ======================

    /**
     * Computes the destination index for a piece at 'fromIndex' moving 'die' steps.
     * WHITE moves toward lower indices, BLACK toward higher indices.
     */
    private int getDestination(int fromIndex, int die, Player player) {
        if (player == Player.WHITE) {
            return fromIndex - die;
        } else {
            return fromIndex + die;
        }
    }

    /**
     * Computes the total pip count for a player.
     * Pip count = sum of (distance to bear off) for each piece.
     * Includes pieces on the bar (distance = 25 for bar pieces).
     */
    public int computePipCount(BackgammonState state, Player player) {
        int pips = 0;

        for (int i = 0; i < BackgammonState.NUM_POINTS; i++) {
            int count = state.getPlayerPiecesAt(i, player);
            if (count > 0) {
                int distance;
                if (player == Player.WHITE) {
                    distance = i + 1;  // White needs to go below index 0; distance = index + 1
                } else {
                    distance = 24 - i; // Black needs to go beyond index 23; distance = 24 - index
                }
                pips += count * distance;
            }
        }

        // Bar pieces: considered as needing to travel full board (distance ~25)
        pips += state.getBar(player) * 25;

        return pips;
    }
}
