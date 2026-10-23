package com.reduan.vibechess.core;

/**
 * Difficulty levels for Single Player vs Computer AI per PROJECT_SPEC.md:
 * <ul>
 *   <li>{@link #EASY}: depth 1 plus ~25% chance of picking a random move among the top 3 moves.</li>
 *   <li>{@link #MEDIUM}: depth 3 minimax with alpha-beta pruning.</li>
 *   <li>{@link #HARD}: iterative deepening up to depth 5 with a per-move time cap
 *       (about 1.0 s or 5% of remaining clock, whichever is lower).</li>
 * </ul>
 */
public enum AiDifficulty {
    EASY(1, 0.25, 3),
    MEDIUM(3, 0.0, 1),
    HARD(5, 0.0, 1);

    private final int maxDepth;
    private final double randomTopMoveProbability;
    private final int topMovePoolSize;

    AiDifficulty(int maxDepth, double randomTopMoveProbability, int topMovePoolSize) {
        this.maxDepth = maxDepth;
        this.randomTopMoveProbability = randomTopMoveProbability;
        this.topMovePoolSize = topMovePoolSize;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public double getRandomTopMoveProbability() {
        return randomTopMoveProbability;
    }

    public int getTopMovePoolSize() {
        return topMovePoolSize;
    }
}
