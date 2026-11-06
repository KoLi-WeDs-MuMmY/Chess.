# Grandmaster Slate — AI Chess & Match Analysis Suite

An offline-ready Android chess application that lets players challenge a four-tier AI engine, track their current and best win streaks across difficulty levels, and deeply review past games with an interactive evaluation bar, move accuracy breakdown, and blunder detection.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product and design choices have been confirmed from your preferences and incorporated into the architecture below:

- **Confirmed Decision 1 — Post-Game Analysis Suite**: Past games open into an interactive review board featuring a live centipawn **Engine Evaluation Bar**, per-move classification (**Best, Good, Inaccuracy, Mistake, Blunder**), overall **Move Accuracy (%)**, and visual best-move arrows on the board when stepping through critical turns.
- **Confirmed Decision 2 — Time Controls**: Pre-game setup supports **Untimed** (casual play), **Blitz** (3+2 and 5+0), and **Rapid** (10+0 and 15+10) presets, with automatic pause/resume lifecycle handling and timeout adjudication.
- **Confirmed Decision 3 — Visual Direction**: **Sleek Dark Tournament Slate** aesthetic combining deep obsidian/slate surfaces, tournament emerald board squares, warm champagne-gold accents for streaks and evaluation highlights, and crisp vector chess pieces.
- **Confirmed Decision 4 — Offline-First Persistence**: All completed matches, move histories (SAN + UCI + per-ply centipawn evaluations), and win streak statistics are stored locally on-device using **Room Database** so history and analysis work 100% offline.

---

## 1. Overview & Core Concept

- **What It Does**: Provides a complete FIDE-compliant chess experience against an on-device AI engine scaled across four distinct difficulty levels (**Easy**, **Medium**, **Hard**, and **Impossible**). Every finished game is automatically archived offline with full move-by-move evaluations so players can replay games, inspect their accuracy, jump directly to blunders, and monitor their win streaks over time.
- **Target Audience / Persona**: Chess enthusiasts and improving players who want a responsive, zero-latency offline sparring partner that not only plays at tailored strength levels but also pinpoints *where* and *why* a game swung in the opponent's favor.
- **Key Value**: Bridges the gap between playing and learning—combining immediate tactile gameplay, motivating win-streak tracking, and concrete post-game blunder analysis in a cohesive tournament-grade interface.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Dashboard & New Game Setup (Play Tab)**:
   - Displays a prominent **Win Streak Banner** showing the player's **Current Win Streak** (with flame/gold badge), **Best All-Time Streak**, total win rate, and per-difficulty record pills.
   - **Quick Match Configurator**:
     - **AI Difficulty Selector**: 4 interactive cards with estimated ELO badges and behavioral descriptions:
       - *Easy (~800 ELO)*: Shallow search with intentional positional noise; forgives tactical oversights.
       - *Medium (~1300 ELO)*: Tactical awareness with 2-ply lookahead and basic piece-square positional play.
       - *Hard (~1800 ELO)*: Alpha-beta pruning with move ordering, quiescence search on captures, and endgame king activity.
       - *Impossible (~2400+ Master Engine)*: Deep iterative alpha-beta search with transposition table caching, killer move heuristics, passed-pawn evaluation, and aggressive tactical punishment.
     - **Time Control Selector**: Segmented chips for **Untimed**, **Blitz (3m+2s / 5m)**, and **Rapid (10m / 15m+10s)**.
     - **Side Selection**: Play as **White**, **Black**, or **Random**.
2. **Active Match Experience**:
   - Full-width custom Canvas chessboard with rank/file coordinates, legal move target indicators (dots for quiet moves, corner rings for captures), last-move highlight tint, and red radial king-in-check alert.
   - **Top Opponent Bar & Bottom Player Bar**: Displays AI tier badge, captured pieces with material advantage delta (`+2`), and high-precision digital chess clocks (when timed).
   - **Live Move Tape & Controls**: Horizontal auto-scrolling algebraic move strip (`1. e4 e5  2. Nf3 Nc6`), plus action controls for **Hint** (available in Easy/Medium), **Flip Board**, and **Resign / Offer Draw**.
   - **Pawn Promotion Sheet**: Clean overlay modal allowing instant selection of Queen, Rook, Bishop, or Knight when a pawn reaches the 8th/1st rank.
   - **Game Over Summary Dialog**: Announces Checkmate, Stalemate, Time Out, or Resignation, shows how the result affects the player's **Win Streak**, displays a quick accuracy preview, and offers a direct **"Analyze Game"** CTA.
3. **Offline Game History & Streak Stats (History Tab)**:
   - Filterable list of all past games stored in Room (filter by *All*, *Wins*, *Losses*, *Draws*, or by *AI Level*).
   - Each match card shows the opponent AI level, player color, game outcome badge, accuracy percentage pill, move count, time control, and date.
   - Tapping any match opens the **Post-Game Analysis Screen**.
4. **Interactive Post-Game Analysis Screen**:
   - **Vertical Evaluation Bar**: Positioned alongside the chessboard, dynamically animating White vs. Black centipawn advantage (`+1.4`, `-3.2`, or `M3` for forced mate) at the selected ply.
   - **Accuracy & Move Classification Summary Card**: Compares Player vs. AI accuracy (`86.4% vs 91.2%`) and tallies **Best Moves**, **Good Moves**, **Inaccuracies (?!)**, **Mistakes (?)**, and **Blunders (??)**.
   - **Key Swing Moments Strip**: Quick-jump chips for every Mistake or Blunder in the match—tapping a chip jumps the board directly to that position and draws a **Gold Best-Move Arrow** comparing what the engine recommended against the move played.
   - **Scrubber & Replay Controls**: First move, Previous ply, Play/Pause auto-step, Next ply, and Last move buttons, synced with a tappable algebraic move table.

### Visual Identity & Theme

- **Aesthetic Direction**: *Sleek Dark Tournament Slate* — inspired by high-stakes broadcast chess studios, combining distraction-free dark slate chrome with crisp emerald board geometry and luminous gold telemetry.
- **Color Palette & Mood**:
  - **Background & Surfaces**: Deep Obsidian Slate (`#0B0F14` primary background), Elevated Slate Card (`#131A22`), and Interactive Surface (`#1C2632`).
  - **Primary Accent**: Tournament Emerald (`#10B981` primary / `#059669` board dark squares / `#D1FAE5` light squares tinted to warm alabaster-jade `#DDE6E1` for glare-free contrast).
  - **Secondary Accent**: Grandmaster Gold (`#F59E0B` / `#FBBF24`) for active win streaks, Impossible AI badges, and best-move arrows.
  - **Semantic Evaluation Colors**: Brilliant/Best (`#10B981` Emerald), Good (`#38BDF8` Sky), Inaccuracy (`#FBBF24` Amber), Mistake (`#F97316` Orange), Blunder (`#EF4444` Crimson).
- **Typography & Hierarchy**:
  - **Display & Headings**: `Cinzel` or `Space Grotesk` (bundled locally via `font-util`) for authoritative tournament headers, streak counters, and ratings.
  - **Body & UI Labels**: `Plus Jakarta Sans` for crisp legibility across game cards and analysis breakdowns.
  - **Clocks, Algebraic Notation & Eval Scores**: `JetBrains Mono` for tabular numerals on chess timers (`04:58.2`) and algebraic move lists (`14. Nxd5 Bxd5`).
- **Interactive Feedback & Motion**:
  - Smooth spring-animated piece transitions (`animateOffsetAsState` / Canvas interpolation) and evaluation bar height morphing.
  - Tactile ripple states on all board controls and move list pills, with minimum `48.dp` touch targets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1 — Custom On-Device Minimax/Alpha-Beta Engine with Post-Game Centipawn Evaluator**:
  - *Chosen Approach*: Build a dedicated, pure-Kotlin FIDE chess rules and search engine running on background coroutines (`Dispatchers.Default`), featuring bitboard/mailbox move generation, Zobrist-style position hashing for threefold repetition and caching, piece-square positional tables, quiescence search, and configurable search depth/noise per difficulty tier.
  - *Why*: Guarantees 100% offline reliability, zero external native binary compatibility issues across emulator architectures, instant position evaluation for the post-game analysis bar, and deterministic difficulty scaling from Easy to Impossible.
  - *Alternatives Considered*: Cloud-based LLM move generation (rejected—high latency, requires internet, and frequently hallucinates illegal moves in complex middlegames).
- **Decision 2 — Room SQLite Persistence for Match History & Streak Telemetry**:
  - *Chosen Approach*: Persist every completed match in a local Room database (`ChessDatabase`) with full ply-by-ply move records (FEN snapshots, SAN notation, played move, engine best move, and centipawn score before/after).
  - *Why*: Directly fulfills the offline game history requirement and allows instant loading of any historical game into the analysis board without re-computing the entire game tree from scratch.
  - *Alternatives Considered*: DataStore-only JSON blobs (rejected—does not scale cleanly for querying, filtering by difficulty/result, and aggregating win streaks).

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌───────────────────────────────────────────────────────────────────────────┐
│                         UI Layer (Jetpack Compose)                        │
│                                                                           │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌───────────────────┐  │
│  │  Home & Streak Hub  │  │  Active Match View  │  │ Post-Game Analysis│  │
│  │  - Streak Dashboard │  │  - Canvas Board     │  │  - Eval Bar (CP)  │  │
│  │  - AI Tier Picker   │  │  - Chess Clocks     │  │  - Accuracy Card  │  │
│  │  - Time Controls    │  │  - Move Strip & Bar │  │  - Blunder Arrows │  │
│  └──────────┬──────────┘  └──────────┬──────────┘  └─────────┬─────────┘  │
└─────────────┼────────────────────────┼───────────────────────┼────────────┘
              │                        │                       │
              ▼                        ▼                       ▼
┌───────────────────────────────────────────────────────────────────────────┐
│                     State & Presentation (ViewModels)                     │
│                                                                           │
│  ┌───────────────────────────────────┐  ┌──────────────────────────────┐  │
│  │         ChessGameViewModel        │  │    HistoryAnalysisViewModel  │  │
│  │ - Match StateFlow (Board, Clocks) │  │ - Saved Games Flow + Filters │  │
│  │ - AI Turn Coroutine Dispatcher    │  │ - Active Replay Ply Scrubber │  │
│  │ - Auto-Save & Streak Calculation  │  │ - Blunder / Best-Move Lookup │  │
│  └──────────┬───────────────────┬────┘  └───────────────┬──────────────┘  │
└─────────────┼───────────────────┼───────────────────────┼─────────────────┘
              │                   │                       │
              ▼                   ▼                       ▼
┌───────────────────────────┐  ┌────────────────────────────────────────────┐
│   Domain & AI Engine      │  │       Data Layer (Room Offline Store)      │
│                           │  │                                            │
│  ┌─────────────────────┐  │  │  ┌──────────────────────────────────────┐  │
│  │ FIDE Rules & Board  │  │  │  │       ChessMatchRepository           │  │
│  │ - Legal Move Gen    │  │  │  │ - Observes Match History & Streaks   │  │
│  │ - Castling/EnPassant│  │  │  │ - Persists Game + Analyzed Plies     │  │
│  │ - Check/Mate/Draw   │  │  │  └──────────────────┬───────────────────┘  │
│  └──────────┬──────────┘  │  │                     │                      │
│             ▼             │  │                     ▼                      │
│  ┌─────────────────────┐  │  │  ┌──────────────────────────────────────┐  │
│  │ 4-Tier AI & Eval    │  │  │  │      Room Database (MatchDao)        │  │
│  │ - Easy/Med/Hard/Imp │  │  │  │ - MatchRecordEntity (Result, Streak, │  │
│  │ - Alpha-Beta + PST  │  │  │  │   Accuracy, Difficulty, TimeControl) │  │
│  │ - Ply Analyzer (CP) │  │  │  │ - Serialized MoveEvaluationList      │  │
│  └─────────────────────┘  │  │  └──────────────────────────────────────┘  │
└───────────────────────────┘  └────────────────────────────────────────────┘
```

### Data Model & State

- **`AiDifficulty` (Enum)**: `EASY` (Depth 1 + evaluation noise), `MEDIUM` (Depth 2 + standard PST), `HARD` (Depth 3 + Quiescence search + tactical ordering), `IMPOSSIBLE` (Depth 4–5 iterative alpha-beta + transposition table + killer heuristic + endgame king push).
- **`TimeControl` (Enum)**: `UNTIMED`, `BLITZ_3_2` (3 min + 2s increment), `BLITZ_5_0` (5 min), `RAPID_10_0` (10 min), `RAPID_15_10` (15 min + 10s increment).
- **`AnalyzedMove` (Model)**: Stores `plyNumber`, `san` (e.g., `"Nf3"`), `uciPlayed` (e.g., `"g1f3"`), `uciBestMove` (e.g., `"d2d4"`), `bestMoveSan`, `fenAfter`, `evalBeforeCp`, `evalAfterCp`, and `classification` (`BEST`, `GOOD`, `INACCURACY`, `MISTAKE`, `BLUNDER`).
- **`MatchRecordEntity` (Room `@Entity`)**:
  - `id: Long`, `timestamp: Long`, `aiDifficulty: String`, `timeControl: String`, `playerColor: String` (`WHITE` / `BLACK`), `outcome: String` (`WIN`, `LOSS`, `DRAW`), `terminationReason: String` (`CHECKMATE`, `TIMEOUT`, `RESIGNATION`, `STALEMATE`, `DRAW_RULE`), `playerAccuracy: Float`, `aiAccuracy: Float`, `blunderCount: Int`, `mistakeCount: Int`, `inaccuracyCount: Int`, `totalMoves: Int`, `durationSeconds: Int`, and `movesJson: String` (serialized list of `AnalyzedMove`).
- **`StreakStats` (Derived Domain Model)**:
  - Computed reactively from the ordered stream of `MatchRecordEntity` rows: `currentWinStreak`, `bestWinStreak`, `totalGames`, `wins`, `losses`, `draws`, `averageAccuracy`, and per-difficulty win streaks.

### Interactive Component & State Mapping

- **Board Interaction Handler**: Tapping a player's piece computes all strictly legal moves (filtering out moves that leave the king in check), highlights valid target squares, and supports either tap-to-move or tap-reselect. Special moves (kingside/queenside castling, en passant capture, and pawn promotion modal trigger) execute deterministically.
- **Real-Time Chess Clock Loop**: A coroutine ticker running at 100ms resolution decrements the active side's remaining milliseconds when a timed preset is chosen, adds the increment upon move completion, and automatically triggers a `TIMEOUT` result if either clock reaches `00:00.0`.
- **Completion & Analysis Pipeline**: When a match concludes (via checkmate, draw, timeout, or resignation), `ChessGameViewModel` runs a fast background pass over the game's move history to finalize move classifications (`BEST`, `GOOD`, `INACCURACY`, `MISTAKE`, `BLUNDER`) and accuracy percentages, saves the `MatchRecordEntity` to Room, updates the player's win streak, and presents the Game Summary modal with a one-tap transition into the **Post-Game Analysis Screen**.
