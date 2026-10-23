# Google Play Store Screenshot Shot List

Google Play requires high-resolution screenshots (`16:9` or `9:16`, PNG or JPEG, up to 8 MB each) across Phone, 7-inch Tablet, and 10-inch Tablet form factors. Capture the shots below in both **English (`en`)** and **Bangla (`bn`)**.

---

## 1. Phone — Portrait (`1080 x 1920` px or `1080 x 2400` px)

| Shot # | Screen / Scene | Theme | Language | Key Visual Elements to Highlight |
| :--- | :--- | :--- | :--- | :--- |
| **P-1** | **Home Screen** | Light (`#FFFFFF` / `#F26A1B`) | English & Bangla | Minimalist brand header with King badge, tagline, and 4 clean action buttons (*Single Player*, *Multiplayer*, *How to Play*, *Settings*). |
| **P-2** | **Game Screen (Mid-Game Action)** | Light | English & Bangla | Active board with a selected piece showing orange legal-move dots/capture rings (`40%` alpha), last-move highlight (`25%` alpha), active running clock (`0:42.8`), captured-pieces trays, and the live **Winning Probability Bar** (`White 62%` vs `Black 38%`). |
| **P-3** | **Game Screen (Dark Mode + Last 10s Tension)** | Dark (`#121212` / `#FF8A3D`) | English & Bangla | High-contrast dark board (`#3A3A3F` / `#26262B`), AI thinking or low-time clock under 10 seconds (`0:07.4`), and pawn promotion dialog open (Queen / Rook / Bishop / Knight). |
| **P-4** | **Game Setup Screen (Single Player)** | Light | English & Bangla | Color selection (*White / Black / Random*), AI Difficulty (*Easy / Medium / Hard*), and Time control (*1 min / 2 min*). |
| **P-5** | **How to Play Screen** | Light | Bangla & English | Illustrated rule cards showing "No Check & No Checkmate (King = 7 pts)" and point values (`1 / 2 / 3 / 4 / 5 / 7`). |
| **P-6** | **Result Screen** | Dark | English & Bangla | Winner announcement ("White Wins!"), reason ("Time ended, more points on board"), **Final Points Card** (`White: 31` vs `Black: 24`), and *Rematch* / *Home* buttons. |

---

## 2. Phone — Landscape (`1920 x 1080` px or `2400 x 1080` px)

| Shot # | Screen / Scene | Theme | Language | Key Visual Elements to Highlight |
| :--- | :--- | :--- | :--- | :--- |
| **L-1** | **Game Screen (Landscape)** | Light | English | Centered 1:1 square chess board, Pause/Exit controls and live Winning Probability Bar on the left panel, and Black/White clocks + captured-piece trays on the right panel. |
| **L-2** | **Result Screen (Landscape)** | Dark | Bangla | Two-column landscape layout: Winner headline & reason on the left, Final Points Card and Rematch/Home buttons on the right. |

---

## 3. Tablet — 7-inch & 10-inch (`sw600dp` Portrait `1200 x 1920` px & Landscape `1920 x 1200` px)

| Shot # | Form Factor & Orientation | Screen / Scene | Theme | Key Visual Elements to Highlight |
| :--- | :--- | :--- | :--- | :--- |
| **T-1** | **10" Tablet Landscape** (`1920 x 1200`) | **Game Screen (2-Player Pass-and-Play)** | Light | Spacious centered board with Black's pieces rotated 180°, left control & probability panel, and right dual-clock & captured-pieces panel. |
| **T-2** | **10" Tablet Portrait** (`1200 x 1920`) | **Game Screen (Single Player vs AI)** | Dark | Left-aligned square board (`60%` width) with clocks, captured trays, win-probability bar, and controls neatly stacked on the right side panel (`40%` width). |
| **T-3** | **7" or 10" Tablet Landscape** | **How to Play / Result Screen** | Light | Wide-format rule cards or two-column Result screen demonstrating clean tablet scaling (`values-sw600dp`). |

---

## Pre-Capture Checklist
- Enable Android System UI **Demo Mode** (`adb shell settings put global sysui_demo_allowed 1`) so the status bar shows a full battery, clean clock (`10:00`), and no notification icons.
- Verify no raw numeric point totals appear anywhere on the live Game screen shots (only the Winning Probability Bar and captured piece icons).
- Capture both English (`en`) and Bangla (`bn`) sets for the respective store listing locales.
