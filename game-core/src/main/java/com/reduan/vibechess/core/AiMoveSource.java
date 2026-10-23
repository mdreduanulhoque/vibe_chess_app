package com.reduan.vibechess.core;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link MoveSource} implementation that computes moves using {@link MinimaxAi} on a background
 * {@link ExecutorService} and posts the result back via a configurable {@link Executor}
 * (such as the Android main thread handler in {@code :app}).
 * <p>
 * Supports clean cancellation on pause, exit, or game end, and notifies a {@link ThinkingListener}
 * when background thinking starts or stops.
 */
public final class AiMoveSource implements MoveSource {

    /**
     * Listener notified when the AI starts or finishes thinking on a move.
     */
    @FunctionalInterface
    public interface ThinkingListener {
        void onThinkingChanged(boolean thinking);
    }

    private final AiDifficulty difficulty;
    private final MinimaxAi aiEngine;
    private final ExecutorService backgroundExecutor;
    private final boolean ownsBackgroundExecutor;
    private final Executor callbackExecutor;
    private final ThinkingListener thinkingListener;

    private final AtomicLong requestCounter = new AtomicLong(0L);
    private Future<?> activeFuture;
    private volatile boolean thinking = false;

    /**
     * Creates an {@link AiMoveSource} with its own daemon background thread and direct callback delivery.
     */
    public AiMoveSource(AiDifficulty difficulty) {
        this(difficulty, Runnable::run, isThinking -> {
        });
    }

    /**
     * Creates an {@link AiMoveSource} with its own daemon background thread, delivering move results
     * and thinking status updates on {@code callbackExecutor}.
     */
    public AiMoveSource(AiDifficulty difficulty,
                        Executor callbackExecutor,
                        ThinkingListener thinkingListener) {
        this(
                difficulty,
                new MinimaxAi(),
                createDefaultBackgroundExecutor(),
                true,
                callbackExecutor,
                thinkingListener
        );
    }

    /**
     * Full constructor allowing custom {@link MinimaxAi} and {@link ExecutorService} injection.
     */
    public AiMoveSource(AiDifficulty difficulty,
                        MinimaxAi aiEngine,
                        ExecutorService backgroundExecutor,
                        Executor callbackExecutor,
                        ThinkingListener thinkingListener) {
        this(
                difficulty,
                aiEngine,
                backgroundExecutor,
                false,
                callbackExecutor,
                thinkingListener
        );
    }

    private AiMoveSource(AiDifficulty difficulty,
                         MinimaxAi aiEngine,
                         ExecutorService backgroundExecutor,
                         boolean ownsBackgroundExecutor,
                         Executor callbackExecutor,
                         ThinkingListener thinkingListener) {
        this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
        this.aiEngine = Objects.requireNonNull(aiEngine, "aiEngine must not be null");
        this.backgroundExecutor = Objects.requireNonNull(
                backgroundExecutor,
                "backgroundExecutor must not be null"
        );
        this.ownsBackgroundExecutor = ownsBackgroundExecutor;
        this.callbackExecutor = Objects.requireNonNull(
                callbackExecutor,
                "callbackExecutor must not be null"
        );
        this.thinkingListener = Objects.requireNonNull(
                thinkingListener,
                "thinkingListener must not be null"
        );
    }

    private static ExecutorService createDefaultBackgroundExecutor() {
        return Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "vibe-chess-ai-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public AiDifficulty getDifficulty() {
        return difficulty;
    }

    public boolean isThinking() {
        return thinking;
    }

    @Override
    public boolean isHumanInteractive() {
        return false;
    }

    @Override
    public synchronized void requestMove(GameState state, MoveCallback callback) {
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        cancelInternal(false);

        if (state.isGameOver() || !state.hasLegalMoves()) {
            thinking = false;
            callbackExecutor.execute(() -> thinkingListener.onThinkingChanged(false));
            return;
        }

        long requestId = requestCounter.incrementAndGet();
        thinking = true;
        callbackExecutor.execute(() -> {
            if (requestCounter.get() == requestId) {
                thinkingListener.onThinkingChanged(true);
            }
        });

        activeFuture = backgroundExecutor.submit(() -> {
            Move chosenMove = null;
            try {
                if (requestCounter.get() != requestId || Thread.currentThread().isInterrupted()) {
                    return;
                }
                chosenMove = aiEngine.chooseMove(state, difficulty);
            } finally {
                final Move resultMove = chosenMove;
                callbackExecutor.execute(() -> {
                    synchronized (AiMoveSource.this) {
                        if (requestCounter.get() != requestId) {
                            return;
                        }
                        thinking = false;
                        activeFuture = null;
                    }
                    thinkingListener.onThinkingChanged(false);
                    if (resultMove != null) {
                        callback.onMoveSelected(resultMove);
                    }
                });
            }
        });
    }

    @Override
    public synchronized void cancel() {
        cancelInternal(true);
    }

    private void cancelInternal(boolean notifyIdle) {
        requestCounter.incrementAndGet();
        Future<?> future = activeFuture;
        activeFuture = null;
        if (future != null) {
            future.cancel(true);
        }
        boolean wasThinking = thinking;
        thinking = false;
        if (notifyIdle && wasThinking) {
            callbackExecutor.execute(() -> thinkingListener.onThinkingChanged(false));
        }
    }

    /**
     * Cancels any running search and shuts down the owned background {@link ExecutorService}.
     */
    public synchronized void shutdown() {
        cancelInternal(true);
        if (ownsBackgroundExecutor) {
            backgroundExecutor.shutdownNow();
        }
    }
}
