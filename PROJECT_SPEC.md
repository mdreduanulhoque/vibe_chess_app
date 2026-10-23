You are building "Vibe Chess", a native Android app in Java (Android SDK, Gradle),
released later on Google Play. Read this file at the start of every task and follow it.

## Product
A minimalist, timed chess variant with NO check and NO checkmate.

### Rules
- Board, setup and piece movement: identical to standard chess.
- No check, no checkmate, no stalemate. A king may be captured like any piece.
  The game does NOT end when a king is captured.
- A player may move the king into an attacked square or leave it attacked.
- Castling: allowed if king and rook have not moved and squares between are empty.
  Attacked squares do NOT block castling (there is no check). Lost if the king is captured.
- En passant: allowed, only immediately after the opponent's double pawn step.
- Pawn promotion: player picks Queen, Rook, Bishop or Knight (AI picks best).
- Points: Pawn 1, Knight 2, Bishop 3, Rook 4, Queen 5, King 7.
  A side's points = sum of its pieces currently on the board.
- Clock: chess clock. Each side gets 1 or 2 minutes total (chosen in setup, no increment).
  Only the side to move has a running clock. White moves first.
- When ANY side's time reaches 0, the game ends immediately. The side with more
  points on the board wins. Equal points = draw.
- If a side has no legal move, its turn is skipped and its clock keeps running.
- No threefold repetition or 50-move rule (the clock ends every game).

### Modes
1. Single Player vs Computer: 3 difficulties (Easy/Medium/Hard), player chooses color
   (White / Black / Random).
2. Multiplayer (offline): two players on one device, pass-and-play.
   Setting: "Rotate opponent's pieces 180°" (default on).

### Live UI rule
Do NOT show raw point scores during play. Show a WINNING PROBABILITY bar instead
(White % vs Black %). Show a captured-pieces tray (icons only, no numbers).
Final points may be shown ONLY on the result screen.

## Tech
- Language: Java 17. minSdk 26. targetSdk/compileSdk = latest stable.
- UI: XML layouts + Material Components + a custom Canvas-drawn BoardView.
  (No Jetpack Compose; it needs Kotlin.)
- Architecture: MVVM (ViewModel + LiveData), single-activity with Navigation
  component OR a small set of activities. Keep it simple.
- Gradle multi-module:
  - :game-core  -> pure Java library. ZERO Android imports. Contains model, rules,
    move generation, clock logic, scoring, win-probability, AI.
    Must be reusable later by a Spring Boot server.
  - :app        -> Android UI. Depends on :game-core.
- Abstraction for future online play: define a `MoveSource` interface
  (LocalHumanMoveSource, AiMoveSource now; RemoteMoveSource later) so the game
  controller never cares where a move comes from.
- Package: com.reduan.vibechess
- Offline only. NO INTERNET permission. No ads, no analytics, no accounts.
- Orientation: portrait AND landscape (and tablets). Game state must survive rotation.

## Design
- Minimalist, flat, generous spacing, rounded corners, no gradients/shadows overload.
- Brand feel: UIU-inspired, orange and white.
- Theme tokens (define once in colors.xml / themes, never hardcode in layouts):
  Light (default):  background #FFFFFF, surface #FFF7F0, primary #F26A1B (orange),
                    onPrimary #FFFFFF, text #1F1F1F, board-light #FFF1E3, board-dark #F2A66B,
                    highlight-move #F26A1B @ 40% alpha, last-move #F26A1B @ 25% alpha.
  Dark (toggle):    background #121212, surface #1E1E1E, primary #FF8A3D,
                    onPrimary #1A1A1A, text #F2F2F2, board-light #3A3A3F, board-dark #26262B,
                    same highlight logic.
- Pieces: original minimalist vector drawables (VectorDrawable XML), 6 types x 2 colors.
  White pieces = white fill + dark outline. Black pieces = charcoal (#2B2B2B) fill.
- Fonts: bundle a Bangla-capable font (e.g. Noto Sans Bengali) so Bangla renders
  consistently on all devices.

## Languages
- English and Bangla (বাংলা). Strings only in res/values/strings.xml and
  res/values-bn/strings.xml. No hardcoded text anywhere.
- Default = device language (fallback English). Switchable in Settings at runtime
  using AppCompatDelegate.setApplicationLocales.

## Screens
Splash -> Home (Single Player, Multiplayer, How to Play, Settings)
-> Game Setup (time 1/2 min, color, difficulty) -> Game -> Result.
Settings: Language, Dark mode toggle, Sound, Vibration, Rotate opponent pieces.
Settings persist with SharedPreferences (or DataStore).

## Working rules for the agent
- Work ONLY on the current step. Do not build future steps early.
- Write clean, commented, small classes. No giant files.
- After each step: build the project, run tests, then STOP and report:
  what was done, how to verify, any assumptions or open issues.
- Never silently change these rules. If something is ambiguous, ask.