# Backgammon with Expectiminimax AI — Project Plan

## 1. Game Rules (Modified Ruleset)

### 1.1 Overview

Backgammon is a two-player board game played on a board with 24 narrow triangles called
**points**. The points are grouped into four quadrants of six points each. The quadrants
are: the player's home board, the player's outer board, the opponent's home board, and the
opponent's outer board. The home and outer boards are separated by a ridge called the **bar**.

Each player has **15 pieces** (checkers). The objective is to move all your pieces into your
home board and then **bear them off** (remove them from the board) before your opponent does.

### 1.2 Simplifications for This Assignment

This implementation differs from standard Backgammon in the following ways:

| Standard Backgammon           | Our Version                            |
|-------------------------------|----------------------------------------|
| Two dice per turn             | **One die** per turn (values 1–6)      |
| Doubles give 4 moves          | No doubles mechanic                   |
| Doubling cube for bets         | **No doubling cube, no bets**         |
| Gammon/Backgammon scoring      | **Simple win/lose** (first to bear off all pieces wins) |

### 1.3 Board Layout and Starting Position

The board has 24 points numbered 1 to 24. Players move in **opposite directions**:

- **White** moves from point 24 toward point 1 (counterclockwise).
- **Black** moves from point 1 toward point 24 (clockwise).

Standard starting position (from White's perspective):

| Point | White Pieces | Black Pieces |
|-------|-------------|-------------|
| 1     | 2           | 0           |
| 6     | 0           | 5           |
| 8     | 0           | 3           |
| 12    | 5           | 0           |
| 13    | 0           | 5           |
| 17    | 3           | 0           |
| 19    | 5           | 0           |
| 24    | 0           | 2           |

All other points start empty.

> Note: White's point 1 is Black's point 24, and vice versa. The numbering is always
> from each player's own perspective, but internally we use a single 0–23 index
> (index 0 = point 1, index 23 = point 24) from White's perspective.

### 1.4 Movement Rules

On each turn, the current player rolls **one die** (1–6) and must move **one piece**
by exactly that many points in their direction of travel.

A move is **legal** if the destination point:
- Is empty (no pieces on it), OR
- Has one or more of the player's own pieces, OR
- Has exactly **one** opponent piece (a "blot") — in this case the opponent's piece is
  **hit** and sent to the bar.

A move is **illegal** if the destination point has **two or more** opponent pieces
(the point is "blocked" or "made").

### 1.5 The Bar

When a piece is hit, it is placed on the **bar** (the center divider of the board).

- A player with pieces on the bar **must** re-enter them before moving any other piece.
- Re-entry: the piece enters the opponent's home board. The die roll determines which
  point it enters on. For White entering Black's home board: a roll of 1 enters on
  point 24, roll of 2 on point 23, ..., roll of 6 on point 19.
  For Black entering White's home board: a roll of 1 enters on point 1, roll of 2 on
  point 2, ..., roll of 6 on point 6.
- If the entry point is blocked (2+ opponent pieces), the player **forfeits their turn**.

### 1.6 Bearing Off

A player may begin bearing off only when **all 15 of their pieces** are in their
home board (points 1–6 for White, points 19–24 for Black).

Rules for bearing off with a single die:

1. **Exact roll**: If the die matches a point that has your piece, bear off that piece.
   - Example: roll a 4 with a piece on point 4 → bear it off.

2. **Higher roll than highest occupied point**: If the die value is greater than the
   highest point you occupy, you **must** bear off a piece from the highest occupied point.
   - Example: pieces only on points 3 and 1, roll a 5 → bear off from point 3.

3. **Lower roll**: If the die value is lower than your highest occupied point and
   there is no piece on the exact point, you must **move a piece within** the home board
   (you cannot bear off).
   - Example: pieces on point 5, roll a 3 → move from point 5 to point 2.

4. **No legal move**: If no move or bear-off is possible, the player forfeits the turn.

### 1.7 Winning Condition

The first player to bear off all 15 pieces wins the game. There is no gammon/backgammon
distinction — it is a simple win/lose.

### 1.8 Turn Forfeiture

If a player has no legal move (all reachable points are blocked, or they cannot
re-enter from the bar), they forfeit their turn and play passes to the opponent.

### 1.9 First Move Determination

Each player rolls one die. The player with the higher roll goes first and uses that
roll for their first move. If both roll the same value, re-roll until they differ.

---

## 2. Implementation Approach

### 2.1 Language and Paradigm

- **Language**: Java
- **Paradigm**: Object-Oriented Programming (mandatory per assignment)
- **UI**: Console/Terminal-based text interface

### 2.2 Project Structure

```
src/backgammon/
├── model/
│   ├── Player.java              # Enum: WHITE (human), BLACK (AI)
│   ├── Move.java                # Represents a single move (from → to)
│   └── BackgammonState.java     # Full game state (board, bar, borne-off, etc.)
├── logic/
│   └── GameLogic.java           # Rules engine: legal moves, apply move, terminal test
├── ai/
│   ├── HeuristicEvaluator.java  # Heuristic function with weighted indicators
│   └── ExpectiminimaxAgent.java # Expectiminimax search (depth 2)
├── ui/
│   └── ConsoleUI.java           # Text-based board display and input handling
└── Main.java                    # Entry point and game loop
```

### 2.3 State Representation (`BackgammonState`)

The game state is represented by:

| Field              | Type     | Description                                          |
|--------------------|----------|------------------------------------------------------|
| `board`            | `int[24]`| Each cell holds a signed integer. Positive = White pieces, Negative = Black pieces. Index 0 = point 1, index 23 = point 24. |
| `barWhite`         | `int`    | Number of White pieces on the bar                    |
| `barBlack`         | `int`    | Number of Black pieces on the bar                    |
| `borneOffWhite`    | `int`    | Number of White pieces borne off                     |
| `borneOffBlack`    | `int`    | Number of Black pieces borne off                     |
| `currentPlayer`    | `Player` | Whose turn it is                                     |
| `dieRoll`          | `int`    | Current die value (1–6)                              |

The state supports **deep copy** so the search algorithm can explore hypothetical
future states without mutating the actual game state.

### 2.4 Successor Function (`GameLogic.getLegalMoves`)

Given a state and a die roll, the successor function generates all legal moves:

1. **Bar check**: If the current player has pieces on the bar, the only legal moves
   are re-entries into the opponent's home board.
2. **Normal moves**: For each point with the player's pieces, compute the destination
   point (current ± die roll depending on direction). Check if the destination is legal.
3. **Bearing off**: If all of the player's pieces are in their home board:
   - Check if a piece can be borne off from the exact point matching the die.
   - If the die exceeds the highest occupied point, bear off from the highest point.
   - Also allow internal home-board moves as alternatives.
4. **No moves**: If no legal moves exist, the turn is forfeited (empty list returned).

### 2.5 Terminal Test (`GameLogic.isTerminal`)

The game is over when either player has borne off all 15 pieces:
- `borneOffWhite == 15` → White wins
- `borneOffBlack == 15` → Black wins

### 2.6 Heuristic Function (`HeuristicEvaluator`)

The heuristic evaluates a state from the AI's perspective using a **weighted linear
combination** of six indicators. Each indicator is computed as a difference
(AI advantage minus opponent advantage), so positive values favor the AI.

```
H(state) = w1 * PipCountDiff
         + w2 * BlotsDiff
         + w3 * BarDiff
         + w4 * BorneOffDiff
         + w5 * MadePointsDiff
         + w6 * HomeBoardDiff
```

#### Indicators

| # | Indicator          | Description                                                   | Weight Sign |
|---|--------------------|---------------------------------------------------------------|-------------|
| 1 | **Pip Count Diff** | Difference in total remaining distance to bear off. Lower pip count is better, so we compute (opponent's pips − AI's pips). | Positive = AI ahead |
| 2 | **Blots Diff**     | Difference in number of exposed single pieces (blots). More opponent blots is better for AI: (opponent blots − AI blots). | Positive = AI safer |
| 3 | **Bar Diff**       | Difference in pieces on the bar: (opponent bar − AI bar). Having opponent pieces on bar is advantageous. | Positive = AI advantage |
| 4 | **Borne Off Diff** | Difference in pieces borne off: (AI borne off − opponent borne off). More pieces off = closer to winning. | Positive = AI ahead |
| 5 | **Made Points Diff** | Difference in number of "made points" (points with 2+ own pieces, acting as blocks). More made points = stronger board control. | Positive = AI stronger |
| 6 | **Home Board Diff**  | Difference in number of pieces in own home board (ready to bear off). More pieces home = closer to bearing off phase. | Positive = AI ahead |

#### Weights (initial tuning)

```
w1 = 1.0   (pip count — most important overall race metric)
w2 = 2.0   (blots — vulnerability matters significantly)
w3 = 3.0   (bar pieces — high penalty, loses entire turn)
w4 = 4.0   (borne off — direct progress toward winning)
w5 = 1.5   (made points — positional/blocking advantage)
w6 = 1.0   (home board — readiness for bearing off)
```

### 2.7 Expectiminimax Algorithm (`ExpectiminimaxAgent`)

Since Backgammon involves dice (a chance element), a pure MIN-MAX tree is not
sufficient. We use **Expectiminimax** — an extension of MIN-MAX that introduces
**chance nodes** to handle stochastic events (die rolls).

Reference: Russell & Norvig, *Artificial Intelligence: A Modern Approach*,
Section 6.5 — "Games That Include an Element of Chance."

#### Tree Structure (Depth = 2 levels of play)

```
Level 0 — MAX (AI's turn):
    The AI knows its current die roll.
    For each legal move, compute the expected value at the next level.
    Choose the move with the MAXIMUM expected value.

Level 1 — CHANCE node:
    The opponent will roll a die. We don't know the outcome.
    Average over all 6 possible die values (each with probability 1/6).
    ExpectedValue = (1/6) * Σ  value(state, die=d)  for d = 1..6

Level 2 — MIN (Opponent's turn):
    For each die value, the opponent has a set of legal moves.
    The opponent picks the move that MINIMIZES the AI's heuristic value.
    If no legal moves exist, evaluate the current state directly.
```

#### Pseudocode

```
function expectiminimax(state, depth, isMaxPlayer):
    if depth == 0 or isTerminal(state):
        return heuristic(state)

    if node is CHANCE:
        value = 0
        for die = 1 to 6:
            state.dieRoll = die
            value += (1/6) * expectiminimax(state, depth, not CHANCE)
        return value

    if isMaxPlayer:  // MAX node
        bestValue = -∞
        for each move in getLegalMoves(state):
            child = applyMove(state, move)
            value = expectiminimax(child, depth-1, CHANCE)
            bestValue = max(bestValue, value)
        return bestValue

    else:  // MIN node
        bestValue = +∞
        for each move in getLegalMoves(state):
            child = applyMove(state, move)
            value = expectiminimax(child, depth-1, CHANCE)
            bestValue = min(bestValue, value)
        return bestValue
```

### 2.8 Console Interface (`ConsoleUI`)

The console displays the board in a text-based format showing:

- All 24 points with piece counts and colors
- Bar contents for both players
- Borne-off counts for both players
- The current die roll
- Numbered list of legal moves for the human player
- AI's chosen move displayed after computation

Example board display:

```
 13  14  15  16  17  18  |BAR|  19  20  21  22  23  24
  O               X      |   |   X                O
  O               X      |   |   X                O
  O               X      |   |   X
  O                       |   |
  O                       |   |
 ─────────────────────────────────────────────────────
  X                       |   |
  X                       |   |
  X               O       |   |   O
  X               O       |   |   O                X
  X               O       |   |   O                X
 12  11  10   9   8   7  |BAR|   6   5   4   3   2   1

 White (You): Bar=0  Off=0  |  Black (AI): Bar=0  Off=0
 Die roll: 4
```

### 2.9 Game Loop (`Main`)

```
1. Initialize the board to the standard starting position.
2. Determine who goes first (each player rolls one die; higher goes first).
3. Loop until game is over:
   a. Roll one die (random 1–6) for the current player.
   b. Generate legal moves.
   c. If no legal moves → display "no moves, turn forfeited", switch player.
   d. If human's turn → display board, list legal moves, prompt for choice.
   e. If AI's turn → run Expectiminimax, display the chosen move.
   f. Apply the chosen move, update state.
   g. Display updated board.
   h. Check terminal condition → if met, announce winner and end.
   i. Switch current player.
4. Display final result.
```

### 2.10 Compilation and Execution

```bash
# Compile
javac -d out src/backgammon/model/*.java src/backgammon/logic/*.java \
      src/backgammon/ai/*.java src/backgammon/ui/*.java src/backgammon/Main.java

# Run
java -cp out backgammon.Main
```

---

## 3. References

- Russell, S. & Norvig, P. *Artificial Intelligence: A Modern Approach*. Section 6.5 —
  Games That Include an Element of Chance.
- Backgammon rules: https://www.ludoteka.com/clasika/backgammon-es.html
- Backgammon Galaxy guide: https://www.backgammongalaxy.com/how-to-play-backgammon
- Wikipedia — Backgammon: https://en.wikipedia.org/wiki/Backgammon
- Course slides: IIA-4 — Juegos 2026-1 & IIA-Taller4 — Juegos Backgamon 2026-1
