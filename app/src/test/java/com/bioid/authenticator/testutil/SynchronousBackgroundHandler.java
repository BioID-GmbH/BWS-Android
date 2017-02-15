package com.bioid.authenticator.testutil;

import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.bioid.authenticator.base.functional.Consumer;
import com.bioid.authenticator.base.functional.Supplier;
import com.bioid.authenticator.base.threading.BackgroundHandler;

/**
 * Fake BackgroundHandler for testing purposes which does execute all code synchronously.
 * Does always return the task id {@link #TASK_ID}.
 */
@SuppressWarnings("unused")
public class SynchronousBackgroundHandler implements BackgroundHandler {

    public static final int TASK_ID = 42;

    private boolean doNothingOnRunWithDelay = false;
    private boolean doNothingOnRunOnBackgroundThread = false;

    public void doNothingOnRunWithDelay() {
        doNothingOnRunWithDelay = true;
    }

    public void doNothingOnRunOnBackgroundThread() {
        this.doNothingOnRunOnBackgroundThread = true;
    }


    @Override
    public int runWithDelay(@NonNull Runnable runnable, @IntRange(from = 0) long delayInMillis) {
        if (!doNothingOnRunWithDelay) {
            runnable.run();
        }

        return TASK_ID;
    }

    @Override
    public void cancelScheduledTask(int taskId) {
        // does nothing because synchronous operation has already complete
    }

    @Override
    public void cancelAllScheduledTasks() {
        // does nothing because synchronous operation has already complete
    }

    @Override
    public <T> int runOnBackgroundThread(@NonNull Supplier<T> supplier,
                                         @NonNull Consumer<T> onSuccess, @Nullable Consumer<RuntimeException> onError,
                                         @Nullable Runnable onComplete) {
        if (!doNothingOnRunOnBackgroundThread) {
            try {
                onSuccess.accept(supplier.get());
                if (onComplete != null) {
                    onComplete.run();
                }
            } catch (RuntimeException e) {
                try {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } catch (RuntimeException eInner) {
                    e = eInner;
                } finally {
                    if (onError != null) {
                        onError.accept(e);
                    }
                }
            }
        }

        return TASK_ID;
    }

    @Override
    public int runOnBackgroundThread(@NonNull Runnable runnable,
                                     @Nullable Runnable onSuccess, @Nullable Consumer<RuntimeException> onError,
                                     @Nullable Runnable onComplete) {
        if (!doNothingOnRunOnBackgroundThread) {
            try {
                runnable.run();
                if (onComplete != null) {
                    onComplete.run();
                }
                if (onSuccess != null) {
                    onSuccess.run();
                }
            } catch (RuntimeException e) {
                try {
                    if (onComplete != null) {
                        onComplete.run();
                    }
                } catch (RuntimeException eInner) {
                    e = eInner;
                } finally {
                    if (onError != null) {
                        onError.accept(e);
                    }
                }
            }
        }

        return TASK_ID;
    }

    @Override
    public void unsubscribeFromBackgroundTask(int taskId) {
        // does nothing because synchronous operation has already complete
    }

    @Override
    public void unsubscribeFromAllBackgroundTasks() {
        // does nothing because synchronous operation has already complete
    }
}
