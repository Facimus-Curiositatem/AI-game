package backgammon.ai;

import backgammon.logic.GameLogic;
import backgammon.model.BackgammonState;
import backgammon.model.Player;

/**
 * Evaluates a Backgammon state from the AI's (BLACK) perspective using a weighted
 * linear combination of six indicators.
 *
 * H(state) = w1 * PipCountDiff
 *          + w2 * BlotsDiff
 *          + w3 * BarDiff
 *          + w4 * BorneOffDiff
 *          + w5 * MadePointsDiff
 *          + w6 * HomeBoardDiff
 *
 * Positive values favor the AI (BLACK). Negative values favor the human (WHITE).
 */
public class HeuristicEvaluator {

    private static final Player AI = Player.BLACK;
    private static final Player HUMAN = Player.WHITE;

    // Weights for each indicator (tunable)
    private static final double W_PIP_COUNT    = 1.0;
    private static final double W_BLOTS        = 2.0;
    private static final double W_BAR          = 3.0;
    private static final double W_BORNE_OFF    = 4.0;
    private static final double W_MADE_POINTS  = 1.5;
    private static final double W_HOME_BOARD   = 1.0;

    private final GameLogic gameLogic;

    public HeuristicEvaluator(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    /**
     * Evaluates the given state. Returns a double where:
     *   Positive = favorable for AI (BLACK)
     *   Negative = favorable for Human (WHITE)
     *   Large magnitude = stronger advantage
     */
    public double evaluate(BackgammonState state) {
        // Terminal states get extreme values
        if (gameLogic.isTerminal(state)) {
            Player winner = gameLogic.getWinner(state);
            if (winner == AI) return 10000.0;
            if (winner == HUMAN) return -10000.0;
        }

        double score = 0.0;

        // Indicator 1: Pip Count Difference
        // Lower pip count is better. We want (Human pips - AI pips) to be positive for AI.
        int aiPips = gameLogic.computePipCount(state, AI);
        int humanPips = gameLogic.computePipCount(state, HUMAN);
        score += W_PIP_COUNT * (humanPips - aiPips);

        // Indicator 2: Blots Difference
        // Fewer blots is better. We want (Human blots - AI blots) to be positive for AI.
        int aiBlots = countBlots(state, AI);
        int humanBlots = countBlots(state, HUMAN);
        score += W_BLOTS * (humanBlots - aiBlots);

        // Indicator 3: Bar Pieces Difference
        // Fewer pieces on bar is better. We want (Human bar - AI bar) to be positive for AI.
        int aiBar = state.getBar(AI);
        int humanBar = state.getBar(HUMAN);
        score += W_BAR * (humanBar - aiBar);

        // Indicator 4: Borne Off Difference
        // More pieces borne off is better. We want (AI borne off - Human borne off).
        int aiBorneOff = state.getBorneOff(AI);
        int humanBorneOff = state.getBorneOff(HUMAN);
        score += W_BORNE_OFF * (aiBorneOff - humanBorneOff);

        // Indicator 5: Made Points Difference
        // More made points (2+ pieces = anchors) is better for board control.
        int aiMadePoints = countMadePoints(state, AI);
        int humanMadePoints = countMadePoints(state, HUMAN);
        score += W_MADE_POINTS * (aiMadePoints - humanMadePoints);

        // Indicator 6: Home Board Strength Difference
        // More pieces in home board = closer to bearing off.
        int aiHome = countHomeBoardPieces(state, AI);
        int humanHome = countHomeBoardPieces(state, HUMAN);
        score += W_HOME_BOARD * (aiHome - humanHome);

        return score;
    }

    /**
     * Counts the number of blots (single exposed pieces) for a player.
     */
    private int countBlots(BackgammonState state, Player player) {
        int blots = 0;
        for (int i = 0; i < BackgammonState.NUM_POINTS; i++) {
            if (state.getPlayerPiecesAt(i, player) == 1) {
                blots++;
            }
        }
        return blots;
    }

    /**
     * Counts the number of "made points" (points with 2+ pieces) for a player.
     * These act as blocks/anchors.
     */
    private int countMadePoints(BackgammonState state, Player player) {
        int madePoints = 0;
        for (int i = 0; i < BackgammonState.NUM_POINTS; i++) {
            if (state.getPlayerPiecesAt(i, player) >= 2) {
                madePoints++;
            }
        }
        return madePoints;
    }

    /**
     * Counts the number of pieces a player has in their home board.
     * White's home board: indices 0-5 (points 1-6).
     * Black's home board: indices 18-23 (points 19-24).
     */
    private int countHomeBoardPieces(BackgammonState state, Player player) {
        int count = 0;
        int start, end;
        if (player == Player.WHITE) {
            start = 0; end = 6;
        } else {
            start = 18; end = 24;
        }
        for (int i = start; i < end; i++) {
            count += state.getPlayerPiecesAt(i, player);
        }
        return count;
    }
}
