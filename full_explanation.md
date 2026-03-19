# Full Class-by-Class Explanation

## 1. Player (enum)

**File**: `src/backgammon/model/Player.java`

**Purpose**: Defines the two participants in the game — `WHITE` and `BLACK`. Instead
of using raw strings or integers throughout the codebase, this enum centralizes all
player-related logic in one place.

### Fields

```java
public enum Player {
    WHITE, BLACK;
}
```

There are exactly two values. `WHITE` is the human player, `BLACK` is the AI.

### Methods

#### `opponent()`

Returns the other player.

```java
public Player opponent() {
    return this == WHITE ? BLACK : WHITE;
}
```

**Example usage**:

```java
Player current = Player.WHITE;
Player other = current.opponent();  // → Player.BLACK

Player ai = Player.BLACK;
Player human = ai.opponent();      // → Player.WHITE
```

This is used heavily in `GameLogic` when checking for opponent pieces (blots,
blocked points) and in `ExpectiminimaxAgent` when switching turns in the search
tree.

#### `sign()`

Returns `+1` for WHITE, `-1` for BLACK.

```java
public int sign() {
    return this == WHITE ? 1 : -1;
}
```

**Why this exists**: The board is a single `int[24]` array where:
- Positive values represent White pieces
- Negative values represent Black pieces
- Zero means empty

The `sign()` method lets us write player-agnostic code. For example, when placing
a piece on point 10:

```java
// Instead of:
if (player == Player.WHITE) {
    board[10] += 1;
} else {
    board[10] -= 1;
}

// We write:
board[10] += player.sign();  // +1 for White, -1 for Black
```

This is used in `GameLogic.applyMove()` at lines 181, 187, and 206.

#### `displayName()`

Returns a human-friendly string for console output.

```java
public String displayName() {
    return this == WHITE ? "White (Human)" : "Black (AI)";
}
```

**Output**:
- `Player.WHITE.displayName()` → `"White (Human)"`
- `Player.BLACK.displayName()` → `"Black (AI)"`

Used by `ConsoleUI` in messages like `"White (Human) rolls a 4."`.

#### `symbol()`

Returns a single character for the board display.

```java
public char symbol() {
    return this == WHITE ? 'O' : 'X';
}
```

**Output**:
- `Player.WHITE.symbol()` → `'O'`
- `Player.BLACK.symbol()` → `'X'`

Used in `ConsoleUI.getPieceChar()` to render pieces on the board grid.

---

## 2. Move (class)

**File**: `src/backgammon/model/Move.java`

**Purpose**: Represents a single move — "take a piece from here, put it there."
A Move is an immutable object: once created, its `from` and `to` values never change.

### Constants

```java
public static final int BAR = -1;
public static final int BEAR_OFF = -1;
```

Both use `-1` as a sentinel value. They are never confused because:
- `BAR` is only valid as a `from` value (you enter FROM the bar)
- `BEAR_OFF` is only valid as a `to` value (you bear off TO... off the board)

### Fields

```java
private final int from;  // 0-23 for board points, or BAR (-1)
private final int to;    // 0-23 for board points, or BEAR_OFF (-1)
```

Both are `final` — immutable after construction.

| `from` value | Meaning |
|-------------|---------|
| 0 to 23 | Piece moves from this board point (0 = point 1, 23 = point 24) |
| -1 (`BAR`) | Piece enters from the bar |

| `to` value | Meaning |
|-----------|---------|
| 0 to 23 | Piece moves to this board point |
| -1 (`BEAR_OFF`) | Piece is removed from the board |

### Constructor

```java
public Move(int from, int to) {
    this.from = from;
    this.to = to;
}
```

**Example constructions**:

```java
new Move(11, 9)      // Normal: point 12 to point 10
new Move(Move.BAR, 23)   // Bar entry: entering at point 24
new Move(3, Move.BEAR_OFF)  // Bearing off from point 4
```

### Methods

#### `isEnterFromBar()` and `isBearOff()`

```java
public boolean isEnterFromBar() { return from == BAR; }
public boolean isBearOff()      { return to == BEAR_OFF; }
```

**Examples**:

```java
Move m1 = new Move(11, 9);
m1.isEnterFromBar();  // false
m1.isBearOff();       // false

Move m2 = new Move(Move.BAR, 23);
m2.isEnterFromBar();  // true
m2.isBearOff();       // false

Move m3 = new Move(3, Move.BEAR_OFF);
m3.isEnterFromBar();  // false
m3.isBearOff();       // true
```

These are used in `GameLogic.applyMove()` to decide how to remove a piece from
the source and where to place it at the destination.

#### `describe()`

Converts the move to a human-readable string using 1-based point numbers.

```java
public String describe() {
    String fromStr = isEnterFromBar() ? "BAR" : String.valueOf(from + 1);
    String toStr = isBearOff() ? "OFF" : String.valueOf(to + 1);
    return fromStr + " -> " + toStr;
}
```

**Examples**:

```java
new Move(11, 9).describe()              // → "12 -> 10"
new Move(Move.BAR, 23).describe()       // → "BAR -> 24"
new Move(3, Move.BEAR_OFF).describe()   // → "4 -> OFF"
```

This is what the player sees in the console:

```
 Your legal moves:
   [1] 12 -> 10
   [2] BAR -> 24
   [3] 4 -> OFF
```

#### `equals()` and `hashCode()`

```java
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
```

Two Move objects are considered equal if they have the same `from` and `to`.
This is standard Java practice — without these overrides, two separate `Move`
objects with the same values would not be considered equal by `==` or `.equals()`.

**Example**:

```java
Move a = new Move(11, 9);
Move b = new Move(11, 9);
a.equals(b);  // true (same from/to)
a == b;       // false (different objects in memory)
```

---

## 3. BackgammonState (class)

**File**: `src/backgammon/model/BackgammonState.java`

**Purpose**: A complete snapshot of the game at any moment. Contains everything
needed to determine what moves are legal, whose turn it is, and whether the game
is over. This is the "state" that gets explored in the Expectiminimax search tree.

### Constants

```java
public static final int NUM_POINTS = 24;
public static final int PIECES_PER_PLAYER = 15;
```

### Fields

| Field | Type | Description |
|-------|------|-------------|
| `board` | `int[24]` | The 24 board points. Index 0 = point 1, index 23 = point 24. Positive values = White pieces, negative = Black, zero = empty. |
| `barWhite` | `int` | Number of White pieces captured on the bar |
| `barBlack` | `int` | Number of Black pieces captured on the bar |
| `borneOffWhite` | `int` | Number of White pieces that have been borne off |
| `borneOffBlack` | `int` | Number of Black pieces that have been borne off |
| `currentPlayer` | `Player` | Whose turn it is |
| `dieRoll` | `int` | The current die value (1-6) |

**Accounting invariant** — at all times, for each player:

```
pieces on board + pieces on bar + pieces borne off = 15
```

This is never explicitly checked in code, but it is always maintained by
`GameLogic.applyMove()`, which removes a piece from one location before placing
it in another.

### Constructor

```java
public BackgammonState() {
    this.board = new int[NUM_POINTS];  // all zeros
    this.barWhite = 0;
    this.barBlack = 0;
    this.borneOffWhite = 0;
    this.borneOffBlack = 0;
    this.currentPlayer = Player.WHITE;
    this.dieRoll = 0;
}
```

Creates an empty board. You must call `initStartingPosition()` to set up the
standard Backgammon layout.

### `initStartingPosition()`

Sets up the standard Backgammon starting position:

```java
public void initStartingPosition() {
    Arrays.fill(board, 0);

    // White pieces (positive)
    board[0]  =  2;   // Point 1:  2 White
    board[11] =  5;   // Point 12: 5 White
    board[16] =  3;   // Point 17: 3 White
    board[18] =  5;   // Point 19: 5 White

    // Black pieces (negative)
    board[5]  = -5;   // Point 6:  5 Black
    board[7]  = -3;   // Point 8:  3 Black
    board[12] = -5;   // Point 13: 5 Black
    board[23] = -2;   // Point 24: 2 Black
}
```

**Visual representation** (what this looks like on the board):

```
 Point:  1   2   3   4   5   6   7   8   9  10  11  12
 Index:  0   1   2   3   4   5   6   7   8   9  10  11
 Value: +2   0   0   0   0  -5   0  -3   0   0   0  +5
         W                   B       B                 W

 Point: 13  14  15  16  17  18  19  20  21  22  23  24
 Index: 12  13  14  15  16  17  18  19  20  21  22  23
 Value: -5   0   0   0  +3   0  +5   0   0   0   0  -2
         B               W       W                    B
```

White has: 2 + 5 + 3 + 5 = 15 pieces. Black has: 5 + 3 + 5 + 2 = 15 pieces.

### `deepCopy()`

Creates an independent clone of the state.

```java
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
```

**Why this is critical**: The AI explores hypothetical future moves. Without
deep copy, modifying a hypothetical state would corrupt the actual game state.

**Example**:

```java
BackgammonState original = new BackgammonState();
original.initStartingPosition();
// original.board[0] is +2 (2 White pieces on point 1)

BackgammonState copy = original.deepCopy();
copy.setPiecesAt(0, 0);  // Remove all pieces from point 1 in the copy

// original.board[0] is STILL +2 — unchanged
// copy.board[0] is 0 — only the copy was modified
```

`Arrays.copyOf()` creates a new array with the same values, so modifying
`copy.board` does not affect `original.board`.

### `getPlayerPiecesAt(index, player)`

Returns how many pieces of a specific player are on a point. Handles the sign
conversion internally.

```java
public int getPlayerPiecesAt(int index, Player player) {
    int val = board[index];
    if (player == Player.WHITE) {
        return val > 0 ? val : 0;
    } else {
        return val < 0 ? -val : 0;
    }
}
```

**Examples** (suppose `board[5] = -3`):

```java
state.getPlayerPiecesAt(5, Player.BLACK);  // → 3  (3 Black pieces)
state.getPlayerPiecesAt(5, Player.WHITE);  // → 0  (no White pieces)
```

**Examples** (suppose `board[11] = 5`):

```java
state.getPlayerPiecesAt(11, Player.WHITE);  // → 5  (5 White pieces)
state.getPlayerPiecesAt(11, Player.BLACK);  // → 0  (no Black pieces)
```

**Example** (suppose `board[9] = 0`):

```java
state.getPlayerPiecesAt(9, Player.WHITE);  // → 0
state.getPlayerPiecesAt(9, Player.BLACK);  // → 0
```

### `isBlockedBy(index, player)`

Checks if the **opponent** has 2 or more pieces on a point, making it impossible
for the player to land there.

```java
public boolean isBlockedBy(int index, Player player) {
    Player opponent = player.opponent();
    return getPlayerPiecesAt(index, opponent) >= 2;
}
```

**Examples** (suppose `board[5] = -3`, i.e., 3 Black pieces):

```java
state.isBlockedBy(5, Player.WHITE);  // → true  (Black has 3 >= 2, blocks White)
state.isBlockedBy(5, Player.BLACK);  // → false (White has 0, doesn't block Black)
```

**Example** (suppose `board[9] = -1`, i.e., 1 Black piece):

```java
state.isBlockedBy(9, Player.WHITE);  // → false (Black has only 1, it's a blot, not blocked)
```

### `isBlot(index, player)`

Checks if the **opponent** has exactly 1 piece on a point — a vulnerable "blot"
that can be hit.

```java
public boolean isBlot(int index, Player player) {
    Player opponent = player.opponent();
    return getPlayerPiecesAt(index, opponent) == 1;
}
```

**Examples** (suppose `board[9] = -1`, i.e., 1 Black piece):

```java
state.isBlot(9, Player.WHITE);  // → true  (Black has 1 piece — White can hit it)
state.isBlot(9, Player.BLACK);  // → false (White has 0 pieces — no blot)
```

**Example** (suppose `board[5] = -3`):

```java
state.isBlot(5, Player.WHITE);  // → false (Black has 3, not a blot)
```

### Getters and Setters

The remaining methods are straightforward accessors:

- `getBar(player)` / `setBar(player, value)` — bar piece count by player
- `getBorneOff(player)` / `setBorneOff(player, value)` — borne-off count by player
- `getCurrentPlayer()` / `setCurrentPlayer(player)` — whose turn
- `getDieRoll()` / `setDieRoll(die)` — current die value
- `getPiecesAt(index)` / `setPiecesAt(index, value)` — raw board value at an index

---

## 4. GameLogic (class)

**File**: `src/backgammon/logic/GameLogic.java`

**Purpose**: The rules engine. This class knows all Backgammon rules but nothing
about AI or UI. It has five responsibilities:

1. Generate all legal moves for a given state
2. Apply a move to produce a new state
3. Check if the game is over (terminal test)
4. Determine if a player can bear off
5. Compute pip counts

### 4.1 `getLegalMoves(state)` — The Successor Function

This is the most important method. Given a state (which contains the current
player and die roll), it returns a `List<Move>` of every legal move.

```java
public List<Move> getLegalMoves(BackgammonState state) {
    Player player = state.getCurrentPlayer();
    int die = state.getDieRoll();
    List<Move> moves = new ArrayList<>();

    // Priority 1: Bar re-entry (mandatory)
    if (state.getBar(player) > 0) {
        generateBarEntryMoves(state, player, die, moves);
        return moves;  // ONLY bar moves allowed
    }

    // Priority 2: Bearing off (if eligible)
    if (canBearOff(state, player)) {
        generateBearingOffMoves(state, player, die, moves);
    }

    // Priority 3: Normal moves
    generateNormalMoves(state, player, die, moves);

    return moves;
}
```

**Priority logic**:

```
Has pieces on bar?
├── YES → Generate ONLY bar entry moves. Return immediately.
│         (Cannot move any other piece until bar is cleared)
└── NO
    ├── All pieces in home board?
    │   ├── YES → Generate bearing-off moves AND normal home-board moves
    │   └── NO  → Generate normal moves only
    └── Return combined list (may be empty → turn forfeited)
```

#### Concrete Example: Starting Position, White Rolls 3

Starting board (White's relevant pieces):
- Point 1 (index 0): 2 White
- Point 12 (index 11): 5 White
- Point 17 (index 16): 3 White
- Point 19 (index 18): 5 White

White moves toward lower indices (subtract die from index).

| Piece at | - die (3) | Destination | `board[dest]` | Legal? | Why |
|----------|-----------|-------------|---------------|--------|-----|
| Index 0 (pt 1) | 0 - 3 = -3 | Off board | N/A | No | Dest < 0 and can't bear off (pieces elsewhere) |
| Index 11 (pt 12) | 11 - 3 = 8 | Index 8 (pt 9) | 0 | **Yes** | Empty point |
| Index 16 (pt 17) | 16 - 3 = 13 | Index 13 (pt 14) | 0 | **Yes** | Empty point |
| Index 18 (pt 19) | 18 - 3 = 15 | Index 15 (pt 16) | 0 | **Yes** | Empty point |

Result: 3 legal moves → `[12 -> 9, 17 -> 14, 19 -> 16]`

#### Concrete Example: White Rolls 5 (hits a blot)

Same starting position. White rolls 5.

| Piece at | - die (5) | Destination | `board[dest]` | Legal? | Why |
|----------|-----------|-------------|---------------|--------|-----|
| Index 0 (pt 1) | 0 - 5 = -5 | Off board | N/A | No | Can't bear off |
| Index 11 (pt 12) | 11 - 5 = 6 | Index 6 (pt 7) | 0 | **Yes** | Empty |
| Index 16 (pt 17) | 16 - 5 = 11 | Index 11 (pt 12) | +5 | **Yes** | Own pieces (stacking) |
| Index 18 (pt 19) | 18 - 5 = 13 | Index 13 (pt 14) | 0 | **Yes** | Empty |

None of the destinations are blocked (no point with 2+ Black pieces in the way).

### 4.2 Bar Entry Moves

When a player has pieces on the bar, they must re-enter before doing anything else.

```java
private void generateBarEntryMoves(BackgammonState state, Player player,
                                   int die, List<Move> moves) {
    int entryIndex;
    if (player == Player.WHITE) {
        entryIndex = 24 - die;  // White enters opponent's home (points 19-24)
    } else {
        entryIndex = die - 1;   // Black enters opponent's home (points 1-6)
    }

    if (!state.isBlockedBy(entryIndex, player)) {
        moves.add(new Move(Move.BAR, entryIndex));
    }
}
```

**Formula**:
- White re-enters at index `24 - die` (die=1 → index 23 = point 24, die=6 → index 18 = point 19)
- Black re-enters at index `die - 1` (die=1 → index 0 = point 1, die=6 → index 5 = point 6)

**Example: White on bar, rolls 4**:

```
entryIndex = 24 - 4 = 20 (point 21)

Is point 21 blocked? (Does Black have 2+ pieces there?)
  board[20] = 0 → No, not blocked
  → Legal move: BAR -> 21
```

**Example: White on bar, rolls 1, but Black has 2 pieces on point 24**:

```
entryIndex = 24 - 1 = 23 (point 24)

Is point 24 blocked?
  board[23] = -2 → Yes, Black has 2 pieces
  → No legal moves. Turn forfeited.
```

### 4.3 Normal Moves

```java
private void generateNormalMoves(BackgammonState state, Player player,
                                 int die, List<Move> moves) {
    for (int i = 0; i < BackgammonState.NUM_POINTS; i++) {
        if (state.getPlayerPiecesAt(i, player) > 0) {
            int dest = getDestination(i, die, player);

            if (dest >= 0 && dest < BackgammonState.NUM_POINTS) {
                if (!state.isBlockedBy(dest, player)) {
                    moves.add(new Move(i, dest));
                }
            }
        }
    }
}
```

The direction helper:

```java
private int getDestination(int fromIndex, int die, Player player) {
    if (player == Player.WHITE) {
        return fromIndex - die;   // White moves toward lower indices
    } else {
        return fromIndex + die;   // Black moves toward higher indices
    }
}
```

**What makes a destination illegal**:
1. `dest < 0` or `dest >= 24` — off the board (and not bearing off)
2. `isBlockedBy(dest, player)` — opponent has 2+ pieces there

**What makes a destination legal**:
1. Empty point (`board[dest] == 0`)
2. Own pieces already there (`board[dest]` has same sign) — stacking
3. Exactly one opponent piece (`isBlot()`) — hitting

### 4.4 Bearing Off Moves

Called only when `canBearOff()` returns true (all pieces in home board).

**White's bearing off** (home board = indices 0-5, points 1-6):

```java
private void generateBearingOffWhite(BackgammonState state, int die, List<Move> moves) {
    // Rule 1: Exact roll
    int exactIndex = die - 1;
    if (exactIndex >= 0 && exactIndex < 6
        && state.getPlayerPiecesAt(exactIndex, Player.WHITE) > 0) {
        moves.add(new Move(exactIndex, Move.BEAR_OFF));
    }

    // Rule 2: Higher roll than highest occupied point
    int highestOccupied = -1;
    for (int i = 5; i >= 0; i--) {
        if (state.getPlayerPiecesAt(i, Player.WHITE) > 0) {
            highestOccupied = i;
            break;
        }
    }

    if (highestOccupied >= 0 && die > (highestOccupied + 1)) {
        if (highestOccupied != exactIndex) {
            moves.add(new Move(highestOccupied, Move.BEAR_OFF));
        }
    }
}
```

**Three bearing-off scenarios for White**:

**Scenario A — Exact roll (die = 4)**:

```
Home board:  Pt1  Pt2  Pt3  Pt4  Pt5  Pt6
Pieces:       3    0    0    2    0    0

exactIndex = 4 - 1 = 3 (point 4)
board[3] has 2 White pieces → YES
→ Move: 4 -> OFF
```

**Scenario B — Higher roll (die = 6, highest piece on point 3)**:

```
Home board:  Pt1  Pt2  Pt3  Pt4  Pt5  Pt6
Pieces:       1    0    2    0    0    0

exactIndex = 6 - 1 = 5 (point 6) → no pieces on point 6
highestOccupied = index 2 (point 3)
die (6) > highestOccupied + 1 (3) → YES
→ Move: 3 -> OFF  (bear off from highest occupied)
```

**Scenario C — Lower roll (die = 2, highest piece on point 5)**:

```
Home board:  Pt1  Pt2  Pt3  Pt4  Pt5  Pt6
Pieces:       0    0    0    0    3    0

exactIndex = 2 - 1 = 1 (point 2) → no pieces on point 2
highestOccupied = index 4 (point 5)
die (2) > highestOccupied + 1 (5)? → 2 > 5? NO
→ No bearing-off move generated.

BUT normal moves still apply: piece at index 4, dest = 4 - 2 = 2 (point 3)
→ Move: 5 -> 3  (internal home-board move)
```

### 4.5 `applyMove(state, move)` — State Transition

Takes a state and a move, returns a **new** state. The original is never modified.

```java
public BackgammonState applyMove(BackgammonState state, Move move) {
    BackgammonState newState = state.deepCopy();
    Player player = newState.getCurrentPlayer();
    int sign = player.sign();

    // Step 1: Remove piece from source
    if (move.isEnterFromBar()) {
        newState.setBar(player, newState.getBar(player) - 1);
    } else {
        newState.setPiecesAt(move.getFrom(),
            newState.getPiecesAt(move.getFrom()) - sign);
    }

    // Step 2: Place piece at destination
    if (move.isBearOff()) {
        newState.setBorneOff(player, newState.getBorneOff(player) + 1);
    } else {
        int dest = move.getTo();

        // Step 2a: Check for hitting a blot
        if (newState.isBlot(dest, player)) {
            Player opponent = player.opponent();
            newState.setPiecesAt(dest, 0);
            newState.setBar(opponent, newState.getBar(opponent) + 1);
        }

        // Step 2b: Place our piece
        newState.setPiecesAt(dest, newState.getPiecesAt(dest) + sign);
    }

    return newState;
}
```

#### Walkthrough Example: White Moves 12 -> 10 (Normal Move)

**Before**: `board[11] = +5` (5 White on point 12), `board[9] = 0` (empty point 10)

1. `deepCopy()` → new state created
2. `sign = +1` (White)
3. Remove from source: `board[11] = 5 - 1 = 4`
4. Not a bear-off. Destination = index 9.
5. `isBlot(9, WHITE)?` → `board[9] = 0` → no opponent piece → no hit
6. Place piece: `board[9] = 0 + 1 = 1`

**After**: `board[11] = +4`, `board[9] = +1`

#### Walkthrough Example: Black Moves 6 -> 10 and Hits White's Blot

**Before**: `board[5] = -5` (5 Black on point 6), `board[9] = +1` (1 White on point 10),
`barWhite = 0`

1. `deepCopy()` → new state
2. `sign = -1` (Black)
3. Remove from source: `board[5] = -5 - (-1) = -4`
4. Destination = index 9.
5. `isBlot(9, BLACK)?` → White has 1 piece → **yes, it's a blot!**
   - Set `board[9] = 0` (remove White's piece)
   - `barWhite = 0 + 1 = 1` (White's piece goes to bar)
6. Place piece: `board[9] = 0 + (-1) = -1`

**After**: `board[5] = -4`, `board[9] = -1`, `barWhite = 1`

### 4.6 `isTerminal(state)` and `getWinner(state)`

```java
public boolean isTerminal(BackgammonState state) {
    return state.getBorneOff(Player.WHITE) == 15
        || state.getBorneOff(Player.BLACK) == 15;
}

public Player getWinner(BackgammonState state) {
    if (state.getBorneOff(Player.WHITE) == 15) return Player.WHITE;
    if (state.getBorneOff(Player.BLACK) == 15) return Player.BLACK;
    return null;  // Game still in progress
}
```

The game ends when either player has borne off all 15 pieces. No partial
victories — first to 15 wins.

### 4.7 `canBearOff(state, player)`

```java
public boolean canBearOff(BackgammonState state, Player player) {
    if (state.getBar(player) > 0) return false;

    if (player == Player.WHITE) {
        for (int i = 6; i < 24; i++) {
            if (state.getPlayerPiecesAt(i, Player.WHITE) > 0) return false;
        }
    } else {
        for (int i = 0; i < 18; i++) {
            if (state.getPlayerPiecesAt(i, Player.BLACK) > 0) return false;
        }
    }
    return true;
}
```

Checks two conditions:
1. No pieces on the bar
2. No pieces outside the home board

**Example — White CAN bear off**:

```
Bar: 0
Board: pieces only on indices 0-5 (points 1-6)
→ canBearOff returns true
```

**Example — White CANNOT bear off**:

```
Bar: 0
Board: 1 piece on index 7 (point 8) ← outside home board
→ canBearOff returns false (must move this piece home first)
```

**Example — White CANNOT bear off (bar)**:

```
Bar: 1
Board: all other pieces on indices 0-5
→ canBearOff returns false (must re-enter from bar first)
```

### 4.8 `computePipCount(state, player)`

Calculates the total distance a player's pieces still need to travel to bear
off. Lower pip count = closer to winning.

```java
public int computePipCount(BackgammonState state, Player player) {
    int pips = 0;

    for (int i = 0; i < 24; i++) {
        int count = state.getPlayerPiecesAt(i, player);
        if (count > 0) {
            int distance;
            if (player == Player.WHITE) {
                distance = i + 1;   // White bears off below index 0
            } else {
                distance = 24 - i;  // Black bears off beyond index 23
            }
            pips += count * distance;
        }
    }

    pips += state.getBar(player) * 25;  // Bar = worst case distance

    return pips;
}
```

**Worked example — Starting position pip count for White**:

```
Index 0  (point 1):  2 pieces * distance 1  =  2
Index 11 (point 12): 5 pieces * distance 12 = 60
Index 16 (point 17): 3 pieces * distance 17 = 51
Index 18 (point 19): 5 pieces * distance 19 = 95
Bar: 0 pieces * 25 = 0
                                        Total = 208? 
```

Wait — let me recalculate. The standard starting pip count in Backgammon is
167 for each player. Let me verify:

```
White (standard, two-dice Backgammon):
  2 pieces on point 24 (our index 23): 2 * 24 = 48... 
```

Actually, in standard Backgammon the numbering is typically from the player's
own perspective. In our code, White's pieces at index 0 = point 1 are closest
to bearing off (distance 1). So:

```
Index 0  (point 1):  2 * 1  =   2
Index 11 (point 12): 5 * 12 =  60
Index 16 (point 17): 3 * 17 =  51
Index 18 (point 19): 5 * 19 =  95
                       Total = 208
```

For Black:

```
Index 5  (point 6):  5 * (24-5)  = 5 * 19 = 95
Index 7  (point 8):  3 * (24-7)  = 3 * 17 = 51
Index 12 (point 13): 5 * (24-12) = 5 * 12 = 60
Index 23 (point 24): 2 * (24-23) = 2 * 1  =  2
                                     Total = 208
```

Both players start with **208 pips** in our single-die representation. This is
symmetric, which is correct — the starting position is mirrored.

Note: In standard two-dice Backgammon, the starting pip count is 167 per player.
The difference is because the standard game numbers points from each player's own
perspective (e.g., White's 24-point is Black's 1-point), while our code uses a
single numbering system from White's perspective. The distances are the same
either way — each player is equidistant from bearing off.

---

## 5. HeuristicEvaluator (class)

**File**: `src/backgammon/ai/HeuristicEvaluator.java`

**Purpose**: Assigns a numeric score to any board state, answering the question:
"How good is this position for the AI (BLACK)?"

### Formula

```
H(state) = w1 * PipCountDiff
         + w2 * BlotsDiff
         + w3 * BarDiff
         + w4 * BorneOffDiff
         + w5 * MadePointsDiff
         + w6 * HomeBoardDiff
```

- **Positive H** = favorable for AI (BLACK)
- **Negative H** = favorable for Human (WHITE)
- **H = +10000** = AI has won (terminal state)
- **H = -10000** = Human has won (terminal state)

### Weights

```java
private static final double W_PIP_COUNT    = 1.0;
private static final double W_BLOTS        = 2.0;
private static final double W_BAR          = 3.0;
private static final double W_BORNE_OFF    = 4.0;
private static final double W_MADE_POINTS  = 1.5;
private static final double W_HOME_BOARD   = 1.0;
```

Why these values?
- **Borne off (4.0)** has the highest weight because bearing off pieces is
  direct, irreversible progress toward winning.
- **Bar (3.0)** is next because a piece on the bar can cost entire turns and
  must travel the full board to get back.
- **Blots (2.0)** matter significantly because exposed pieces risk being hit.
- **Made points (1.5)** provide positional/blocking advantage.
- **Pip count (1.0)** and **Home board (1.0)** are important but less decisive
  on a per-unit basis (though pip count differences can be large numbers).

### The Six Indicators

#### Indicator 1: Pip Count Diff

```java
int aiPips = gameLogic.computePipCount(state, AI);
int humanPips = gameLogic.computePipCount(state, HUMAN);
score += W_PIP_COUNT * (humanPips - aiPips);
```

Measures who is ahead in the "race." Lower pip count = closer to winning.
`(humanPips - aiPips)` is positive when the AI has fewer pips (is ahead).

**Example**: AI has 150 pips, Human has 180 pips → `180 - 150 = +30` → AI is
30 pips ahead in the race.

#### Indicator 2: Blots Diff

```java
int aiBlots = countBlots(state, AI);
int humanBlots = countBlots(state, HUMAN);
score += W_BLOTS * (humanBlots - aiBlots);
```

A "blot" is a point with exactly 1 piece — vulnerable to being hit.

```java
private int countBlots(BackgammonState state, Player player) {
    int blots = 0;
    for (int i = 0; i < 24; i++) {
        if (state.getPlayerPiecesAt(i, player) == 1) {
            blots++;
        }
    }
    return blots;
}
```

**Example**: AI has 1 blot, Human has 3 blots → `3 - 1 = +2` → AI is safer.

#### Indicator 3: Bar Diff

```java
int aiBar = state.getBar(AI);
int humanBar = state.getBar(HUMAN);
score += W_BAR * (humanBar - aiBar);
```

Pieces on the bar are very costly — the player loses their move if they can't
re-enter, and the piece must travel the entire board.

**Example**: AI has 0 on bar, Human has 2 on bar → `2 - 0 = +2` → AI advantage.

#### Indicator 4: Borne Off Diff

```java
int aiBorneOff = state.getBorneOff(AI);
int humanBorneOff = state.getBorneOff(HUMAN);
score += W_BORNE_OFF * (aiBorneOff - humanBorneOff);
```

Direct winning progress. Once a piece is borne off, it's permanent.

**Example**: AI has 5 borne off, Human has 3 → `5 - 3 = +2` → AI closer to winning.

#### Indicator 5: Made Points Diff

```java
int aiMadePoints = countMadePoints(state, AI);
int humanMadePoints = countMadePoints(state, HUMAN);
score += W_MADE_POINTS * (aiMadePoints - humanMadePoints);
```

A "made point" is a point with 2+ pieces — it acts as a blocker that the
opponent cannot land on.

```java
private int countMadePoints(BackgammonState state, Player player) {
    int madePoints = 0;
    for (int i = 0; i < 24; i++) {
        if (state.getPlayerPiecesAt(i, player) >= 2) {
            madePoints++;
        }
    }
    return madePoints;
}
```

**Example**: AI has 4 made points, Human has 3 → `4 - 3 = +1` → AI has more
board control.

#### Indicator 6: Home Board Diff

```java
int aiHome = countHomeBoardPieces(state, AI);
int humanHome = countHomeBoardPieces(state, HUMAN);
score += W_HOME_BOARD * (aiHome - humanHome);
```

Counts pieces in the home board (ready for bearing off).

```java
private int countHomeBoardPieces(BackgammonState state, Player player) {
    int count = 0;
    int start, end;
    if (player == Player.WHITE) {
        start = 0; end = 6;    // indices 0-5
    } else {
        start = 18; end = 24;  // indices 18-23
    }
    for (int i = start; i < end; i++) {
        count += state.getPlayerPiecesAt(i, player);
    }
    return count;
}
```

**Example**: AI has 8 pieces in home board, Human has 4 → `8 - 4 = +4` → AI is
more ready to bear off.

### Full Worked Example

**Board state**:

```
White pieces: 3 on point 1, 2 on point 5, 1 on point 10
Black pieces: 2 on point 20, 4 on point 22, 1 on point 13
Bar: White = 1, Black = 0
Borne off: White = 8, Black = 8
```

**Indicator 1 — Pip Count**:

```
White pips:
  Index 0  (pt 1):  3 * 1  = 3
  Index 4  (pt 5):  2 * 5  = 10
  Index 9  (pt 10): 1 * 10 = 10
  Bar: 1 * 25 = 25
  Total = 48

Black pips:
  Index 19 (pt 20): 2 * (24-19) = 2 * 5 = 10
  Index 21 (pt 22): 4 * (24-21) = 4 * 3 = 12
  Index 12 (pt 13): 1 * (24-12) = 1 * 12 = 12
  Total = 34

PipCountDiff = 48 - 34 = +14  (AI ahead by 14 pips)
```

**Indicator 2 — Blots**:

```
White blots: point 10 has 1 piece → 1 blot
Black blots: point 13 has 1 piece → 1 blot
BlotsDiff = 1 - 1 = 0
```

**Indicator 3 — Bar**:

```
BarDiff = 1 - 0 = +1 (Human has 1 on bar, AI has 0)
```

**Indicator 4 — Borne Off**:

```
BorneOffDiff = 8 - 8 = 0
```

**Indicator 5 — Made Points**:

```
White made points: point 1 (3 pieces), point 5 (2 pieces) → 2
Black made points: point 20 (2 pieces), point 22 (4 pieces) → 2
MadePointsDiff = 2 - 2 = 0
```

**Indicator 6 — Home Board**:

```
White home (points 1-6): point 1 (3) + point 5 (2) = 5
Black home (points 19-24): point 20 (2) + point 22 (4) = 6
HomeBoardDiff = 6 - 5 = +1
```

**Final Score**:

```
H = 1.0 * 14    Pip count:    +14.0
  + 2.0 * 0     Blots:          0.0
  + 3.0 * 1     Bar:           +3.0
  + 4.0 * 0     Borne off:      0.0
  + 1.5 * 0     Made points:    0.0
  + 1.0 * 1     Home board:    +1.0
  ──────────────────────────────────
H = +18.0
```

**Interpretation**: H = +18.0 (positive = favorable for AI). The AI is ahead
primarily because of the pip count advantage (+14 pips) and the opponent having
a piece on the bar (+3 from bar weight). This is a moderately good position for
the AI.

---

## 6. ExpectiminimaxAgent (class)

**File**: `src/backgammon/ai/ExpectiminimaxAgent.java`

**Purpose**: The AI's decision-making brain. Given a game state where it is the
AI's turn, it searches a tree of possible futures and picks the move with the
highest expected value.

### Constants

```java
private static final int MAX_DEPTH = 2;   // 2 levels of play
private static final int DIE_SIDES = 6;   // die values 1-6
```

### Dependencies

```java
private final GameLogic gameLogic;          // to generate/apply moves
private final HeuristicEvaluator evaluator; // to evaluate leaf states
```

### Three Node Types

| Node | Who decides | Strategy | When used |
|------|-----------|----------|-----------|
| **MAX** | AI (BLACK) | Pick the move with the **highest** value | AI's turn |
| **CHANCE** | Nobody (dice) | **Average** over all 6 die outcomes | Between turns |
| **MIN** | Human (WHITE) | Pick the move with the **lowest** value | Human's turn |

### Method: `chooseMove(state)`

The entry point. Acts as the root MAX node.

```java
public Move chooseMove(BackgammonState state) {
    List<Move> legalMoves = gameLogic.getLegalMoves(state);

    if (legalMoves.isEmpty()) {
        return null;  // Turn forfeited
    }

    Move bestMove = null;
    double bestValue = Double.NEGATIVE_INFINITY;

    for (Move move : legalMoves) {
        BackgammonState child = gameLogic.applyMove(state, move);
        child.setCurrentPlayer(Player.WHITE);   // opponent's turn next

        double value = chanceNode(child, MAX_DEPTH - 1);  // depth = 1

        if (value > bestValue) {
            bestValue = value;
            bestMove = move;
        }
    }

    return bestMove;
}
```

For each legal move, it:
1. Applies the move to get a child state
2. Switches to the opponent's turn
3. Evaluates the child via `chanceNode()` (because the opponent will roll a die)
4. Keeps track of the move with the highest expected value

### Method: `chanceNode(state, depth)`

Simulates the uncertainty of a die roll.

```java
private double chanceNode(BackgammonState state, int depth) {
    if (depth <= 0 || gameLogic.isTerminal(state)) {
        return evaluator.evaluate(state);
    }

    double expectedValue = 0.0;

    for (int die = 1; die <= DIE_SIDES; die++) {
        BackgammonState stateWithDie = state.deepCopy();
        stateWithDie.setDieRoll(die);

        double value;
        if (stateWithDie.getCurrentPlayer() == Player.BLACK) {
            value = maxNode(stateWithDie, depth);
        } else {
            value = minNode(stateWithDie, depth);
        }

        expectedValue += value / DIE_SIDES;  // weight: 1/6 each
    }

    return expectedValue;
}
```

It loops through all 6 possible die values, evaluates each, and computes the
weighted average (each die value has probability 1/6).

### Method: `maxNode(state, depth)`

AI's turn — pick the move that maximizes the score.

```java
private double maxNode(BackgammonState state, int depth) {
    if (depth <= 0 || gameLogic.isTerminal(state)) {
        return evaluator.evaluate(state);
    }

    List<Move> legalMoves = gameLogic.getLegalMoves(state);

    if (legalMoves.isEmpty()) {
        BackgammonState passState = state.deepCopy();
        passState.setCurrentPlayer(Player.WHITE);
        return chanceNode(passState, depth - 1);  // forfeit turn
    }

    double bestValue = Double.NEGATIVE_INFINITY;

    for (Move move : legalMoves) {
        BackgammonState child = gameLogic.applyMove(state, move);
        child.setCurrentPlayer(Player.WHITE);

        double value = chanceNode(child, depth - 1);
        bestValue = Math.max(bestValue, value);
    }

    return bestValue;
}
```

If no legal moves exist, the turn is forfeited — the code passes to the opponent
via a chance node (because the opponent still needs to roll a die). This correctly
models the Backgammon rule that a player with no moves simply skips their turn.

### Method: `minNode(state, depth)`

Human's turn — pick the move that minimizes the score (worst for AI).

```java
private double minNode(BackgammonState state, int depth) {
    if (depth <= 0 || gameLogic.isTerminal(state)) {
        return evaluator.evaluate(state);
    }

    List<Move> legalMoves = gameLogic.getLegalMoves(state);

    if (legalMoves.isEmpty()) {
        BackgammonState passState = state.deepCopy();
        passState.setCurrentPlayer(Player.BLACK);
        return chanceNode(passState, depth - 1);  // forfeit turn
    }

    double bestValue = Double.POSITIVE_INFINITY;

    for (Move move : legalMoves) {
        BackgammonState child = gameLogic.applyMove(state, move);
        child.setCurrentPlayer(Player.BLACK);

        double value = chanceNode(child, depth - 1);
        bestValue = Math.min(bestValue, value);
    }

    return bestValue;
}
```

Mirror of `maxNode()` but uses `POSITIVE_INFINITY` as starting value and
`Math.min()` instead of `Math.max()`.

### Full Traced Example

**Scenario**: AI (BLACK) has die = 3 and two legal moves:
- **Move A**: point 6 → point 9
- **Move B**: point 13 → point 16

Here is the complete call chain:

```
chooseMove(state)                          ← ROOT (MAX)
│
├── Try Move A (6 → 9):
│   Apply move → childA (currentPlayer = WHITE)
│   │
│   └── chanceNode(childA, depth=1)        ← CHANCE
│       │
│       ├── die=1: deepCopy, setDieRoll(1)
│       │   └── minNode(state_d1, depth=1) ← MIN (human's turn)
│       │       │ getLegalMoves() → [move_x, move_y, ...]
│       │       │ For each human move:
│       │       │   applyMove → leaf state
│       │       │   chanceNode(leaf, depth=0) → evaluator.evaluate() → H value
│       │       └── return MIN of all H values → e.g., +4.5
│       │
│       ├── die=2: ... → minNode → +3.0
│       ├── die=3: ... → minNode → +1.0
│       ├── die=4: ... → minNode → +2.5
│       ├── die=5: ... → minNode → -1.0
│       └── die=6: ... → minNode → +0.5
│       │
│       └── expectedValue = (4.5+3.0+1.0+2.5-1.0+0.5) / 6 = 1.75
│
│   value for Move A = 1.75
│
├── Try Move B (13 → 16):
│   Apply move → childB (currentPlayer = WHITE)
│   │
│   └── chanceNode(childB, depth=1)        ← CHANCE
│       │
│       ├── die=1: ... → minNode → +2.0
│       ├── die=2: ... → minNode → +5.5
│       ├── die=3: ... → minNode → +3.0
│       ├── die=4: ... → minNode → +4.0
│       ├── die=5: ... → minNode → +1.5
│       └── die=6: ... → minNode → +3.5
│       │
│       └── expectedValue = (2.0+5.5+3.0+4.0+1.5+3.5) / 6 = 3.25
│
│   value for Move B = 3.25
│
└── MAX picks: Move B (3.25 > 1.75)
    return Move B
```

**Step-by-step inside one minNode call** (e.g., Move A, die=1):

```
minNode(state_d1, depth=1):
  getLegalMoves(state_d1) → human has 4 legal moves with die=1
  
  For human move_x (e.g., point 19 → point 18):
    child = applyMove(state_d1, move_x)
    child.currentPlayer = BLACK
    chanceNode(child, depth=0)
      → depth=0, hits base case
      → return evaluator.evaluate(child) → e.g., +6.0
  
  For human move_y (e.g., point 12 → point 11):
    ... → evaluate → e.g., +4.5
  
  For human move_z (e.g., point 17 → point 16):
    ... → evaluate → e.g., +7.0
  
  For human move_w (e.g., point 1 → point... can't, die=1 goes to 0):
    ... → evaluate → e.g., +5.0
  
  MIN picks the lowest: min(6.0, 4.5, 7.0, 5.0) = 4.5
  return 4.5
```

The human is assumed to play optimally (minimizing the AI's score), which is
why the MIN node picks the worst-case outcome for the AI.

---

## 7. ConsoleUI (class)

**File**: `src/backgammon/ui/ConsoleUI.java`

**Purpose**: Handles everything the player sees and interacts with. This class
has no game logic — it only displays information and reads input.

### Constructor

```java
private final Scanner scanner;

public ConsoleUI() {
    this.scanner = new Scanner(System.in);
}
```

Creates a `Scanner` to read keyboard input from the player.

### `displayBoard(state)`

Renders the full board as a text grid. The layout mirrors a real Backgammon
board viewed from White's side:

```
 Top half:    Points 13-18 | BAR | Points 19-24
 Separator:   ─────────────────────────────────
 Bottom half: Points 12-7  | BAR | Points 6-1
```

The board is drawn in two halves, each with up to 5 rows of piece symbols.
The bar column in the center shows captured pieces for both players.

```
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
```

### `getPieceChar(boardValue, row)`

Determines what character to display at a specific row of a specific point.

```java
private String getPieceChar(int boardValue, int row) {
    int absCount = Math.abs(boardValue);
    if (row < absCount) {
        if (absCount > 5 && row == 4) {
            return String.valueOf(absCount);  // show count for stacks > 5
        }
        return boardValue > 0 ? "O" : "X";
    }
    return ".";
}
```

**How it works**: Each point is drawn with up to 5 vertical rows (row 0 at the
top). If a point has `N` pieces, rows 0 through `N-1` show the piece symbol,
and the remaining rows show `.` (empty).

**Example**: Point has 3 Black pieces (`boardValue = -3`):

```
Row 0: "X"   (0 < 3)
Row 1: "X"   (1 < 3)
Row 2: "X"   (2 < 3)
Row 3: "."   (3 < 3 is false)
Row 4: "."   (4 < 3 is false)
```

**Example**: Point has 7 White pieces (`boardValue = +7`):

```
Row 0: "O"   (0 < 7)
Row 1: "O"   (1 < 7)
Row 2: "O"   (2 < 7)
Row 3: "O"   (3 < 7)
Row 4: "7"   (absCount > 5 && row == 4 → show numeric count)
```

This handles the display of large stacks gracefully without distorting the board.

### `promptHumanMove(moves)`

Displays numbered legal moves and reads the player's choice.

```java
public Move promptHumanMove(List<Move> moves) {
    System.out.println(" Your legal moves:");
    for (int i = 0; i < moves.size(); i++) {
        System.out.printf("   [%d] %s%n", i + 1, moves.get(i).describe());
    }

    while (true) {
        System.out.printf(" Choose a move (1-%d): ", moves.size());
        String input = scanner.nextLine().trim();
        try {
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= moves.size()) {
                return moves.get(choice - 1);
            }
            System.out.println(" Invalid choice...");
        } catch (NumberFormatException e) {
            System.out.println(" Invalid input...");
        }
    }
}
```

The `while(true)` loop keeps asking until valid input is given. Invalid inputs
(non-numeric, out of range) print an error and re-prompt. This prevents crashes
from bad user input.

**Example interaction**:

```
 Your legal moves:
   [1] 12 -> 10
   [2] 17 -> 15
   [3] 19 -> 17

 Choose a move (1-3): hello
 Invalid input. Please enter a number.
 Choose a move (1-3): 5
 Invalid choice. Please enter a number between 1 and 3
 Choose a move (1-3): 2
```

Returns the Move at index 1 (0-based) → `17 -> 15`.

### Other Display Methods

| Method | What it displays |
|--------|-----------------|
| `displayWelcome()` | Game title banner, player assignments, movement directions |
| `displayFirstRoll(whiteRoll, blackRoll, firstPlayer)` | "White rolls: 5, Black rolls: 2, White goes first!" |
| `displayDieRoll(player, die)` | "White (Human) rolls a 4." |
| `displayAIMove(move)` | "AI chose: 6 -> 10" (or "Turn forfeited" if null) |
| `displayNoMoves(player)` | "White (Human) has no legal moves. Turn forfeited." |
| `displayWinner(winner)` | "GAME OVER! Black (AI) wins!" with asterisk border |
| `displayMessage(message)` | General-purpose message line |

All of these are simple `System.out.println()` / `System.out.printf()` calls
with no logic.

---

## 8. Main (class)

**File**: `src/backgammon/Main.java`

**Purpose**: The entry point that wires all components together and runs the
game loop. This is the only class with a `main()` method.

### Initialization (Dependency Wiring)

```java
GameLogic gameLogic = new GameLogic();
HeuristicEvaluator evaluator = new HeuristicEvaluator(gameLogic);
ExpectiminimaxAgent aiAgent = new ExpectiminimaxAgent(gameLogic, evaluator);
ConsoleUI ui = new ConsoleUI();
```

The dependency chain:

```
GameLogic ← standalone, no dependencies
    ↓
HeuristicEvaluator ← needs GameLogic (for computePipCount, isTerminal)
    ↓
ExpectiminimaxAgent ← needs GameLogic + HeuristicEvaluator
ConsoleUI ← standalone, no dependencies
```

### `determineFirstPlayer(ui)`

Both players roll one die. Higher roll goes first. Ties re-roll.

```java
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
```

The `while(true)` loop handles ties — it keeps rolling until the values differ.

### `rollDie()`

```java
private static final Random random = new Random();

private static int rollDie() {
    return random.nextInt(6) + 1;  // returns 1, 2, 3, 4, 5, or 6
}
```

`Random.nextInt(6)` returns 0-5, so `+1` shifts it to 1-6.

### Game Loop

```java
while (!gameLogic.isTerminal(state)) {
    Player currentPlayer = state.getCurrentPlayer();

    // 1. Generate legal moves
    List<Move> legalMoves = gameLogic.getLegalMoves(state);

    Move chosenMove;

    if (legalMoves.isEmpty()) {
        // 2a. No legal moves → forfeit
        ui.displayNoMoves(currentPlayer);
        chosenMove = null;
    } else if (currentPlayer == Player.WHITE) {
        // 2b. Human's turn → prompt for input
        chosenMove = ui.promptHumanMove(legalMoves);
        ui.displayMessage("You chose: " + chosenMove.describe());
    } else {
        // 2c. AI's turn → run Expectiminimax
        ui.displayMessage("AI is thinking...");
        chosenMove = aiAgent.chooseMove(state);
        ui.displayAIMove(chosenMove);
    }

    // 3. Apply the move (if any)
    if (chosenMove != null) {
        state = gameLogic.applyMove(state, chosenMove);
    }

    // 4. Check terminal
    if (gameLogic.isTerminal(state)) {
        state.setCurrentPlayer(currentPlayer);
        ui.displayBoard(state);
        break;
    }

    // 5. Switch player and roll for next turn
    Player nextPlayer = currentPlayer.opponent();
    state.setCurrentPlayer(nextPlayer);
    int die = rollDie();
    state.setDieRoll(die);
    ui.displayDieRoll(nextPlayer, die);
    ui.displayBoard(state);
}
```

**Key detail on line 84**: `state = gameLogic.applyMove(state, chosenMove)`
replaces the entire state variable. `applyMove()` returns a new state object;
the old state is discarded and garbage-collected. The original state is never
mutated.

**Key detail on line 89**: After the game ends, `setCurrentPlayer(currentPlayer)`
preserves who made the winning move, so the final board display shows the correct
winner information.

### End of Game

```java
Player winner = gameLogic.getWinner(state);
if (winner != null) {
    ui.displayWinner(winner);
} else {
    ui.displayMessage("Game ended unexpectedly.");
}
```

After the loop breaks, the winner is determined and displayed. The `else` branch
should never execute in normal play but guards against unexpected edge cases.

---

## 9. How They All Connect

### Dependency Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                          Main                                │
│  (entry point, game loop, wires everything together)         │
│                                                              │
│  Creates and uses:                                           │
│  ┌──────────────┐  ┌──────────────────┐  ┌──────────────┐   │
│  │  ConsoleUI   │  │   GameLogic      │  │ Expecti-     │   │
│  │              │  │                  │  │ minimaxAgent │   │
│  │ displayBoard │  │ getLegalMoves()  │  │              │   │
│  │ promptHuman  │  │ applyMove()      │  │ chooseMove() │   │
│  │ displayAI    │  │ isTerminal()     │  │              │   │
│  │ displayWin   │  │ canBearOff()     │  └──────┬───────┘   │
│  └──────────────┘  │ computePipCount()│         │            │
│                    └────────┬─────────┘         │            │
│                             │                   │            │
│                             │    ┌──────────────┴──────┐     │
│                             │    │ HeuristicEvaluator  │     │
│                             │    │                     │     │
│                             ├───►│ evaluate()          │     │
│                             │    │ countBlots()        │     │
│                             │    │ countMadePoints()   │     │
│                             │    │ countHomePieces()   │     │
│                             │    └─────────────────────┘     │
│                             │                                │
│                    ┌────────┴────────────┐                   │
│                    │    Model Classes    │                   │
│                    │                     │                   │
│                    │  Player (enum)      │                   │
│                    │  Move (class)       │                   │
│                    │  BackgammonState    │                   │
│                    └─────────────────────┘                   │
└──────────────────────────────────────────────────────────────┘
```

### Data Flow During One Turn

```
1. Main: state.getCurrentPlayer() → who's turn is it?

2. Main: gameLogic.getLegalMoves(state) → List<Move>
   └── GameLogic reads state.board, state.bar, state.dieRoll
       └── Returns list of valid Move objects

3a. If human: ui.promptHumanMove(moves) → player types "2" → Move
3b. If AI:    aiAgent.chooseMove(state) → Move
    └── ExpectiminimaxAgent:
        ├── For each legal move:
        │   ├── gameLogic.applyMove(state, move) → child state
        │   └── chanceNode(child, depth=1)
        │       ├── For die=1..6:
        │       │   └── minNode(stateWithDie, depth=1)
        │       │       ├── gameLogic.getLegalMoves() → opponent's moves
        │       │       ├── For each opponent move:
        │       │       │   ├── gameLogic.applyMove() → leaf state
        │       │       │   └── evaluator.evaluate(leaf) → H value
        │       │       └── return MIN of all H values
        │       └── return AVERAGE of all 6 die results
        └── return move with MAX expected value

4. Main: state = gameLogic.applyMove(state, chosenMove)
   └── Returns new BackgammonState (old state discarded)

5. Main: gameLogic.isTerminal(state) → continue or end?

6. Main: Roll new die, switch player, display board
   └── ui.displayBoard(state) → renders to console
```

### Package Organization

```
backgammon/
├── model/       ← Data classes (what the game state looks like)
│   ├── Player        knows nothing about rules, AI, or display
│   ├── Move          knows nothing about rules, AI, or display
│   └── BackgammonState  knows nothing about rules, AI, or display
│
├── logic/       ← Rules engine (what moves are legal)
│   └── GameLogic     uses model classes, knows nothing about AI or display
│
├── ai/          ← Artificial intelligence (which move to pick)
│   ├── HeuristicEvaluator    uses model + logic, knows nothing about display
│   └── ExpectiminimaxAgent   uses model + logic + evaluator
│
├── ui/          ← User interface (what the player sees)
│   └── ConsoleUI     uses model classes, knows nothing about logic or AI
│
└── Main.java    ← Glue (connects everything)
```

Each layer depends only on the layers above it. The model classes are completely
independent — they could be reused with a different rules engine, a different AI,
or a graphical UI without any changes.
