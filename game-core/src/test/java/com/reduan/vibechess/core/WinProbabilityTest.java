package com.reduan.vibechess.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Tests WinProbability formula, percentage sum invariant, game-over edge cases,
 * and monotonicity with respect to point differential and remaining time.
 */
public class WinProbabilityTest {

    @Test
    public void equalPointsReturnsFiftyFifty() {
        WinProbability prob = WinProbability.compute(38, 38, 60_000L, 60_000L, 60_000L);
        assertEquals(50, prob.getWhitePercent());
        assertEquals(50, prob.getBlackPercent());
    }

    @Test
    public void matchesExactLogisticFormula() {
        // At start: r = 1.0, k = 1.5 + 4.5 * 1.0 = 6.0
        // If d = +6 (e.g., White 38, Black 32), pWhite = 1 / (1 + exp(-1)) = 0.731058... -> 73% / 27%
        WinProbability startLead = WinProbability.compute(38, 32, 60_000L, 60_000L, 60_000L);
        assertEquals(73, startLead.getWhitePercent());
        assertEquals(27, startLead.getBlackPercent());

        // Near end: r -> 0 (e.g. 1ms left each out of 60_000ms), k ≈ 1.5
        // For d = +3, pWhite ≈ 1 / (1 + exp(-2)) = 0.880797... -> 88% / 12%
        WinProbability lateLead = WinProbability.compute(20, 17, 1L, 1L, 60_000L);
        assertEquals(88, lateLead.getWhitePercent());
        assertEquals(12, lateLead.getBlackPercent());
    }

    @Test
    public void monotonicityMorePointsYieldsHigherOrEqualPercentageAndSumsTo100() {
        long initialMillis = 60_000L;
        long whiteMillis = 45_000L;
        long blackMillis = 45_000L;
        int blackPoints = 20;

        int previousWhitePct = -1;
        for (int whitePoints = 0; whitePoints <= 38; whitePoints++) {
            WinProbability prob = WinProbability.compute(
                    whitePoints, blackPoints, whiteMillis, blackMillis, initialMillis);
            assertEquals(100, prob.getWhitePercent() + prob.getBlackPercent());
            assertTrue("Expected monotonic non-decreasing whitePercent as points increase",
                    prob.getWhitePercent() >= previousWhitePct);
            previousWhitePct = prob.getWhitePercent();
        }
        // Strictly higher when ahead vs behind
        WinProbability behind = WinProbability.compute(15, 25, whiteMillis, blackMillis, initialMillis);
        WinProbability ahead = WinProbability.compute(25, 15, whiteMillis, blackMillis, initialMillis);
        assertTrue(ahead.getWhitePercent() > 50);
        assertTrue(behind.getWhitePercent() < 50);
        assertEquals(100, ahead.getWhitePercent() + behind.getWhitePercent());
    }

    @Test
    public void gameOverReturns100ZeroOr50FiftyOnTie() {
        WinProbability whiteWins = WinProbability.compute(25, 18, 0L, 15_000L, 60_000L);
        assertEquals(100, whiteWins.getWhitePercent());
        assertEquals(0, whiteWins.getBlackPercent());

        WinProbability blackWins = WinProbability.compute(12, 20, 10_000L, 0L, 60_000L);
        assertEquals(0, blackWins.getWhitePercent());
        assertEquals(100, blackWins.getBlackPercent());

        WinProbability draw = WinProbability.compute(20, 20, 0L, 10_000L, 60_000L);
        assertEquals(50, draw.getWhitePercent());
        assertEquals(50, draw.getBlackPercent());
    }
}
