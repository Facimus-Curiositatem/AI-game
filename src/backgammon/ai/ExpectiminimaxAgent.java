package backgammon.ai;

import backgammon.logic.GameLogic;
import backgammon.model.BackgammonState;
import backgammon.model.Move;
import backgammon.model.Player;

import java.util.List;

/**
 * AI agent that uses the Expectiminimax algorithm to choose moves.
 *
 * Expectiminimax extends standard MIN-MAX by introducing CHANCE nodes
 * to handle stochastic events (die rolls). This is the standard approach
 * for games with randomness, as described in Russell & Norvig, Section 6.5.
 *
 * Tree structure (depth = 2 levels of play):
 *
 *   Level 0 — MAX (AI's turn):
 *       For the current die roll, evaluate all legal moves.
 *       Choose the move with the maximum expected value.
 *
 *   Level 1 — CHANCE node:
 *       Average over all 6 possible die values (each with probability 1/6).
 *
 *   Level 2 — MIN (Opponent's turn):
 *       For each die value, the opponent picks the move that minimizes
 *       the AI's heuristic value.
 *
 * The AI plays as BLACK.
 */
public class ExpectiminimaxAgent {

    private static final int MAX_DEPTH = 2;
    private static final int DIE_SIDES = 6;

    private final GameLogic gameLogic;
    private final HeuristicEvaluator evaluator;
    private final Player aiPlayer;
    private final Player opponent;

    public ExpectiminimaxAgent(GameLogic gameLogic, HeuristicEvaluator evaluator, Player aiPlayer) {
        this.gameLogic = gameLogic;
        this.evaluator = evaluator;
        this.aiPlayer = aiPlayer;
        this.opponent = aiPlayer.opponent();
    }

    /**
     * Chooses the best move for the AI given the current state.
     * The state must have the AI as the current player and a valid die roll.
     *
     * @param state the current game state (AI's turn, die already rolled)
     * @return the best Move, or null if no legal moves exist (turn forfeited)
     */
    public Move chooseMove(BackgammonState state) {
        List<Move> legalMoves = gameLogic.getLegalMoves(state);

        if (legalMoves.isEmpty()) {
            return null;  // No legal moves — turn forfeited
        }

        Move bestMove = null;
        double bestValue = Double.NEGATIVE_INFINITY;

        for (Move move : legalMoves) {
            BackgammonState child = gameLogic.applyMove(state, move);
            // After AI moves, switch to opponent's turn for the subtree
            child.setCurrentPlayer(opponent);

            // Next level is a CHANCE node (opponent will roll a die)
            double value = chanceNode(child, MAX_DEPTH - 1);

            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
        }

        return bestMove;
    }

    /**
     * CHANCE node: averages the value over all 6 possible die rolls.
     * Each die value has equal probability (1/6).
     */
    private double chanceNode(BackgammonState state, int depth) {
        if (depth <= 0 || gameLogic.isTerminal(state)) {
            return evaluator.evaluate(state);
        }

        double expectedValue = 0.0;

        for (int die = 1; die <= DIE_SIDES; die++) {
            BackgammonState stateWithDie = state.deepCopy();
            stateWithDie.setDieRoll(die);

            double value;
            if (stateWithDie.getCurrentPlayer() == aiPlayer) {
                value = maxNode(stateWithDie, depth);
            } else {
                value = minNode(stateWithDie, depth);
            }

            expectedValue += value / DIE_SIDES;
        }

        return expectedValue;
    }

    /**
     * MAX node: AI (BLACK) picks the move that maximizes the heuristic value.
     */
    private double maxNode(BackgammonState state, int depth) {
        if (depth <= 0 || gameLogic.isTerminal(state)) {
            return evaluator.evaluate(state);
        }

        List<Move> legalMoves = gameLogic.getLegalMoves(state);

        if (legalMoves.isEmpty()) {
            // No legal moves — turn forfeited, pass to opponent via chance node
            BackgammonState passState = state.deepCopy();
            passState.setCurrentPlayer(opponent);
            return chanceNode(passState, depth - 1);
        }

        double bestValue = Double.NEGATIVE_INFINITY;

        for (Move move : legalMoves) {
            BackgammonState child = gameLogic.applyMove(state, move);
            child.setCurrentPlayer(opponent);

            double value = chanceNode(child, depth - 1);
            bestValue = Math.max(bestValue, value);
        }

        return bestValue;
    }

    /**
     * MIN node: Opponent (WHITE) picks the move that minimizes the heuristic value.
     */
    private double minNode(BackgammonState state, int depth) {
        if (depth <= 0 || gameLogic.isTerminal(state)) {
            return evaluator.evaluate(state);
        }

        List<Move> legalMoves = gameLogic.getLegalMoves(state);

        if (legalMoves.isEmpty()) {
            // No legal moves — turn forfeited, pass to AI via chance node
            BackgammonState passState = state.deepCopy();
            passState.setCurrentPlayer(aiPlayer);
            return chanceNode(passState, depth - 1);
        }

        double bestValue = Double.POSITIVE_INFINITY;

        for (Move move : legalMoves) {
            BackgammonState child = gameLogic.applyMove(state, move);
            child.setCurrentPlayer(aiPlayer);

            double value = chanceNode(child, depth - 1);
            bestValue = Math.min(bestValue, value);
        }

        return bestValue;
    }
}
