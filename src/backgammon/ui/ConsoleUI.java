package backgammon.ui;

import backgammon.model.BackgammonState;
import backgammon.model.Move;
import backgammon.model.Player;

import java.util.List;
import java.util.Scanner;

/**
 * Text-based console interface for the Backgammon game.
 * Displays the board state and handles human player input.
 */
public class ConsoleUI {

    private final Scanner scanner;

    public ConsoleUI() {
        this.scanner = new Scanner(System.in);
    }

    /**
     * Displays the full board state including bar, borne-off, and die info.
     *
     * Layout:
     *   Top row:    Points 13-24 (left to right)
     *   Bottom row: Points 12-1  (left to right)
     *   Bar in the center
     *
     * O = White (Human), X = Black (AI)
     */
    public void displayBoard(BackgammonState state) {
        int[] board = state.getBoard();

        System.out.println();
        System.out.println("=".repeat(62));

        // --- Top row: Points 13-18 | BAR | Points 19-24 ---
        System.out.print(" ");
        for (int i = 12; i < 18; i++) {
            System.out.printf("%3d ", i + 1);
        }
        System.out.print(" |BAR| ");
        for (int i = 18; i < 24; i++) {
            System.out.printf("%3d ", i + 1);
        }
        System.out.println();

        // Print top-half pieces (up to 5 rows of pieces)
        for (int row = 0; row < 5; row++) {
            System.out.print(" ");
            for (int i = 12; i < 18; i++) {
                System.out.print(" " + getPieceChar(board[i], row) + "  ");
            }
            // Bar column
            if (row == 0) {
                System.out.printf(" |%1s%1s | ",
                        state.getBar(Player.WHITE) > 0 ? "O" : " ",
                        state.getBar(Player.BLACK) > 0 ? "X" : " ");
            } else if (row == 1) {
                System.out.printf(" |%d,%d| ",
                        state.getBar(Player.WHITE), state.getBar(Player.BLACK));
            } else {
                System.out.print(" |   | ");
            }
            for (int i = 18; i < 24; i++) {
                System.out.print(" " + getPieceChar(board[i], row) + "  ");
            }
            System.out.println();
        }

        // --- Separator ---
        System.out.println(" " + "-".repeat(60));

        // Print bottom-half pieces (up to 5 rows, bottom-up)
        for (int row = 4; row >= 0; row--) {
            System.out.print(" ");
            for (int i = 11; i >= 6; i--) {
                System.out.print(" " + getPieceChar(board[i], row) + "  ");
            }
            System.out.print(" |   | ");
            for (int i = 5; i >= 0; i--) {
                System.out.print(" " + getPieceChar(board[i], row) + "  ");
            }
            System.out.println();
        }

        // --- Bottom row: Points 12-7 | BAR | Points 6-1 ---
        System.out.print(" ");
        for (int i = 11; i >= 6; i--) {
            System.out.printf("%3d ", i + 1);
        }
        System.out.print(" |BAR| ");
        for (int i = 5; i >= 0; i--) {
            System.out.printf("%3d ", i + 1);
        }
        System.out.println();

        System.out.println("=".repeat(62));

        // --- Status line ---
        System.out.printf(" White (O - Human): Bar=%d  Off=%d  |  Black (X - AI): Bar=%d  Off=%d%n",
                state.getBar(Player.WHITE), state.getBorneOff(Player.WHITE),
                state.getBar(Player.BLACK), state.getBorneOff(Player.BLACK));

        if (state.getDieRoll() > 0) {
            System.out.printf(" Current player: %s  |  Die roll: %d%n",
                    state.getCurrentPlayer().displayName(), state.getDieRoll());
        }
        System.out.println();
    }

    /**
     * Returns a character representing the piece at a given board index for a given display row.
     * Shows the player symbol if there are enough pieces to fill that row.
     */
    private String getPieceChar(int boardValue, int row) {
        int absCount = Math.abs(boardValue);
        if (row < absCount) {
            if (absCount > 5 && row == 4) {
                // Show count for stacks > 5
                return String.valueOf(absCount);
            }
            return boardValue > 0 ? "O" : "X";
        }
        return ".";
    }

    /**
     * Displays the list of legal moves and prompts the human to choose one.
     *
     * @param moves the list of legal moves
     * @return the chosen Move
     */
    public Move promptHumanMove(List<Move> moves) {
        System.out.println(" Your legal moves:");
        for (int i = 0; i < moves.size(); i++) {
            System.out.printf("   [%d] %s%n", i + 1, moves.get(i).describe());
        }
        System.out.println();

        while (true) {
            System.out.printf(" Choose a move (1-%d): ", moves.size());
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= moves.size()) {
                    return moves.get(choice - 1);
                }
                System.out.println(" Invalid choice. Please enter a number between 1 and " + moves.size());
            } catch (NumberFormatException e) {
                System.out.println(" Invalid input. Please enter a number.");
            }
        }
    }

    /**
     * Displays a message that the AI has chosen a move.
     */
    public void displayAIMove(Move move) {
        if (move == null) {
            System.out.println(" AI has no legal moves. Turn forfeited.");
        } else {
            System.out.println(" AI chose: " + move.describe());
        }
        System.out.println();
    }

    /**
     * Displays that a player has no legal moves and forfeits the turn.
     */
    public void displayNoMoves(Player player) {
        System.out.printf(" %s has no legal moves. Turn forfeited.%n%n", player.displayName());
    }

    /**
     * Displays the winner of the game.
     */
    public void displayWinner(Player winner) {
        System.out.println();
        System.out.println("*".repeat(40));
        System.out.printf("  GAME OVER! %s wins!%n", winner.displayName());
        System.out.println("*".repeat(40));
        System.out.println();
    }

    /**
     * Displays the initial roll to determine who goes first.
     */
    public void displayFirstRoll(int whiteRoll, int blackRoll, Player firstPlayer) {
        System.out.println();
        System.out.printf(" Determining who goes first...%n");
        System.out.printf(" White rolls: %d  |  Black rolls: %d%n", whiteRoll, blackRoll);
        System.out.printf(" %s goes first with a roll of %d!%n%n",
                firstPlayer.displayName(), Math.max(whiteRoll, blackRoll));
    }

    /**
     * Displays a die roll for a player's turn.
     */
    public void displayDieRoll(Player player, int die) {
        System.out.printf(" %s rolls a %d.%n", player.displayName(), die);
    }

    /**
     * Displays a general message.
     */
    public void displayMessage(String message) {
        System.out.println(" " + message);
    }

    /**
     * Displays a welcome banner.
     */
    public void displayWelcome() {
        System.out.println();
        System.out.println("=".repeat(62));
        System.out.println("       BACKGAMMON — Simplified (1 Die, No Bets)");
        System.out.println("       AI powered by Expectiminimax (Depth 2)");
        System.out.println("=".repeat(62));
        System.out.println("  You play as White (O). AI plays as Black (X).");
        System.out.println("  White moves: 24 -> 1  |  Black moves: 1 -> 24");
        System.out.println("=".repeat(62));
        System.out.println();
    }
}
