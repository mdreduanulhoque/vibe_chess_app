package com.reduan.vibechess.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Minimax AI engine with alpha-beta pruning, MVV-LVA move ordering, iterative deepening,
 * strict per-move clock budgeting, and last-10-seconds safe-capture/anti-hanging awareness.
 * <p>
 * Pure Java (zero Android dependencies). Understands Vibe Chess rules:
 * no check, kings are capturable and worth 7 points, and final board points at timeout decide the game.
 */
public final class MinimaxAi {

    /**
     * Hard difficulty per-move maximum time cap (1.0 second).
     */
    public static final long HARD_MAX_MOVE_TIME_MILLIS = 1_000L;

    private static final int INF = 1_000_000_000;

    private final Random random;

    public MinimaxAi() {
        this(new Random());
    }

    public MinimaxAi(Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /**
     * Computes the time budget in milliseconds for the AI move in {@code state} at {@code difficulty}.
     * <ul>
     *   <li>For {@link AiDifficulty#HARD}: {@code min(1000ms, 5% of remaining clock)}.</li>
     *   <li>For {@link AiDifficulty#MEDIUM} and {@link AiDifficulty#EASY}: bounded by remaining clock.</li>
     *   <li>Never exceeds the remaining clock of the active or moving side.</li>
     * </ul>
     */
    public static long computeTimeBudgetMillis(GameState state, AiDifficulty difficulty) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(difficulty, "difficulty must not be null");

        ChessClock clock = state.getClock();
        long ownRemaining = clock.getRemainingMillis(state.getSideToMove());
        long activeRemaining = clock.getRemainingMillis(clock.getActiveSide());
        long remainingClock = Math.max(1L, Math.min(ownRemaining, activeRemaining));

        long fivePercent = Math.max(1L, (remainingClock * 5L) / 100L);
        long budget = switch (difficulty) {
            case HARD -> Math.min(HARD_MAX_MOVE_TIME_MILLIS, fivePercent);
            case MEDIUM -> Math.min(600L, Math.max(1L, remainingClock / 2L));
            case EASY -> Math.min(250L, Math.max(1L, remainingClock / 2L));
        };
        return Math.min(budget, remainingClock);
    }

    /**
     * Chooses a legal move for {@code state.getSideToMove()} using the difficulty's depth and time cap.
     * Returns {@code null} only if the game is already over or there are no legal moves.
     */
    public Move chooseMove(GameState state, AiDifficulty difficulty) {
        long budgetMillis = computeTimeBudgetMillis(state, difficulty);
        return chooseMove(state, difficulty, budgetMillis);
    }

    /**
     * Chooses a legal move for {@code state.getSideToMove()} within {@code maxTimeBudgetMillis}
     * (also clamped to never exceed the remaining clock in {@code state}).
     */
    public Move chooseMove(GameState state, AiDifficulty difficulty, long maxTimeBudgetMillis) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(difficulty, "difficulty must not be null");

        if (state.isGameOver()) {
            return null;
        }

        List<Move> legalMoves = state.generateMoves();
        if (legalMoves.isEmpty()) {
            return null;
        }
        if (legalMoves.size() == 1) {
            return legalMoves.get(0);
        }

        long startNanos = System.nanoTime();
        ChessClock clock = state.getClock();
        long remainingClock = Math.max(
                1L,
                Math.min(
                        clock.getRemainingMillis(state.getSideToMove()),
                        clock.getRemainingMillis(clock.getActiveSide())
                )
        );
        long clampedBudgetMillis = Math.max(1L, Math.min(maxTimeBudgetMillis, remainingClock));
        // Reserve a small safety margin so search unwinds and returns strictly before budget expiry
        long searchWindowMillis = clampedBudgetMillis > 25L
                ? clampedBudgetMillis - Math.min(20L, clampedBudgetMillis / 5L)
                : Math.max(1L, clampedBudgetMillis / 2L);
        long deadlineNanos = startNanos + TimeUnit.MILLISECONDS.toNanos(searchWindowMillis);

        boolean lastTenSeconds = BoardEvaluator.isLastTenSeconds(state);
        Color aiColor = state.getSideToMove();

        if (difficulty == AiDifficulty.EASY) {
            return chooseEasyMove(state, legalMoves, aiColor, lastTenSeconds, deadlineNanos);
        }

        return chooseIterativeDeepeningMove(
                state,
                legalMoves,
                aiColor,
                difficulty.getMaxDepth(),
                lastTenSeconds,
                deadlineNanos
        );
    }

    /**
     * Easy difficulty: depth-1 evaluation of all legal moves, with ~25% probability of picking
     * randomly among the top 3 moves.
     */
    private Move chooseEasyMove(GameState state,
                                List<Move> legalMoves,
                                Color aiColor,
                                boolean lastTenSeconds,
                                long deadlineNanos) {
        List<Move> ordered = MoveOrderer.orderMoves(state, legalMoves, null);
        List<ScoredMove> scoredMoves = new ArrayList<>(ordered.size());

        for (Move move : ordered) {
            GameState nextState = state.applyMove(move);
            int eval = BoardEvaluator.evaluate(nextState, aiColor, lastTenSeconds);
            if (lastTenSeconds) {
                eval += BoardEvaluator.evaluateMoveSafetyAdjustment(state, move, nextState, aiColor);
            }
            scoredMoves.add(new ScoredMove(move, eval));
            if (System.nanoTime() >= deadlineNanos || Thread.currentThread().isInterrupted()) {
                break;
            }
        }

        scoredMoves.sort((a, b) -> Integer.compare(b.score, a.score));
        int poolSize = Math.min(AiDifficulty.EASY.getTopMovePoolSize(), scoredMoves.size());
        if (poolSize > 1 && random.nextDouble() < AiDifficulty.EASY.getRandomTopMoveProbability()) {
            int index = random.nextInt(poolSize);
            return scoredMoves.get(index).move;
        }
        return scoredMoves.get(0).move;
    }

    /**
     * Medium (depth 3) and Hard (iterative deepening up to depth 5) search with alpha-beta pruning
     * and MVV-LVA + principal-variation move ordering.
     */
    private Move chooseIterativeDeepeningMove(GameState state,
                                              List<Move> legalMoves,
                                              Color aiColor,
                                              int maxDepth,
                                              boolean lastTenSeconds,
                                              long deadlineNanos) {
        List<Move> initialOrdered = MoveOrderer.orderMoves(state, legalMoves, null);
        Move bestMove = initialOrdered.get(0);

        SearchContext context = new SearchContext(deadlineNanos, lastTenSeconds);

        for (int depth = 1; depth <= maxDepth; depth++) {
            if (System.nanoTime() >= deadlineNanos || Thread.currentThread().isInterrupted()) {
                break;
            }

            List<Move> orderedRootMoves = MoveOrderer.orderMoves(state, legalMoves, bestMove);
            Move depthBestMove = bestMove;
            int depthBestScore = -INF;
            int alpha = -INF;
            int beta = INF;
            boolean completedAtLeastOneMove = false;

            for (Move move : orderedRootMoves) {
                if (completedAtLeastOneMove && context.shouldAbort()) {
                    context.aborted = true;
                    break;
                }

                GameState childState = state.applyMove(move);
                int safetyAdjustment = lastTenSeconds
                        ? BoardEvaluator.evaluateMoveSafetyAdjustment(state, move, childState, aiColor)
                        : 0;

                int childScore = minimax(
                        childState,
                        depth - 1,
                        alpha - safetyAdjustment,
                        beta - safetyAdjustment,
                        aiColor,
                        context
                );

                if (context.aborted && completedAtLeastOneMove) {
                    break;
                }

                int totalScore = childScore + safetyAdjustment;
                if (!completedAtLeastOneMove || totalScore > depthBestScore) {
                    depthBestScore = totalScore;
                    depthBestMove = move;
                }
                if (depthBestScore > alpha) {
                    alpha = depthBestScore;
                }
                completedAtLeastOneMove = true;

                if (context.aborted) {
                    break;
                }
            }

            if (completedAtLeastOneMove) {
                bestMove = depthBestMove;
            }
            if (context.aborted) {
                break;
            }
        }

        return bestMove;
    }

    private int minimax(GameState node,
                        int depth,
                        int alpha,
                        int beta,
                        Color maximizingColor,
                        SearchContext context) {
        if (context.tickAndCheckAbort()) {
            return 0;
        }
        if (depth <= 0 || node.isGameOver()) {
            return BoardEvaluator.evaluate(node, maximizingColor, context.lastTenSeconds);
        }

        List<Move> moves = node.generateMoves();
        if (moves.isEmpty()) {
            // Per PROJECT_SPEC.md: if a side has no legal move, its turn is skipped.
            GameState skipped = node.skipTurn();
            if (!skipped.hasLegalMoves()) {
                return BoardEvaluator.evaluate(node, maximizingColor, context.lastTenSeconds);
            }
            return minimax(skipped, depth - 1, alpha, beta, maximizingColor, context);
        }

        List<Move> ordered = MoveOrderer.orderMoves(node, moves, null);
        boolean maximizing = (node.getSideToMove() == maximizingColor);

        if (maximizing) {
            int best = -INF;
            for (Move move : ordered) {
                int val = minimax(node.applyMove(move), depth - 1, alpha, beta, maximizingColor, context);
                if (context.aborted) {
                    return 0;
                }
                if (val > best) {
                    best = val;
                }
                if (best > alpha) {
                    alpha = best;
                }
                if (beta <= alpha) {
                    break;
                }
            }
            return best;
        } else {
            int best = INF;
            for (Move move : ordered) {
                int val = minimax(node.applyMove(move), depth - 1, alpha, beta, maximizingColor, context);
                if (context.aborted) {
                    return 0;
                }
                if (val < best) {
                    best = val;
                }
                if (best < beta) {
                    beta = best;
                }
                if (beta <= alpha) {
                    break;
                }
            }
            return best;
        }
    }

    private record ScoredMove(Move move, int score) {
    }

    private static final class SearchContext {
        final long deadlineNanos;
        final boolean lastTenSeconds;
        int nodesVisited = 0;
        boolean aborted = false;

        SearchContext(long deadlineNanos, boolean lastTenSeconds) {
            this.deadlineNanos = deadlineNanos;
            this.lastTenSeconds = lastTenSeconds;
        }

        boolean shouldAbort() {
            if (aborted) {
                return true;
            }
            if (Thread.currentThread().isInterrupted() || System.nanoTime() >= deadlineNanos) {
                aborted = true;
                return true;
            }
            return false;
        }

        boolean tickAndCheckAbort() {
            if (aborted) {
                return true;
            }
            if ((++nodesVisited & 15) == 0) {
                return shouldAbort();
            }
            return false;
        }
    }
}
