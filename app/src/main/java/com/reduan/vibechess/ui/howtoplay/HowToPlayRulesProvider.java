package com.reduan.vibechess.ui.howtoplay;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.reduan.vibechess.R;
import com.reduan.vibechess.core.Color;
import com.reduan.vibechess.core.PieceType;
import com.reduan.vibechess.ui.game.BoardDemoConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides the ordered 18 visual rule sections for the "How to Play" screen,
 * including code-drawn {@link BoardDemoConfig} illustrations for each rule.
 */
public final class HowToPlayRulesProvider {

    public enum ExtraVisualType {
        NONE,
        DUAL_BOARD_EN_PASSANT,
        POINTS_TABLE,
        TIMER_BADGES,
        PROBABILITY_BAR
    }

    public static final class RuleSection {
        public final int sectionNumber;
        @StringRes
        public final int titleRes;
        @StringRes
        public final int bodyRes;
        @NonNull
        public final BoardDemoConfig primaryBoard;
        @Nullable
        public final BoardDemoConfig secondaryBoard;
        @NonNull
        public final ExtraVisualType extraVisualType;
        public final boolean highlightCardBorder;

        RuleSection(int sectionNumber,
                    @StringRes int titleRes,
                    @StringRes int bodyRes,
                    @NonNull BoardDemoConfig primaryBoard,
                    @Nullable BoardDemoConfig secondaryBoard,
                    @NonNull ExtraVisualType extraVisualType,
                    boolean highlightCardBorder) {
            this.sectionNumber = sectionNumber;
            this.titleRes = titleRes;
            this.bodyRes = bodyRes;
            this.primaryBoard = primaryBoard;
            this.secondaryBoard = secondaryBoard;
            this.extraVisualType = extraVisualType;
            this.highlightCardBorder = highlightCardBorder;
        }
    }

    private HowToPlayRulesProvider() {
    }

    /**
     * Returns all 18 sections in the exact specification order.
     */
    @NonNull
    public static List<RuleSection> getSections() {
        List<RuleSection> list = new ArrayList<>(18);

        // 1. Goal of the game: have more points on the board when time ends.
        list.add(new RuleSection(
                1,
                R.string.how_to_play_sec1_title,
                R.string.how_to_play_sec1_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e2", Color.WHITE, PieceType.KING)
                        .piece("d4", Color.WHITE, PieceType.QUEEN)
                        .piece("f3", Color.WHITE, PieceType.ROOK)
                        .piece("e5", Color.BLACK, PieceType.KNIGHT)
                        .piece("c5", Color.BLACK, PieceType.PAWN)
                        .pulse("e2", "d4", "f3")
                        .rings("e5")
                        .arrow("d4", "e5")
                        .build(),
                null,
                ExtraVisualType.NONE,
                true
        ));

        // 2. The board and starting setup.
        list.add(new RuleSection(
                2,
                R.string.how_to_play_sec2_title,
                R.string.how_to_play_sec2_body,
                BoardDemoConfig.builder8x8()
                        .board(com.reduan.vibechess.core.Board.initial())
                        .pulse("e2", "d2")
                        .dots("e3", "e4")
                        .arrow("e2", "e4")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 3. Pawn: forward 1, forward 2 on its first move, captures diagonally, never forward.
        list.add(new RuleSection(
                3,
                R.string.how_to_play_sec3_title,
                R.string.how_to_play_sec3_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e2", Color.WHITE, PieceType.PAWN)
                        .piece("d3", Color.BLACK, PieceType.KNIGHT)
                        .piece("f3", Color.BLACK, PieceType.PAWN)
                        .pulse("e2")
                        .dots("e3", "e4")
                        .rings("d3", "f3")
                        .arrow("e2", "d3")
                        .arrow("e2", "f3")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 4. Knight: L-shape, jumps over pieces.
        list.add(new RuleSection(
                4,
                R.string.how_to_play_sec4_title,
                R.string.how_to_play_sec4_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e4", Color.WHITE, PieceType.KNIGHT)
                        .piece("d4", Color.WHITE, PieceType.PAWN)
                        .piece("e5", Color.BLACK, PieceType.PAWN)
                        .piece("f4", Color.WHITE, PieceType.PAWN)
                        .piece("e3", Color.WHITE, PieceType.PAWN)
                        .piece("f2", Color.BLACK, PieceType.BISHOP)
                        .pulse("e4")
                        .dots("d6", "f6", "c5", "g5", "c3", "g3", "d2")
                        .rings("f2")
                        .arrow("e4", "d6")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 5. Bishop: diagonals, any distance.
        list.add(new RuleSection(
                5,
                R.string.how_to_play_sec5_title,
                R.string.how_to_play_sec5_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e4", Color.WHITE, PieceType.BISHOP)
                        .piece("g6", Color.BLACK, PieceType.ROOK)
                        .pulse("e4")
                        .dots("d5", "c6", "f5", "d3", "c2", "f3", "g2")
                        .rings("g6")
                        .arrow("e4", "g6")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 6. Rook: straight lines, any distance.
        list.add(new RuleSection(
                6,
                R.string.how_to_play_sec6_title,
                R.string.how_to_play_sec6_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e4", Color.WHITE, PieceType.ROOK)
                        .piece("e6", Color.BLACK, PieceType.QUEEN)
                        .pulse("e4")
                        .dots("e5", "e3", "e2", "c4", "d4", "f4", "g4")
                        .rings("e6")
                        .arrow("e4", "e6")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 7. Queen: rook + bishop combined.
        list.add(new RuleSection(
                7,
                R.string.how_to_play_sec7_title,
                R.string.how_to_play_sec7_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e4", Color.WHITE, PieceType.QUEEN)
                        .piece("g6", Color.BLACK, PieceType.ROOK)
                        .pulse("e4")
                        .dots("e5", "e6", "e3", "e2", "d4", "c4", "f4", "g4",
                                "d5", "c6", "f5", "d3", "c2", "f3", "g2")
                        .rings("g6")
                        .arrow("e4", "g6")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 8. King: one square in any direction.
        list.add(new RuleSection(
                8,
                R.string.how_to_play_sec8_title,
                R.string.how_to_play_sec8_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e4", Color.WHITE, PieceType.KING)
                        .piece("f5", Color.BLACK, PieceType.PAWN)
                        .pulse("e4")
                        .dots("d5", "e5", "d4", "f4", "d3", "e3", "f3")
                        .rings("f5")
                        .arrow("e4", "f5")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 9. Capturing: land on an enemy piece to take it.
        list.add(new RuleSection(
                9,
                R.string.how_to_play_sec9_title,
                R.string.how_to_play_sec9_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("d3", Color.WHITE, PieceType.KNIGHT)
                        .piece("f4", Color.BLACK, PieceType.QUEEN)
                        .piece("e5", Color.BLACK, PieceType.PAWN)
                        .pulse("d3", "f4")
                        .rings("f4")
                        .arrow("d3", "f4")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 10. Castling: king moves 2 toward a rook, rook jumps over (note: not blocked by attacks).
        list.add(new RuleSection(
                10,
                R.string.how_to_play_sec10_title,
                R.string.how_to_play_sec10_body,
                BoardDemoConfig.builder5x5(0, 3)
                        .piece("e1", Color.WHITE, PieceType.KING)
                        .piece("h1", Color.WHITE, PieceType.ROOK)
                        .piece("d2", Color.WHITE, PieceType.PAWN)
                        .piece("g2", Color.WHITE, PieceType.PAWN)
                        .piece("h2", Color.WHITE, PieceType.PAWN)
                        .piece("d3", Color.BLACK, PieceType.BISHOP)
                        .pulse("e1", "h1")
                        .dots("f1", "g1")
                        .arrow("e1", "g1")
                        .arrow("h1", "f1")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 11. En passant: shown as a 2-frame before/after.
        BoardDemoConfig enPassantBefore = BoardDemoConfig.builder5x5(3, 2)
                .piece("e5", Color.WHITE, PieceType.PAWN)
                .piece("d5", Color.BLACK, PieceType.PAWN)
                .pulse("e5", "d5")
                .dots("d7")
                .rings("d6")
                .arrow("d7", "d5")
                .arrow("e5", "d6")
                .build();

        BoardDemoConfig enPassantAfter = BoardDemoConfig.builder5x5(3, 2)
                .piece("d6", Color.WHITE, PieceType.PAWN)
                .pulse("d6")
                .dots("e5", "d5")
                .build();

        list.add(new RuleSection(
                11,
                R.string.how_to_play_sec11_title,
                R.string.how_to_play_sec11_body,
                enPassantBefore,
                enPassantAfter,
                ExtraVisualType.DUAL_BOARD_EN_PASSANT,
                false
        ));

        // 12. Pawn promotion: reach the last row, choose Queen/Rook/Bishop/Knight.
        list.add(new RuleSection(
                12,
                R.string.how_to_play_sec12_title,
                R.string.how_to_play_sec12_body,
                BoardDemoConfig.builder5x5(3, 2)
                        .piece("e7", Color.WHITE, PieceType.PAWN)
                        .piece("c8", Color.WHITE, PieceType.QUEEN)
                        .piece("d8", Color.WHITE, PieceType.ROOK)
                        .piece("f8", Color.WHITE, PieceType.BISHOP)
                        .piece("g8", Color.WHITE, PieceType.KNIGHT)
                        .pulse("e7", "e8")
                        .rings("e8")
                        .arrow("e7", "e8")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 13. No check, no checkmate: kings can be captured; the game keeps going.
        list.add(new RuleSection(
                13,
                R.string.how_to_play_sec13_title,
                R.string.how_to_play_sec13_body,
                BoardDemoConfig.builder5x5(3, 2)
                        .piece("e4", Color.WHITE, PieceType.ROOK)
                        .piece("e7", Color.BLACK, PieceType.KING)
                        .piece("c6", Color.BLACK, PieceType.QUEEN)
                        .piece("f5", Color.WHITE, PieceType.KNIGHT)
                        .pulse("e4", "e7")
                        .rings("e7")
                        .arrow("e4", "e7")
                        .build(),
                null,
                ExtraVisualType.NONE,
                true
        ));

        // 14. Points table: pieces with their values (Pawn 1, Knight 2, Bishop 3, Rook 4, Queen 5, King 7).
        list.add(new RuleSection(
                14,
                R.string.how_to_play_sec14_title,
                R.string.how_to_play_sec14_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("c3", Color.WHITE, PieceType.PAWN)
                        .piece("e3", Color.WHITE, PieceType.KNIGHT)
                        .piece("g3", Color.WHITE, PieceType.BISHOP)
                        .piece("c5", Color.WHITE, PieceType.ROOK)
                        .piece("e5", Color.WHITE, PieceType.QUEEN)
                        .piece("g5", Color.WHITE, PieceType.KING)
                        .pulse("e5", "g5")
                        .build(),
                null,
                ExtraVisualType.POINTS_TABLE,
                true
        ));

        // 15. Timer: each side has 1 or 2 minutes; clock runs only on your turn.
        list.add(new RuleSection(
                15,
                R.string.how_to_play_sec15_title,
                R.string.how_to_play_sec15_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e2", Color.WHITE, PieceType.PAWN)
                        .piece("f3", Color.WHITE, PieceType.KNIGHT)
                        .piece("e5", Color.BLACK, PieceType.PAWN)
                        .pulse("e2", "e4")
                        .dots("e3", "e4")
                        .arrow("e2", "e4")
                        .build(),
                null,
                ExtraVisualType.TIMER_BADGES,
                false
        ));

        // 16. How to win: when time ends, more points on the board wins; equal = draw.
        list.add(new RuleSection(
                16,
                R.string.how_to_play_sec16_title,
                R.string.how_to_play_sec16_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e2", Color.WHITE, PieceType.KING)
                        .piece("d4", Color.WHITE, PieceType.QUEEN)
                        .piece("f3", Color.WHITE, PieceType.ROOK)
                        .piece("e6", Color.BLACK, PieceType.KING)
                        .piece("f5", Color.BLACK, PieceType.BISHOP)
                        .pulse("e2", "d4", "f3")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 17. No legal moves: your turn is skipped, your clock keeps running.
        list.add(new RuleSection(
                17,
                R.string.how_to_play_sec17_title,
                R.string.how_to_play_sec17_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("d3", Color.WHITE, PieceType.PAWN)
                        .piece("d4", Color.BLACK, PieceType.PAWN)
                        .piece("f5", Color.BLACK, PieceType.KNIGHT)
                        .pulse("d3")
                        .dots("e3", "g3")
                        .arrow("f5", "e3")
                        .build(),
                null,
                ExtraVisualType.NONE,
                false
        ));

        // 18. Winning probability bar: how to read it.
        list.add(new RuleSection(
                18,
                R.string.how_to_play_sec18_title,
                R.string.how_to_play_sec18_body,
                BoardDemoConfig.builder5x5(1, 2)
                        .piece("e2", Color.WHITE, PieceType.KING)
                        .piece("d4", Color.WHITE, PieceType.QUEEN)
                        .piece("f3", Color.WHITE, PieceType.ROOK)
                        .piece("e6", Color.BLACK, PieceType.KING)
                        .pulse("e2", "d4", "f3")
                        .build(),
                null,
                ExtraVisualType.PROBABILITY_BAR,
                false
        ));

        return Collections.unmodifiableList(list);
    }
}
