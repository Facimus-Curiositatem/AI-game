# Backgammon with Expectiminimax AI

A simplified Backgammon game featuring an AI opponent powered by the
**Expectiminimax** algorithm — a variant of MIN-MAX designed for games with
stochastic elements (dice).

---

## Project Overview

This project implements the classic board game **Backgammon** with a simplified
ruleset (single die, no doubling cube, no bets) and an AI opponent that uses
the **Expectiminimax** search algorithm at depth 2 to decide its moves.

The human plays as **White (O)** and the AI plays as **Black (X)**. The game
runs entirely in the terminal with a text-based board display. The AI evaluates
positions using a heuristic function composed of six weighted indicators that
capture race progress, piece safety, board control, and bearing-off readiness.

Built in **Java 17** using an object-oriented architecture.

---

## Game Rules Summary

This implementation uses a **simplified ruleset**. For full details see
[PLAN.md](PLAN.md).

### Simplifications

| Standard Backgammon          | This Version                                   |
|------------------------------|------------------------------------------------|
| Two dice per turn            | **One die** per turn (values 1-6)              |
| Doubles give 4 moves         | No doubles mechanic                           |
| Doubling cube for bets        | **No doubling cube, no bets**                 |
| Gammon / Backgammon scoring   | **Simple win/lose** (first to bear off wins)  |

### Core Rules

- **Board**: 24 points, 15 pieces per player, standard starting position.
- **Movement**: Roll one die (1-6), move one piece by exactly that many points.
  White moves 24 toward 1; Black moves 1 toward 24.
- **Legal destinations**: Empty points, points with your own pieces, or points
  with exactly one opponent piece (a **blot** — it gets **hit** and sent to
  the **bar**).
- **Blocked points**: A point with 2+ opponent pieces cannot be landed on.
- **Bar re-entry**: A player with pieces on the bar **must** re-enter them into
  the opponent's home board before making any other move. If the entry point is
  blocked, the turn is forfeited.
- **Bearing off**: Allowed only when **all 15 pieces** are in the home board.
  An exact roll bears off from that point. A roll higher than the highest
  occupied point bears off from the highest occupied point. A lower roll
  requires moving within the home board.
- **Winning**: First player to bear off all 15 pieces wins.

---

## AI Approach

### Why Expectiminimax?

Standard **MIN-MAX** assumes a fully deterministic game: one player maximizes,
the other minimizes, and both have complete control over their moves. But
Backgammon introduces **randomness through dice rolls** — neither player knows
what value they will roll next. This means the search tree must account for
**chance events** between decision layers.

**Expectiminimax** solves this by adding a third type of node — the **CHANCE
node** — between MAX and MIN layers. Instead of assuming a specific die outcome,
the CHANCE node computes the **expected value** (weighted average) across all
possible die outcomes.

> Reference: Russell & Norvig, *Artificial Intelligence: A Modern Approach*,
> Section 6.5 — "Games That Include an Element of Chance."

### Tree Structure (Depth 2)

The AI searches 2 levels of play with the following node structure:

```
                      +-----------+
                      | MAX (AI)  |   Level 0: AI knows its die roll.
                      | die = d0  |   Picks the move that MAXIMIZES value.
                      +-----+-----+
                            |
              +-------------+-------------+
              |                           |
         [Move A]                    [Move B]          ... (all legal moves)
              |                           |
        +-----+-----+              +------+------+
        |  CHANCE   |              |   CHANCE    |     Level 1: Unknown future
        | avg d=1-6 |              |  avg d=1-6  |     die roll. Average over
        +-----+-----+              +------+------+     all 6 outcomes (1/6 each).
              |                           |
     +--------+--------+        +--------+--------+
     |    |   ...  |    |        |    |   ...  |    |
   d=1  d=2      d=5  d=6     d=1  d=2      d=5  d=6
     |    |        |    |        |    |        |    |
  +--+--+ ...   +--+--+      +--+--+ ...   +--+--+
  | MIN |       | MIN |      | MIN |       | MIN |    Level 2: Opponent picks
  +--+--+       +--+--+      +--+--+       +--+--+    the move that MINIMIZES
     |             |             |             |       the AI's heuristic value.
  H(leaf)       H(leaf)      H(leaf)       H(leaf)    Leaves evaluated by H().
```

### Worked Example: AI Choosing a Move

Consider a mid-game scenario where it is the AI's turn (BLACK) and the die
roll is **3**. Suppose the AI has two legal moves:

- **Move A**: Move a piece from point 6 to point 9
- **Move B**: Move a piece from point 13 to point 16

The AI must evaluate which move leads to a better expected outcome.

#### Step 1 — Expand MAX node (AI, die = 3)

The AI tries both moves and needs the expected value of each:

```
               MAX (AI, die=3)
              /                \
         Move A               Move B
        (6 -> 9)            (13 -> 16)
            |                    |
        CHANCE_A             CHANCE_B
```

#### Step 2 — Expand CHANCE nodes (average over opponent's die)

For **Move A**, the CHANCE node averages over all possible die values the
opponent (WHITE) could roll next:

```
               CHANCE_A
     /    /    |    |    \    \
   d=1  d=2  d=3  d=4  d=5  d=6
    |    |    |    |    |    |
  MIN   MIN  MIN  MIN  MIN  MIN
```

Each die value leads to a MIN node where the opponent chooses their best
response (the move that minimizes the AI's score).

#### Step 3 — Evaluate MIN nodes (opponent's response)

For each die value, the opponent generates their legal moves and picks the one
that produces the **lowest** heuristic score (worst for AI). If the opponent
has no legal moves (turn forfeited), the state is evaluated directly.

Suppose after expanding, the MIN nodes produce these heuristic values:

| Die | Move A: MIN result | Move B: MIN result |
|-----|-------------------:|-------------------:|
| 1   |              +4.5  |              +2.0  |
| 2   |              +3.0  |              +5.5  |
| 3   |              +1.0  |              +3.0  |
| 4   |              +2.5  |              +4.0  |
| 5   |              -1.0  |              +1.5  |
| 6   |              +0.5  |              +3.5  |

#### Step 4 — Compute expected values at CHANCE nodes

```
E(Move A) = (1/6) * (4.5 + 3.0 + 1.0 + 2.5 + (-1.0) + 0.5)
          = (1/6) * 10.5
          = 1.75

E(Move B) = (1/6) * (2.0 + 5.5 + 3.0 + 4.0 + 1.5 + 3.5)
          = (1/6) * 19.5
          = 3.25
```

#### Step 5 — MAX chooses the best move

```
MAX picks: Move B  (E = 3.25 > 1.75)
```

The AI selects **Move B** (13 -> 16) because its expected heuristic value
across all possible opponent dice outcomes is higher. Even though Move A has
the single best case (d=1 yields +4.5), Move B is **more consistently good**
across all dice values, which is exactly what Expectiminimax optimizes for.

---

### Heuristic Function

The heuristic evaluates a board state from the AI's perspective using a
**weighted linear combination** of six indicators:

```
H(state) = w1 * PipCountDiff
         + w2 * BlotsDiff
         + w3 * BarDiff
         + w4 * BorneOffDiff
         + w5 * MadePointsDiff
         + w6 * HomeBoardDiff
```

Each indicator is computed as a **difference** (AI advantage minus opponent
advantage), so **positive values favor the AI** and negative values favor the
human.

| #  | Indicator            | What It Measures                                         | Weight |
|----|----------------------|----------------------------------------------------------|:------:|
| 1  | **Pip Count Diff**   | (Opponent pips - AI pips). Lower pip count = ahead in the race. | 1.0 |
| 2  | **Blots Diff**       | (Opponent blots - AI blots). Fewer exposed pieces = safer position. | 2.0 |
| 3  | **Bar Diff**         | (Opponent bar - AI bar). Opponent on bar = they lose turns. | 3.0 |
| 4  | **Borne Off Diff**   | (AI borne off - Opponent borne off). Direct winning progress. | 4.0 |
| 5  | **Made Points Diff** | (AI anchors - Opponent anchors). Points with 2+ pieces block the opponent. | 1.5 |
| 6  | **Home Board Diff**  | (AI home pieces - Opponent home pieces). Readiness to bear off. | 1.0 |

Terminal states bypass the heuristic entirely:
- AI wins: H = **+10000**
- Human wins: H = **-10000**

### Heuristic Calculation Example

Consider the following mid-game board state (simplified for clarity):

```
 Points with pieces:
   Point  1: 2 White       Point 19: 3 White
   Point  3: 1 White       Point 20: 1 Black
   Point  6: 3 Black       Point 21: 2 Black
   Point  8: 2 Black       Point 22: 3 Black
   Point 12: 2 White       Point 24: 1 Black
   Point 13: 4 Black

 Bar:   White = 0,  Black = 1
 Off:   White = 2,  Black = 1
```

**Indicator 1 — Pip Count Diff** (opponent pips - AI pips):

```
White pips: (2*1) + (1*3) + (2*12) + (3*19) = 2 + 3 + 24 + 57 = 86
  (remaining 2 pieces already borne off, not counted)

Black pips: (3*19) + (4*12) + (2*17) + (1*5) + (3*3) + (1*1) + (1*25 bar)
  Recalculated from Black's perspective (distance to bear off):
  Point  6 (idx 5):  3 pieces * (24-5)  = 3 * 19 = 57
  Point  8 (idx 7):  2 pieces * (24-7)  = 2 * 17 = 34
  Point 13 (idx 12): 4 pieces * (24-12) = 4 * 12 = 48
  Point 20 (idx 19): 1 piece  * (24-19) = 1 * 5  = 5
  Point 21 (idx 20): 2 pieces * (24-20) = 2 * 4  = 8
  Point 22 (idx 21): 3 pieces * (24-21) = 3 * 3  = 9
  Point 24 (idx 23): 1 piece  * (24-23) = 1 * 1  = 1
  Bar: 1 piece * 25 = 25
  Total Black pips = 57 + 34 + 48 + 5 + 8 + 9 + 1 + 25 = 187

PipCountDiff = WhitePips - BlackPips = 86 - 187 = -101
```

**Indicator 2 — Blots Diff** (opponent blots - AI blots):

```
White blots: Point 3 has 1 piece = 1 blot.    Total = 1
Black blots: Point 20 has 1 piece, Point 24 has 1 piece.  Total = 2

BlotsDiff = WhiteBlots - BlackBlots = 1 - 2 = -1
```

**Indicator 3 — Bar Diff** (opponent bar - AI bar):

```
BarDiff = WhiteBar - BlackBar = 0 - 1 = -1
```

**Indicator 4 — Borne Off Diff** (AI off - opponent off):

```
BorneOffDiff = BlackOff - WhiteOff = 1 - 2 = -1
```

**Indicator 5 — Made Points Diff** (AI anchors - opponent anchors):

```
White made points (2+ pieces): Point 1 (2), Point 12 (2), Point 19 (3) = 3
Black made points (2+ pieces): Point 6 (3), Point 8 (2), Point 13 (4),
                                Point 21 (2), Point 22 (3) = 5

MadePointsDiff = 5 - 3 = +2
```

**Indicator 6 — Home Board Diff** (AI home - opponent home):

```
White home (points 1-6): Point 1 (2) + Point 3 (1) = 3 pieces
Black home (points 19-24): Point 20 (1) + Point 21 (2) + Point 22 (3)
                           + Point 24 (1) = 7 pieces

HomeBoardDiff = 7 - 3 = +4
```

**Final Heuristic Score**:

```
H = (1.0 * -101)          Pip count:   -101.0
  + (2.0 * -1)             Blots:        -2.0
  + (3.0 * -1)             Bar:          -3.0
  + (4.0 * -1)             Borne off:    -4.0
  + (1.5 * +2)             Made points:  +3.0
  + (1.0 * +4)             Home board:   +4.0
  ─────────────────────────────────────────────
H = -103.0
```

**Interpretation**: H = -103.0 (negative = unfavorable for AI). The dominant
factor is the **pip count** — the AI is significantly behind in the race
(187 pips vs 86). Despite having more made points (+2) and more pieces in its
home board (+4), the massive pip count deficit makes this a losing position. The
AI would avoid moves that lead to this state.

---

### Algorithm Pseudocode

```
function chooseMove(state):
    bestMove = null
    bestValue = -infinity
    for each move in getLegalMoves(state):
        child = applyMove(state, move)
        value = chanceNode(child, depth=1)
        if value > bestValue:
            bestValue = value
            bestMove = move
    return bestMove

function chanceNode(state, depth):
    if depth == 0 or isTerminal(state):
        return heuristic(state)
    expectedValue = 0
    for die = 1 to 6:
        state.dieRoll = die
        if currentPlayer is MAX:
            expectedValue += (1/6) * maxNode(state, depth)
        else:
            expectedValue += (1/6) * minNode(state, depth)
    return expectedValue

function maxNode(state, depth):
    if depth == 0 or isTerminal(state):
        return heuristic(state)
    bestValue = -infinity
    for each move in getLegalMoves(state):
        child = applyMove(state, move)
        child.currentPlayer = opponent
        value = chanceNode(child, depth - 1)
        bestValue = max(bestValue, value)
    return bestValue

function minNode(state, depth):
    if depth == 0 or isTerminal(state):
        return heuristic(state)
    bestValue = +infinity
    for each move in getLegalMoves(state):
        child = applyMove(state, move)
        child.currentPlayer = opponent
        value = chanceNode(child, depth - 1)
        bestValue = min(bestValue, value)
    return bestValue
```

---

## Gameplay

```
==============================================================
       BACKGAMMON — Simplified (1 Die, No Bets)
       AI powered by Expectiminimax (Depth 2)
==============================================================
  You play as White (O). AI plays as Black (X).
  White moves: 24 -> 1  |  Black moves: 1 -> 24
==============================================================

 Determining who goes first...
 White rolls: 5  |  Black rolls: 2
 White (Human) goes first with a roll of 5!

 White (Human) rolls a 2.

==============================================================
  13  14  15  16  17  18  |BAR|  19  20  21  22  23  24
  X   .   .   .   O   .   |   |  O   .   .   .   .   X
  X   .   .   .   O   .   |0,0|  O   .   .   .   .   X
  X   .   .   .   O   .   |   |  O   .   .   .   .   .
  X   .   .   .   .   .   |   |  O   .   .   .   .   .
  X   .   .   .   .   .   |   |  O   .   .   .   .   .
 ------------------------------------------------------------
  O   .   .   .   .   .   |   |  X   .   .   .   .   .
  O   .   .   .   .   .   |   |  X   .   .   .   .   .
  O   .   .   .   X   .   |   |  X   .   .   .   .   .
  O   .   .   .   X   .   |   |  X   .   .   .   .   O
  O   .   .   .   X   .   |   |  X   .   .   .   .   O
  12  11  10   9   8   7  |BAR|   6   5   4   3   2   1
==============================================================
 White (O - Human): Bar=0  Off=0  |  Black (X - AI): Bar=0  Off=0
 Current player: White (Human)  |  Die roll: 2

 Your legal moves:
   [1] 12 -> 10
   [2] 17 -> 15
   [3] 19 -> 17

 Choose a move (1-3): 1
 You chose: 12 -> 10

 Black (AI) rolls a 4.
 AI is thinking...
 AI chose: 6 -> 10
```

---

## Prerequisites

- **Java 17** or later (JDK, not just JRE — needed for compilation)
- A terminal / command prompt

Verify your Java version:

```bash
java -version
javac -version
```

---

## Build & Run

```bash
# Clone the repository
git clone https://github.com/Facimus-Curiositatem/AI-game.git
cd AI-game

# Compile all source files
javac -d out src/backgammon/model/*.java src/backgammon/logic/*.java \
      src/backgammon/ai/*.java src/backgammon/ui/*.java src/backgammon/Main.java

# Run the game
java -cp out backgammon.Main
```

On Windows (if backslashes are needed):

```cmd
javac -d out src\backgammon\model\*.java src\backgammon\logic\*.java ^
      src\backgammon\ai\*.java src\backgammon\ui\*.java src\backgammon\Main.java

java -cp out backgammon.Main
```

---

## References

- Russell, S. & Norvig, P. *Artificial Intelligence: A Modern Approach*.
  Section 6.5 — Games That Include an Element of Chance.
- Backgammon rules (Spanish): https://www.ludoteka.com/clasika/backgammon-es.html
- Backgammon Galaxy — How to Play: https://www.backgammongalaxy.com/how-to-play-backgammon
- Wikipedia — Backgammon: https://en.wikipedia.org/wiki/Backgammon
- Detailed project plan and full ruleset: [PLAN.md](PLAN.md)
- Expectiminimax algorithm: https://en.wikipedia.org/wiki/Expectiminimax
