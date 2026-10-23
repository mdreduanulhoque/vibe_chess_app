package com.reduan.vibechess.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Orders moves for alpha-beta pruning efficiency:
 * <ol>
 *   <li>Principal variation move (best move from previous iterative-deepening depth) first.</li>
 *   <li>Captures ordered by Most Valuable Victim - Least Valuable Attacker (MVV-LVA),
 *       using spec point values (Pawn=1, Knight=2, Bishop=3, Rook=4, Queen=5, King=7).</li>
 *   <li>Promotions ordered by promoted piece value.</li>
 *   <li>Quiet moves ordered by center control and pawn advancement.</li>
 * </ol>
 */
public final class MoveOrderer {

    private static final int PV_MOVE_BONUS = 1_000_000;
    private static final int CAPTURE_BASE_SCORE = 100_000;
    private static final int PROMOTION_BASE_SCORE = 50_000;

    private MoveOrderer() {
    }

    /**
     * Returns a new list containing {@code moves} sorted from highest to lowest move-ordering score.
     */
    public static List<Move> orderMoves(GameState state, List<Move> moves, Move pvMove) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(moves, "moves must not be null");
        int size = moves.size();
        if (size <= 1) {
            return new ArrayList<>(moves);
        }

        Board board = state.getBoard();
        Move[] array = moves.toArray(new Move[size]);
        int[] scores = new int[size];

        for (int i = 0; i < size; i++) {
            scores[i] = scoreMove(board, array[i], pvMove);
        }

        // Insertion sort is allocation-free and fast for typical chess move lists (20-45 moves)
        for (int i = 1; i < size; i++) {
            Move keyMove = array[i];
            int keyScore = scores[i];
            int j = i - 1;
            while (j >= 0 && scores[j] < keyScore) {
                array[j + 1] = array[j];
                scores[j + 1] = scores[j];
                j--;
            }
            array[j + 1] = keyMove;
            scores[j + 1] = keyScore;
        }

        List<Move> ordered = new ArrayList<>(size);
        for (Move m : array) {
            ordered.add(m);
        }
        return ordered;
    }

    /**
     * Computes the MVV-LVA and positional priority score for a single move.
     */
    public static int scoreMove(Board board, Move move, Move pvMove) {
        if (move.equals(pvMove)) {
            return PV_MOVE_BONUS;
        }

        Piece attacker = board.getPieceAt(move.from());
        int attackerPoints = (attacker != null) ? attacker.getPoints() : 1;
        int promoPoints = move.isPromotion() ? move.promotion().getPoints() : 0;

        if (move.isCapture()) {
            int victimPoints;
            if (move.isEnPassant()) {
                victimPoints = PieceType.PAWN.getPoints();
            } else {
                Piece victim = board.getPieceAt(move.to());
                victimPoints = (victim != null) ? victim.getPoints() : 1;
            }
            // MVV-LVA: prioritize high victim value (King=7 > Queen=5 > Rook=4 > ...) and low attacker value
            return CAPTURE_BASE_SCORE
                    + (victimPoints * 1_000)
                    - (attackerPoints * 10)
                    + (promoPoints * 500);
        }

        if (move.isPromotion()) {
            return PROMOTION_BASE_SCORE + (promoPoints * 1_000);
        }

        int quietScore = BoardEvaluator.centerControlBonus(move.to().rank(), move.to().file());
        if (attacker != null && attacker.type() == PieceType.PAWN) {
            int advance = (attacker.color() == Color.WHITE)
                    ? (move.to().rank() - move.from().rank())
                    : (move.from().rank() - move.to().rank());
            quietScore += Math.max(0, advance * 4);
        } else if (move.isCastle()) {
            quietScore += 15;
        }
        return quietScore;
    }
}
