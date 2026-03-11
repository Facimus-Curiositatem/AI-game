package backgammon;

import backgammon.ai.ExpectiminimaxAgent;
import backgammon.ai.HeuristicEvaluator;
import backgammon.logic.GameLogic;
import backgammon.model.BackgammonState;
import backgammon.model.Move;
import backgammon.model.Player;
import backgammon.ui.ConsoleUI;

import java.util.List;
import java.util.Random;

/**
 * Main entry point for the Backgammon game.
 *
 * Game loop:
 *   1. Initialize the board to standard starting position.
 *   2. Determine who goes first (each rolls one die, higher goes first).
 *   3. Alternate turns: roll die, select move (human input or AI search), apply move.
 *   4. Display the board after each move.
 *   5. Check for terminal condition (all pieces borne off).
 *   6. Announce winner.
 *
 * Human plays as WHITE (O). AI plays as BLACK (X).
 */
public class Main {

    private static final Random random = new Random();

    public static void main(String[] args) {
        // Initialize components
        GameLogic gameLogic = new GameLogic();
        HeuristicEvaluator evaluator = new HeuristicEvaluator(gameLogic);
        ExpectiminimaxAgent aiAgent = new ExpectiminimaxAgent(gameLogic, evaluator);
        ConsoleUI ui = new ConsoleUI();

        // Display welcome
        ui.displayWelcome();

        // Initialize game state
        BackgammonState state = new BackgammonState();
        state.initStartingPosition();

        // Determine who goes first
        Player firstPlayer = determineFirstPlayer(ui);
        state.setCurrentPlayer(firstPlayer);

        // If AI goes first, roll and use the first-move die
        // If human goes first, the first-move die was already shown
        int firstDie = rollDie();
        state.setDieRoll(firstDie);
        ui.displayDieRoll(firstPlayer, firstDie);

        // Display initial board
        ui.displayBoard(state);

        // ====================== Game Loop ======================
        while (!gameLogic.isTerminal(state)) {
            Player currentPlayer = state.getCurrentPlayer();

            // Generate legal moves
            List<Move> legalMoves = gameLogic.getLegalMoves(state);

            Move chosenMove;

            if (legalMoves.isEmpty()) {
                // No legal moves — turn forfeited
                ui.displayNoMoves(currentPlayer);
                chosenMove = null;
            } else if (currentPlayer == Player.WHITE) {
                // Human's turn
                chosenMove = ui.promptHumanMove(legalMoves);
                ui.displayMessage("You chose: " + chosenMove.describe());
            } else {
                // AI's turn
                ui.displayMessage("AI is thinking...");
                chosenMove = aiAgent.chooseMove(state);
                ui.displayAIMove(chosenMove);
            }

            // Apply the chosen move (if any)
            if (chosenMove != null) {
                state = gameLogic.applyMove(state, chosenMove);
            }

            // Check for terminal condition before switching turns
            if (gameLogic.isTerminal(state)) {
                state.setCurrentPlayer(currentPlayer);  // keep for display
                ui.displayBoard(state);
                break;
            }

            // Switch to the other player's turn
            Player nextPlayer = currentPlayer.opponent();
            state.setCurrentPlayer(nextPlayer);

            // Roll die for next turn
            int die = rollDie();
            state.setDieRoll(die);
            ui.displayDieRoll(nextPlayer, die);

            // Display updated board
            ui.displayBoard(state);
        }

        // ====================== Game Over ======================
        Player winner = gameLogic.getWinner(state);
        if (winner != null) {
            ui.displayWinner(winner);
        } else {
            ui.displayMessage("Game ended unexpectedly.");
        }
    }

    /**
     * Determines who goes first by having each player roll one die.
     * Re-rolls on ties.
     */
    private static Player determineFirstPlayer(ConsoleUI ui) {
        while (true) {
            int whiteRoll = rollDie();
            int blackRoll = rollDie();

            if (whiteRoll != blackRoll) {
                Player first = whiteRoll > blackRoll ? Player.WHITE : Player.BLACK;
                ui.displayFirstRoll(whiteRoll, blackRoll, first);
                return first;
            }
            ui.displayMessage("Both rolled " + whiteRoll + ". Rolling again...");
        }
    }

    /**
     * Rolls a single die (1-6).
     */
    private static int rollDie() {
        return random.nextInt(6) + 1;
    }
}
