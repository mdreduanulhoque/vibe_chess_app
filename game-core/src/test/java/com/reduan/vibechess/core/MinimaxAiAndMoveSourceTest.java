package com.reduan.vibechess.core;

import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link MinimaxAi}, {@link BoardEvaluator}, {@link MoveOrderer},
 * and {@link AiMoveSource}.
 */
public class MinimaxAiAndMoveSourceTest {

    @Test
    public void aiTakesFreeQueen_asWhiteAndBlack() {
        MinimaxAi ai = new MinimaxAi(new Random(42L));

        // 1. White to move: White Rook on d1 can capture undefended Black Queen on d5
        Board whiteBoard = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("d1"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("a2"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("d5"), Piece.of(Color.BLACK, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("a7"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState whiteState = GameState.custom(
                whiteBoard, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        Move expectedWhiteCapture = Move.capture(
                Square.fromAlgebraic("d1"), Square.fromAlgebraic("d5"));

        assertEquals(expectedWhiteCapture, ai.chooseMove(whiteState, AiDifficulty.MEDIUM));
        assertEquals(expectedWhiteCapture, ai.chooseMove(whiteState, AiDifficulty.HARD));

        // 2. Black to move: Black Bishop on c5 can capture undefended White Queen on f2
        Board blackBoard = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("f2"), Piece.of(Color.WHITE, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("c5"), Piece.of(Color.BLACK, PieceType.BISHOP));

        GameState blackState = GameState.custom(
                blackBoard, Color.BLACK, CastlingRights.none(), null, new ChessClock(60_000L));

        Move expectedBlackCapture = Move.capture(
                Square.fromAlgebraic("c5"), Square.fromAlgebraic("f2"));

        assertEquals(expectedBlackCapture, ai.chooseMove(blackState, AiDifficulty.MEDIUM));
        assertEquals(expectedBlackCapture, ai.chooseMove(blackState, AiDifficulty.HARD));
    }

    @Test
    public void aiUnderstandsKingIsCapturableAndWorthSevenPoints() {
        MinimaxAi ai = new MinimaxAi(new Random(1L));

        // White Rook on a4 can capture either undefended Black Queen on a8 (5 pts)
        // or undefended Black King on h4 (7 pts). Neither piece defends the other.
        // White must choose capturing the King (7 pts > 5 pts).
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a4"), Piece.of(Color.WHITE, PieceType.ROOK))
                .withPieceAt(Square.fromAlgebraic("a8"), Piece.of(Color.BLACK, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("h4"), Piece.of(Color.BLACK, PieceType.KING));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        Move captureKing = Move.capture(Square.fromAlgebraic("a4"), Square.fromAlgebraic("h4"));
        assertEquals(captureKing, ai.chooseMove(state, AiDifficulty.MEDIUM));
        assertEquals(captureKing, ai.chooseMove(state, AiDifficulty.HARD));
    }

    @Test
    public void moveOrderer_ordersCapturesFirstUsingMvvLva() {
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("d4"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e3"), Piece.of(Color.WHITE, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("e5"), Piece.of(Color.BLACK, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("c5"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        List<Move> moves = state.generateMoves();
        List<Move> ordered = MoveOrderer.orderMoves(state, moves, null);

        // First move must be Pawn captures Queen (d4xe5: MVV=5, LVA=1)
        Move pawnCapturesQueen = Move.capture(Square.fromAlgebraic("d4"), Square.fromAlgebraic("e5"));
        // Second move must be Queen captures Queen (e3xe5: MVV=5, LVA=5)
        Move queenCapturesQueen = Move.capture(Square.fromAlgebraic("e3"), Square.fromAlgebraic("e5"));

        assertEquals(pawnCapturesQueen, ordered.get(0));
        assertEquals(queenCapturesQueen, ordered.get(1));
        assertTrue(ordered.get(2).isCapture());
        assertFalse(ordered.get(ordered.size() - 1).isCapture());
    }

    @Test
    public void boardEvaluator_usesSpecPointDifferencePlusPositionalTieBreaker() {
        // Equal material, but White has a central advanced pawn on e4 while Black pawn is on a7
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("e4"), Piece.of(Color.WHITE, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("a7"), Piece.of(Color.BLACK, PieceType.PAWN));

        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, new ChessClock(60_000L));

        int whiteEval = BoardEvaluator.evaluate(state, Color.WHITE, false);
        int blackEval = BoardEvaluator.evaluate(state, Color.BLACK, false);

        assertTrue("White should have positive positional tie-breaker", whiteEval > 0);
        assertTrue("Positional bonus must be smaller than 1 full point (1000)", whiteEval < BoardEvaluator.POINT_SCALE);
        assertEquals(-whiteEval, blackEval);
    }

    @Test
    public void aiNeverMakesIllegalMove_acrossDifficultiesAndColors() {
        MinimaxAi ai = new MinimaxAi(new Random(123L));

        for (AiDifficulty difficulty : AiDifficulty.values()) {
            GameState state = GameState.initial(60_000L);
            int pliesToPlay = (difficulty == AiDifficulty.HARD) ? 6 : 16;

            for (int ply = 0; ply < pliesToPlay && !state.isGameOver(); ply++) {
                List<Move> legalMoves = state.generateMoves();
                if (legalMoves.isEmpty()) {
                    state = state.skipTurn();
                    continue;
                }
                Move chosen = ai.chooseMove(state, difficulty, 150L);
                assertNotNull("AI must return a non-null move when legal moves exist", chosen);
                assertTrue(
                        "AI move " + chosen + " must be in legal moves list for " + state.getSideToMove(),
                        legalMoves.contains(chosen)
                );
                state = state.applyMove(chosen);
            }
        }
    }

    @Test
    public void aiAlwaysReturnsMoveWithinTimeBudget_andNeverExceedsRemainingClock() {
        MinimaxAi ai = new MinimaxAi(new Random(7L));

        // 1. Hard difficulty with 4,000ms remaining clock -> 5% budget is 200ms
        ChessClock fourSecClock = new ChessClock(60_000L, 4_000L, 4_000L, Color.WHITE);
        GameState fourSecState = GameState.custom(
                Board.initial(), Color.WHITE, CastlingRights.all(), null, fourSecClock);

        long expectedBudget = MinimaxAi.computeTimeBudgetMillis(fourSecState, AiDifficulty.HARD);
        assertEquals(200L, expectedBudget);

        long startMs = System.nanoTime();
        Move move1 = ai.chooseMove(fourSecState, AiDifficulty.HARD);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startMs);

        assertNotNull(move1);
        assertTrue(fourSecState.generateMoves().contains(move1));
        assertTrue("Elapsed " + elapsedMs + "ms exceeded time budget", elapsedMs <= expectedBudget + 50L);
        assertTrue("Elapsed " + elapsedMs + "ms exceeded remaining clock", elapsedMs < 4_000L);

        // 2. Very tight remaining clock (80ms left)
        ChessClock tightClock = new ChessClock(60_000L, 80L, 80L, Color.BLACK);
        GameState tightState = GameState.custom(
                Board.initial(), Color.BLACK, CastlingRights.all(), null, tightClock);

        long startTight = System.nanoTime();
        Move move2 = ai.chooseMove(tightState, AiDifficulty.HARD);
        long elapsedTightMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTight);

        assertNotNull(move2);
        assertTrue(tightState.generateMoves().contains(move2));
        assertTrue("Elapsed " + elapsedTightMs + "ms exceeded tight clock", elapsedTightMs <= 80L + 40L);
    }

    @Test
    public void lastTenSeconds_favorsSafeCapturesAndAvoidsHangingHighValuePiece() {
        // Deterministically disable the 25% random Easy pick by using a Random that returns > 0.25
        Random nonRandomEasy = new Random() {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
        MinimaxAi ai = new MinimaxAi(nonRandomEasy);

        // White has 5 seconds left (last 10 seconds mode).
        // White Queen on d4 can either:
        // - Capture Black Pawn on d7 which is defended by Black Pawn on e6 (UNSAFE: hangs 5-pt Queen for 1-pt Pawn!)
        // - Capture Black Knight on g4 which is completely undefended (SAFE capture: +2 pts safely!)
        Board board = Board.empty()
                .withPieceAt(Square.fromAlgebraic("e1"), Piece.of(Color.WHITE, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("d4"), Piece.of(Color.WHITE, PieceType.QUEEN))
                .withPieceAt(Square.fromAlgebraic("a8"), Piece.of(Color.BLACK, PieceType.KING))
                .withPieceAt(Square.fromAlgebraic("d7"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("e6"), Piece.of(Color.BLACK, PieceType.PAWN))
                .withPieceAt(Square.fromAlgebraic("g4"), Piece.of(Color.BLACK, PieceType.KNIGHT));

        ChessClock lastTenClock = new ChessClock(60_000L, 5_000L, 5_000L, Color.WHITE);
        GameState state = GameState.custom(
                board, Color.WHITE, CastlingRights.none(), null, lastTenClock);

        assertTrue(BoardEvaluator.isLastTenSeconds(state));

        Move safeCapture = Move.capture(Square.fromAlgebraic("d4"), Square.fromAlgebraic("g4"));
        Move unsafePoisonedPawn = Move.capture(Square.fromAlgebraic("d4"), Square.fromAlgebraic("d7"));

        Move easyChoice = ai.chooseMove(state, AiDifficulty.EASY);
        Move mediumChoice = ai.chooseMove(state, AiDifficulty.MEDIUM);

        assertEquals(safeCapture, easyChoice);
        assertEquals(safeCapture, mediumChoice);
        assertFalse(easyChoice.equals(unsafePoisonedPawn));
    }

    @Test
    public void aiMoveSource_runsOnBackgroundThreadAndSupportsCleanCancel() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Move> chosenMoveRef = new AtomicReference<>();
        AtomicBoolean sawThinkingTrue = new AtomicBoolean(false);
        AtomicBoolean finalThinkingState = new AtomicBoolean(true);

        AiMoveSource aiSource = new AiMoveSource(
                AiDifficulty.MEDIUM,
                Runnable::run,
                thinking -> {
                    if (thinking) {
                        sawThinkingTrue.set(true);
                    } else {
                        finalThinkingState.set(false);
                    }
                }
        );

        try {
            assertFalse(aiSource.isHumanInteractive());
            GameState initial = GameState.initial(60_000L);
            aiSource.requestMove(initial, move -> {
                chosenMoveRef.set(move);
                latch.countDown();
            });

            assertTrue("AiMoveSource should complete within 2 seconds", latch.await(2, TimeUnit.SECONDS));
            assertTrue(sawThinkingTrue.get());
            assertFalse(finalThinkingState.get());
            assertNotNull(chosenMoveRef.get());
            assertTrue(initial.generateMoves().contains(chosenMoveRef.get()));

            // Verify clean cancellation
            AtomicReference<Move> cancelledMoveRef = new AtomicReference<>();
            aiSource.requestMove(initial, cancelledMoveRef::set);
            aiSource.cancel();
            assertFalse(aiSource.isThinking());
            Thread.sleep(100L);
            assertNull("Cancelled request must not deliver a move", cancelledMoveRef.get());
        } finally {
            aiSource.shutdown();
        }
    }
}
