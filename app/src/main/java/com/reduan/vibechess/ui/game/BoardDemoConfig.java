package com.reduan.vibechess.ui.game;

import androidx.annotation.NonNull;

import com.reduan.vibechess.core.Board;
import com.reduan.vibechess.core.Square;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable configuration for rendering {@link BoardView} in read-only "demo" mode
 * for the visual "How to Play" tutorial cards.
 * <p>
 * Supports either a full 8x8 board or a focused 5x5 sub-board window, along with
 * pulsing key-square highlights, move dots, capture rings, and directional arrows.
 */
public final class BoardDemoConfig {

    /**
     * Directional arrow between two board squares.
     */
    public record Arrow(@NonNull Square from, @NonNull Square to) {
        public Arrow {
            Objects.requireNonNull(from, "from must not be null");
            Objects.requireNonNull(to, "to must not be null");
        }
    }

    private final Board board;
    private final int startRank;
    private final int startFile;
    private final int gridSize;
    private final List<Square> pulsingSquares;
    private final List<Square> moveDotSquares;
    private final List<Square> captureRingSquares;
    private final List<Arrow> arrows;

    private BoardDemoConfig(Builder builder) {
        this.board = builder.board;
        this.startRank = builder.startRank;
        this.startFile = builder.startFile;
        this.gridSize = builder.gridSize;
        this.pulsingSquares = Collections.unmodifiableList(new ArrayList<>(builder.pulsingSquares));
        this.moveDotSquares = Collections.unmodifiableList(new ArrayList<>(builder.moveDotSquares));
        this.captureRingSquares = Collections.unmodifiableList(new ArrayList<>(builder.captureRingSquares));
        this.arrows = Collections.unmodifiableList(new ArrayList<>(builder.arrows));
    }

    /**
     * Creates a builder for a focused 5x5 mini-board window starting at {@code (startRank, startFile)}.
     */
    @NonNull
    public static Builder builder5x5(int startRank, int startFile) {
        return new Builder(startRank, startFile, 5);
    }

    /**
     * Creates a builder for a full 8x8 mini-board.
     */
    @NonNull
    public static Builder builder8x8() {
        return new Builder(0, 0, 8);
    }

    @NonNull
    public Board getBoard() {
        return board;
    }

    public int getStartRank() {
        return startRank;
    }

    public int getStartFile() {
        return startFile;
    }

    public int getGridSize() {
        return gridSize;
    }

    @NonNull
    public List<Square> getPulsingSquares() {
        return pulsingSquares;
    }

    @NonNull
    public List<Square> getMoveDotSquares() {
        return moveDotSquares;
    }

    @NonNull
    public List<Square> getCaptureRingSquares() {
        return captureRingSquares;
    }

    @NonNull
    public List<Arrow> getArrows() {
        return arrows;
    }

    public static final class Builder {
        private Board board = Board.empty();
        private final int startRank;
        private final int startFile;
        private final int gridSize;
        private final List<Square> pulsingSquares = new ArrayList<>();
        private final List<Square> moveDotSquares = new ArrayList<>();
        private final List<Square> captureRingSquares = new ArrayList<>();
        private final List<Arrow> arrows = new ArrayList<>();

        private Builder(int startRank, int startFile, int gridSize) {
            if (gridSize < 1 || gridSize > Square.BOARD_SIZE) {
                throw new IllegalArgumentException("Invalid gridSize: " + gridSize);
            }
            if (startRank < 0 || startRank + gridSize > Square.BOARD_SIZE
                    || startFile < 0 || startFile + gridSize > Square.BOARD_SIZE) {
                throw new IllegalArgumentException(
                        "Invalid window: startRank=" + startRank + ", startFile=" + startFile + ", size=" + gridSize);
            }
            this.startRank = startRank;
            this.startFile = startFile;
            this.gridSize = gridSize;
        }

        @NonNull
        public Builder board(@NonNull Board board) {
            this.board = Objects.requireNonNull(board);
            return this;
        }

        @NonNull
        public Builder piece(@NonNull String algebraic,
                             @NonNull com.reduan.vibechess.core.Color color,
                             @NonNull com.reduan.vibechess.core.PieceType type) {
            this.board = this.board.withPieceAt(
                    Square.fromAlgebraic(algebraic),
                    com.reduan.vibechess.core.Piece.of(color, type)
            );
            return this;
        }

        @NonNull
        public Builder pulse(@NonNull String... algebraicSquares) {
            for (String sq : algebraicSquares) {
                pulsingSquares.add(Square.fromAlgebraic(sq));
            }
            return this;
        }

        @NonNull
        public Builder dots(@NonNull String... algebraicSquares) {
            for (String sq : algebraicSquares) {
                moveDotSquares.add(Square.fromAlgebraic(sq));
            }
            return this;
        }

        @NonNull
        public Builder rings(@NonNull String... algebraicSquares) {
            for (String sq : algebraicSquares) {
                captureRingSquares.add(Square.fromAlgebraic(sq));
            }
            return this;
        }

        @NonNull
        public Builder arrow(@NonNull String fromAlgebraic, @NonNull String toAlgebraic) {
            arrows.add(new Arrow(
                    Square.fromAlgebraic(fromAlgebraic),
                    Square.fromAlgebraic(toAlgebraic)
            ));
            return this;
        }

        @NonNull
        public BoardDemoConfig build() {
            return new BoardDemoConfig(this);
        }
    }
}
