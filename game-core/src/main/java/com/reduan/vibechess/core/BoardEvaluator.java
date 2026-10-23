package com.reduan.vibechess.core;

import java.util.Objects;

/**
 * Static board evaluation and square-attack analysis for the Vibe Chess AI.
 * <p>
 * Evaluation rules:
 * <ul>
 *   <li>Material difference = {@code (ownPoints - opponentPoints)} using spec values
 *       (Pawn=1, Knight=2, Bishop=3, Rook=4, Queen=5, King=7), scaled by {@link #POINT_SCALE}.</li>
 *   <li>Very small positional bonus (center control and pawn advancement) for tie-breaking.</li>
 *   <li>In the last 10 seconds of the clock, favors safe captures and penalizes hanging
 *       high-value pieces since final points on the board decide the game.</li>
 * </ul>
 */
public final class BoardEvaluator {

    /**
     * Evaluation units per 1 board point. Positional bonuses are kept much smaller than 1 point
     * so material (board points) always dominates.
     */
    public static final int POINT_SCALE = 1000;

    /**
     * Threshold (10 seconds) below which the AI enters endgame clock-protection mode,
     * favoring safe captures and avoiding hanging high-value pieces.
     */
    public static final long LAST_TEN_SECONDS_MILLIS = 10_000L;

    private static final int[][] KNIGHT_OFFSETS = {
            {-2, -1}, {-2, 1},
            {-1, -2}, {-1, 2},
            {1, -2},  {1, 2},
            {2, -1},  {2, 1}
    };

    private static final int[][] DIAGONAL_DIRS = {
            {-1, -1}, {-1, 1}, {1, -1}, {1, 1}
    };

    private static final int[][] ORTHOGONAL_DIRS = {
            {-1, 0}, {1, 0}, {0, -1}, {0, 1}
    };

    private BoardEvaluator() {
    }

    /**
     * Returns true if either side has 10 seconds or less remaining on the clock.
     */
    public static boolean isLastTenSeconds(GameState state) {
        Objects.requireNonNull(state, "state must not be null");
        ChessClock clock = state.getClock();
        long minRemaining = Math.min(
                clock.getWhiteRemainingMillis(),
                clock.getBlackRemainingMillis()
        );
        return minRemaining <= LAST_TEN_SECONDS_MILLIS;
    }

    /**
     * Evaluates {@code state} from the perspective of {@code perspective} in standard mode.
     */
    public static int evaluate(GameState state, Color perspective) {
        return evaluate(state, perspective, isLastTenSeconds(state));
    }

    /**
     * Evaluates {@code state} from the perspective of {@code perspective}.
     *
     * @param state          the game state to evaluate
     * @param perspective    the side for which higher scores are better
     * @param lastTenSeconds true to apply last-10-seconds hanging-piece penalties
     * @return evaluation score in centi/milli-point units ({@link #POINT_SCALE} = 1 board point)
     */
    public static int evaluate(GameState state, Color perspective, boolean lastTenSeconds) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(perspective, "perspective must not be null");

        Board board = state.getBoard();
        Color opponent = perspective.opposite();

        int ownPoints = 0;
        int opponentPoints = 0;
        int ownPositional = 0;
        int opponentPositional = 0;

        for (int rank = 0; rank < Square.BOARD_SIZE; rank++) {
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null) {
                    continue;
                }
                int pts = piece.getPoints();
                int posBonus = positionalBonusForPiece(piece, rank, file);
                if (piece.color() == perspective) {
                    ownPoints += pts;
                    ownPositional += posBonus;
                } else {
                    opponentPoints += pts;
                    opponentPositional += posBonus;
                }
            }
        }

        int score = (ownPoints - opponentPoints) * POINT_SCALE
                + (ownPositional - opponentPositional);

        if (lastTenSeconds) {
            score -= computeHangingPenalty(board, perspective, state.getSideToMove());
        }

        return score;
    }

    /**
     * Computes a small positional tie-breaker bonus (center control + pawn advancement)
     * for a single piece at {@code (rank, file)}. Maximum bonus per piece is ~45 units
     * (under 0.05 of a single point) so it never overrides actual point differences.
     */
    static int positionalBonusForPiece(Piece piece, int rank, int file) {
        int bonus = centerControlBonus(rank, file);

        if (piece.type() == PieceType.PAWN) {
            int advancement = (piece.color() == Color.WHITE) ? (rank - 1) : (6 - rank);
            if (advancement > 0) {
                bonus += advancement * 6;
                if (advancement >= 4) {
                    // Extra nudge for pawns close to promotion
                    bonus += 8;
                }
            }
        } else if (piece.type() == PieceType.KNIGHT || piece.type() == PieceType.BISHOP) {
            // Slight nudge to develop minor pieces off the back rank
            int homeRank = (piece.color() == Color.WHITE) ? 0 : 7;
            if (rank != homeRank) {
                bonus += 4;
            }
        }

        return bonus;
    }

    /**
     * Small bonus for controlling or occupying central squares.
     */
    static int centerControlBonus(int rank, int file) {
        if (rank >= 3 && rank <= 4 && file >= 3 && file <= 4) {
            // Inner center: d4, e4, d5, e5
            return 12;
        }
        if (rank >= 2 && rank <= 5 && file >= 2 && file <= 5) {
            // Extended center: c3..f6
            return 6;
        }
        if (rank >= 1 && rank <= 6 && file >= 1 && file <= 6) {
            return 2;
        }
        return 0;
    }

    /**
     * Computes a root-level adjustment in the last 10 seconds to favor safe captures
     * and discourage moves that place high-value pieces onto attacked squares.
     */
    public static int evaluateMoveSafetyAdjustment(GameState beforeMove,
                                                   Move move,
                                                   GameState afterMove,
                                                   Color aiColor) {
        Board beforeBoard = beforeMove.getBoard();
        Piece movingPiece = beforeBoard.getPieceAt(move.from());
        if (movingPiece == null) {
            return 0;
        }

        Color opponent = aiColor.opposite();
        Board afterBoard = afterMove.getBoard();
        int destRank = move.to().rank();
        int destFile = move.to().file();

        int movingValue = movingPiece.getPoints();
        int minEnemyAttacker = getLeastValuableAttackerPoints(afterBoard, destRank, destFile, opponent);
        boolean destAttacked = minEnemyAttacker > 0;
        boolean destDefended = getLeastValuableAttackerPoints(afterBoard, destRank, destFile, aiColor) > 0;

        int capturedValue = 0;
        if (move.isCapture()) {
            if (move.isEnPassant()) {
                capturedValue = PieceType.PAWN.getPoints();
            } else {
                Piece victim = beforeBoard.getPieceAt(move.to());
                if (victim != null) {
                    capturedValue = victim.getPoints();
                }
            }
        }

        int adjustment = 0;
        if (move.isCapture()) {
            if (!destAttacked) {
                // Completely safe capture: reward banking guaranteed points before timeout
                adjustment += 260 + capturedValue * 80;
            } else if (capturedValue >= movingValue) {
                // Profitable or equal trade even if recaptured
                adjustment += 140 + (capturedValue - movingValue) * 80;
            } else if (!destDefended || minEnemyAttacker <= movingValue) {
                // Unsafe capture risking a higher-value piece for a smaller victim
                adjustment -= (movingValue - capturedValue) * 450;
            }
        } else {
            // Quiet move: avoid stepping a valuable piece into an unfavorable attack
            if (destAttacked && movingValue >= PieceType.KNIGHT.getPoints()) {
                if (!destDefended) {
                    adjustment -= movingValue * 500;
                } else if (minEnemyAttacker < movingValue) {
                    adjustment -= (movingValue - minEnemyAttacker) * 450;
                }
            }
        }

        return adjustment;
    }

    /**
     * Penalizes hanging high-value pieces belonging to {@code side} during the last 10 seconds.
     */
    private static int computeHangingPenalty(Board board, Color side, Color sideToMove) {
        Color opponent = side.opposite();
        boolean opponentMovesNext = (sideToMove == opponent);
        int penalty = 0;

        for (int rank = 0; rank < Square.BOARD_SIZE; rank++) {
            for (int file = 0; file < Square.BOARD_SIZE; file++) {
                Piece piece = board.getPieceAt(rank, file);
                if (piece == null || piece.color() != side) {
                    continue;
                }
                int value = piece.getPoints();
                if (value < PieceType.KNIGHT.getPoints()) {
                    continue;
                }
                int minAttacker = getLeastValuableAttackerPoints(board, rank, file, opponent);
                if (minAttacker == 0) {
                    continue;
                }
                boolean defended = getLeastValuableAttackerPoints(board, rank, file, side) > 0;
                if (opponentMovesNext) {
                    if (!defended) {
                        penalty += value * 550;
                    } else if (minAttacker < value) {
                        penalty += (value - minAttacker) * 500;
                    }
                } else if (value >= PieceType.ROOK.getPoints()) {
                    if (!defended) {
                        penalty += value * 120;
                    } else if (minAttacker < value) {
                        penalty += (value - minAttacker) * 100;
                    }
                }
            }
        }

        return penalty;
    }

    /**
     * Returns true if square {@code (targetRank, targetFile)} is attacked by any piece of {@code attackerColor}.
     */
    public static boolean isSquareAttackedBy(Board board,
                                             int targetRank,
                                             int targetFile,
                                             Color attackerColor) {
        return getLeastValuableAttackerPoints(board, targetRank, targetFile, attackerColor) > 0;
    }

    /**
     * Returns the point value of the least valuable piece of {@code attackerColor} that attacks
     * {@code (targetRank, targetFile)}, or {@code 0} if no piece of {@code attackerColor} attacks it.
     */
    public static int getLeastValuableAttackerPoints(Board board,
                                                     int targetRank,
                                                     int targetFile,
                                                     Color attackerColor) {
        // 1. Check Pawn attacks (1 pt - lowest possible attacker)
        int pawnAttackOriginRank = (attackerColor == Color.WHITE) ? (targetRank - 1) : (targetRank + 1);
        if (pawnAttackOriginRank >= 0 && pawnAttackOriginRank < Square.BOARD_SIZE) {
            if (targetFile - 1 >= 0) {
                Piece p = board.getPieceAt(pawnAttackOriginRank, targetFile - 1);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.PAWN) {
                    return PieceType.PAWN.getPoints();
                }
            }
            if (targetFile + 1 < Square.BOARD_SIZE) {
                Piece p = board.getPieceAt(pawnAttackOriginRank, targetFile + 1);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.PAWN) {
                    return PieceType.PAWN.getPoints();
                }
            }
        }

        // 2. Check Knight attacks (2 pts)
        for (int[] offset : KNIGHT_OFFSETS) {
            int r = targetRank + offset[0];
            int f = targetFile + offset[1];
            if (Square.isValid(r, f)) {
                Piece p = board.getPieceAt(r, f);
                if (p != null && p.color() == attackerColor && p.type() == PieceType.KNIGHT) {
                    return PieceType.KNIGHT.getPoints();
                }
            }
        }

        int minPoints = Integer.MAX_VALUE;

        // 3. Check diagonal rays: Bishop (3 pts), Queen (5 pts), adjacent King (7 pts)
        for (int[] dir : DIAGONAL_DIRS) {
            int r = targetRank + dir[0];
            int f = targetFile + dir[1];
            int dist = 1;
            while (Square.isValid(r, f)) {
                Piece p = board.getPieceAt(r, f);
                if (p != null) {
                    if (p.color() == attackerColor) {
                        PieceType type = p.type();
                        if (type == PieceType.BISHOP || type == PieceType.QUEEN
                                || (dist == 1 && type == PieceType.KING)) {
                            minPoints = Math.min(minPoints, type.getPoints());
                        }
                    }
                    break;
                }
                r += dir[0];
                f += dir[1];
                dist++;
            }
        }

        if (minPoints == PieceType.BISHOP.getPoints()) {
            return minPoints;
        }

        // 4. Check orthogonal rays: Rook (4 pts), Queen (5 pts), adjacent King (7 pts)
        for (int[] dir : ORTHOGONAL_DIRS) {
            int r = targetRank + dir[0];
            int f = targetFile + dir[1];
            int dist = 1;
            while (Square.isValid(r, f)) {
                Piece p = board.getPieceAt(r, f);
                if (p != null) {
                    if (p.color() == attackerColor) {
                        PieceType type = p.type();
                        if (type == PieceType.ROOK || type == PieceType.QUEEN
                                || (dist == 1 && type == PieceType.KING)) {
                            minPoints = Math.min(minPoints, type.getPoints());
                        }
                    }
                    break;
                }
                r += dir[0];
                f += dir[1];
                dist++;
            }
        }

        return minPoints == Integer.MAX_VALUE ? 0 : minPoints;
    }
}
