package backgammon;

import backgammon.ai.ExpectiminimaxAgent;
import backgammon.ai.HeuristicEvaluator;
import backgammon.logic.GameLogic;
import backgammon.model.BackgammonState;
import backgammon.model.Move;
import backgammon.model.Player;
import backgammon.ui.ConsoleUI;

import java.util.List;

/**
 * Main entry point for the Backgammon game.
 *
 * Game loop:
 *   1. Initialize the board to standard starting position.
 *   2. Let the user choose which color to play (AI takes the other).
 *   3. Determine who goes first (each rolls one die, higher goes first).
 *   4. Alternate turns: enter die roll, select move (human input or AI search), apply move.
 *   5. Display the board after each move.
 *   6. Check for terminal condition (all pieces borne off).
 *   7. Announce winner.
 *
 * Die rolls are entered manually (physical dice used in competition).
 */
public class Main {

    public static void main(String[] args) {
        // Initialize core components
        GameLogic gameLogic = new GameLogic();
        ConsoleUI ui = new ConsoleUI();

        // Display welcome
        ui.displayWelcome();

        // Let the user choose their color
        Player humanPlayer = ui.promptColorChoice();
        Player aiPlayer = humanPlayer.opponent();
        ui.setPlayers(humanPlayer);
        ui.displayRoleAssignment(humanPlayer);

        // Configure AI with the chosen color
        HeuristicEvaluator evaluator = new HeuristicEvaluator(gameLogic, aiPlayer);
        ExpectiminimaxAgent aiAgent = new ExpectiminimaxAgent(gameLogic, evaluator, aiPlayer);

        // Initialize game state
        BackgammonState state = new BackgammonState();
        state.initStartingPosition();

        // Determine who goes first
        Player firstPlayer = determineFirstPlayer(ui);
        state.setCurrentPlayer(firstPlayer);

        // Roll die for the first turn
        int firstDie = ui.promptDieRoll(firstPlayer);
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
            } else if (currentPlayer == humanPlayer) {
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

            // Enter die roll for next turn
            int die = ui.promptDieRoll(nextPlayer);
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
     * Die values are entered manually. Re-rolls on ties.
     */
    private static Player determineFirstPlayer(ConsoleUI ui) {
        System.out.println(" Determining who goes first...");
        while (true) {
            int whiteRoll = ui.promptDieRoll(Player.WHITE);
            int blackRoll = ui.promptDieRoll(Player.BLACK);

            if (whiteRoll != blackRoll) {
                Player first = whiteRoll > blackRoll ? Player.WHITE : Player.BLACK;
                ui.displayFirstRoll(whiteRoll, blackRoll, first);
                return first;
            }
            ui.displayMessage("Both rolled " + whiteRoll + ". Rolling again...");
        }
    }
}
